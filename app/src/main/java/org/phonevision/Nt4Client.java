package org.phonevision;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Minimal, dependency-free NetworkTables 4 client that publishes ONE double[] topic.
 * (RFC 6455 WebSocket client + MessagePack, subprotocol v4.1/v4.0.)
 * Values are timestamped in the roboRIO's clock (= FPGA time), corrected for capture latency,
 * using the NT4 round-trip time-sync, so the robot can pass them straight to a pose estimator.
 */
public class Nt4Client {
    public static final int PORT = 5810;
    public static final int MAX_TAGS = 24;
    private static final int TYPE_DOUBLE = 1, TYPE_DOUBLE_ARRAY = 17;
    private static final int PUBUID = 1;                  // "data" array; scalars use PUBUID+1+i
    /** Limelight-style scalar topics, published every frame under /PhoneVision/<cam>/ */
    public static final String[] SCALARS = {"tv", "tx", "ty", "ta", "tid", "tl", "cl", "td", "tc", "hb"};

    private final String clientName, base;
    private volatile String host = "";
    private volatile Socket sock;
    private OutputStream out;
    private final Object writeLock = new Object();
    private final SecureRandom rnd = new SecureRandom();
    private volatile boolean running = true, connected = false, synced = false;
    private volatile long offsetMicros = 0, bestRtt = Long.MAX_VALUE, lastRxNanos = 0;
    public volatile String status = "not connected";

    /** @param base topic prefix, e.g. "/PhoneVision/phone/" */
    public Nt4Client(String clientName, String base) { this.clientName = clientName; this.base = base; }

    public void start() {
        Thread t = new Thread(this::loop, "nt4-client"); t.setDaemon(true); t.start();
        Thread s = new Thread(this::syncLoop, "nt4-sync"); s.setDaemon(true); s.start();
        Thread w = new Thread(this::sendLoop, "nt4-send"); w.setDaemon(true); w.start();
    }

    public void stop() { running = false; closeSocket(); }

    public void setHost(String h) {
        if (!h.equals(host)) { host = h; closeSocket(); }
    }

    public boolean isConnected() { return connected && synced; }

    private void closeSocket() { try { Socket s = sock; if (s != null) s.close(); } catch (IOException ignored) {} }

    /** Client monotonic clock in microseconds. */
    private static long nowMicros() { return System.nanoTime() / 1000; }

    /** Current time in the server's (roboRIO FPGA) clock. */
    public long serverNowMicros() { return nowMicros() + offsetMicros; }

    // ------------------------------------------------------------------ public API
    /**
     * Publish one frame: the "data" array plus the Limelight-style scalars (order = SCALARS),
     * all stamped with the capture time (now - latencyMs) in roboRIO clock. One WebSocket frame. Drops if not connected.
     *
     * This only encodes the message and hands it to the send thread; it never touches the socket itself.
     * The actual TCP write used to happen right here, on the caller's thread (the camera frame-processing
     * thread) - if the network was slow, congested, or the roboRIO briefly stopped reading, that blocking
     * write() could stall for a long time and freeze the whole vision pipeline (dropped frames, and the
     * on-screen tag box visibly lagging behind the live video). Now a stalled network can only ever delay
     * outgoing NT4 data, never the camera/detector/overlay.
     */
    public void publishFrame(double[] data, double[] scalars, double latencyMs) {
        if (!isConnected()) return;
        long ts = serverNowMicros() - (long) (latencyMs * 1000);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] m = MsgPack.encodeValue(PUBUID, ts, TYPE_DOUBLE_ARRAY, data); bo.write(m, 0, m.length);
        for (int i = 0; i < SCALARS.length; i++) { m = MsgPack.encodeDouble(PUBUID + 1 + i, ts, TYPE_DOUBLE, scalars[i]); bo.write(m, 0, m.length); }
        pendingPayload.set(bo.toByteArray());
        synchronized (sendSignal) { sendSignal.notify(); }
    }

    // Latest-wins mailbox for outgoing frames: if the send thread is behind, we want the newest frame,
    // not a backlog of stale ones queued up.
    private final java.util.concurrent.atomic.AtomicReference<byte[]> pendingPayload = new java.util.concurrent.atomic.AtomicReference<>();
    private final Object sendSignal = new Object();

    private void sendLoop() {
        while (running) {
            byte[] payload = pendingPayload.getAndSet(null);
            if (payload == null) {
                synchronized (sendSignal) { try { sendSignal.wait(200); } catch (InterruptedException ignored) {} }
                continue;
            }
            try { writeFrame(0x2, payload); } catch (IOException e) { closeSocket(); }
        }
    }

    // ------------------------------------------------------------------ connection loop
    private void loop() {
        while (running) {
            String h = host;
            if (h.isEmpty()) { sleep(300); continue; }
            try { session(h); }
            catch (Exception e) { status = "retrying " + h + ": " + e.getClass().getSimpleName() + " " + e.getMessage(); }
            connected = false; synced = false; bestRtt = Long.MAX_VALUE; closeSocket();
            sleep(1000);
        }
    }

    private void session(String h) throws Exception {
        status = "connecting to " + h + ":" + PORT;
        Socket s = new Socket();
        s.connect(new InetSocketAddress(h, PORT), 2000);
        s.setTcpNoDelay(true);
        sock = s;
        InputStream in = new BufferedInputStream(s.getInputStream(), 8192);
        synchronized (writeLock) { out = s.getOutputStream(); }
        byte[] k = new byte[16]; rnd.nextBytes(k);
        String req = "GET /nt/" + clientName + " HTTP/1.1\r\nHost: " + h + ":" + PORT + "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                + "Sec-WebSocket-Key: " + Base64.getEncoder().encodeToString(k) + "\r\nSec-WebSocket-Version: 13\r\n"
                + "Sec-WebSocket-Protocol: v4.1.networktables.first.wpi.edu, networktables.first.wpi.edu\r\n\r\n";
        synchronized (writeLock) { out.write(req.getBytes(StandardCharsets.US_ASCII)); out.flush(); }
        // read HTTP response headers
        StringBuilder hdr = new StringBuilder();
        while (!hdr.toString().endsWith("\r\n\r\n")) {
            int c = in.read(); if (c < 0) throw new IOException("closed during handshake");
            hdr.append((char) c); if (hdr.length() > 8192) throw new IOException("bad handshake");
        }
        if (!hdr.toString().startsWith("HTTP/1.1 101")) throw new IOException("upgrade refused: " + hdr.toString().split("\r\n")[0]);
        lastRxNanos = System.nanoTime();
        StringBuilder pb = new StringBuilder("[");
        pb.append("{\"method\":\"publish\",\"params\":{\"name\":\"").append(base).append("data\",\"type\":\"double[]\",\"pubuid\":").append(PUBUID).append(",\"properties\":{}}}");
        for (int i = 0; i < SCALARS.length; i++)
            pb.append(",{\"method\":\"publish\",\"params\":{\"name\":\"").append(base).append(SCALARS[i]).append("\",\"type\":\"double\",\"pubuid\":").append(PUBUID + 1 + i).append(",\"properties\":{}}}");
        String pub = pb.append("]").toString();
        writeFrame(0x1, pub.getBytes(StandardCharsets.UTF_8));
        connected = true;
        status = "connected to " + h + " (syncing clock)";
        sendSync();

        ByteArrayOutputStream msg = new ByteArrayOutputStream(); int msgOp = 0;
        while (running) {
            int b0 = in.read(), b1 = in.read();
            if (b0 < 0 || b1 < 0) throw new IOException("connection closed");
            boolean fin = (b0 & 0x80) != 0; int op = b0 & 0x0F;
            long len = b1 & 0x7F;
            if (len == 126) len = ((long) in.read() << 8) | in.read();
            else if (len == 127) { len = 0; for (int i = 0; i < 8; i++) len = (len << 8) | in.read(); }
            byte[] mask = null;
            if ((b1 & 0x80) != 0) { mask = new byte[4]; readFully(in, mask); }
            if (len > (1 << 24)) throw new IOException("frame too large");
            byte[] pl = new byte[(int) len]; readFully(in, pl);
            if (mask != null) for (int i = 0; i < pl.length; i++) pl[i] ^= mask[i & 3];
            lastRxNanos = System.nanoTime();
            if (op == 0x8) throw new IOException("server closed connection");
            if (op == 0x9) { writeFrame(0xA, pl); continue; }
            if (op == 0xA) continue;
            if (op != 0) { msg.reset(); msgOp = op; }
            msg.write(pl);
            if (!fin) continue;
            if (msgOp == 0x2) handleBinary(msg.toByteArray());
            msg.reset();
        }
    }

    private static void readFully(InputStream in, byte[] b) throws IOException {
        int o = 0;
        while (o < b.length) { int n = in.read(b, o, b.length - o); if (n < 0) throw new IOException("eof"); o += n; }
    }

    // ------------------------------------------------------------------ time sync
    private void syncLoop() {
        while (running) {
            sleep(1000);
            if (!connected) continue;
            if ((System.nanoTime() - lastRxNanos) > 5_000_000_000L) { status = "timeout, reconnecting"; closeSocket(); continue; }
            try { sendSync(); } catch (IOException e) { closeSocket(); }
        }
    }

    private void sendSync() throws IOException { writeFrame(0x2, MsgPack.encodeValue(-1, 0, 2, nowMicros())); }

    private void handleBinary(byte[] data) {
        MsgPack.Reader r = new MsgPack.Reader(data);
        try {
            while (r.hasMore()) {
                if (r.readArrayHeader() != 4) return;
                long id = r.readLong(), serverTime = r.readLong(); r.readLong();
                if (id != -1) return;              // we never subscribe, so only time-sync replies are expected
                long echo = r.readLong(), now = nowMicros(), rtt = now - echo;
                if (rtt < 0) continue;
                if (rtt <= bestRtt * 1.5 + 2000 || !synced) {   // prefer low-latency samples
                    offsetMicros = serverTime + rtt / 2 - now; bestRtt = Math.min(bestRtt, rtt);
                    if (!synced) status = String.format("connected to %s, clock synced (rtt %.1f ms)", host, rtt / 1000.0);
                    synced = true;
                }
                if (rtt > 0 && bestRtt < Long.MAX_VALUE && (nowMicros() & 0xFFFFF) == 0) bestRtt = rtt; // slow re-learn
            }
        } catch (RuntimeException e) { /* malformed message: ignore */ }
    }

    // ------------------------------------------------------------------ websocket framing
    private void writeFrame(int opcode, byte[] payload) throws IOException {
        byte[] mask = new byte[4]; rnd.nextBytes(mask);
        ByteBuffer b = ByteBuffer.allocate(14 + payload.length);
        b.put((byte) (0x80 | opcode));
        if (payload.length < 126) b.put((byte) (0x80 | payload.length));
        else if (payload.length < 65536) { b.put((byte) (0x80 | 126)); b.putShort((short) payload.length); }
        else { b.put((byte) (0x80 | 127)); b.putLong(payload.length); }
        b.put(mask);
        for (int i = 0; i < payload.length; i++) b.put((byte) (payload[i] ^ mask[i & 3]));
        synchronized (writeLock) {
            if (out == null) throw new IOException("not connected");
            out.write(b.array(), 0, b.position()); out.flush();
        }
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
}
