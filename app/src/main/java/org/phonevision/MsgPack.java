package org.phonevision;

import java.nio.ByteBuffer;

/** Just enough MessagePack for NT4 value messages: [id, timestamp, typeIdx, value]. */
final class MsgPack {
    private MsgPack() {}

    static byte[] encodeValue(long id, long timestampMicros, int type, long intValue) {
        ByteBuffer b = ByteBuffer.allocate(64);
        b.put((byte) 0x94); putInt(b, id); putU64(b, timestampMicros); putInt(b, type); putU64(b, intValue);
        return trim(b);
    }

    static byte[] encodeValue(long id, long timestampMicros, int type, double[] arr) {
        ByteBuffer b = ByteBuffer.allocate(32 + 9 * arr.length);
        b.put((byte) 0x94); putInt(b, id); putU64(b, timestampMicros); putInt(b, type);
        if (arr.length < 16) b.put((byte) (0x90 | arr.length)); else { b.put((byte) 0xdc); b.putShort((short) arr.length); }
        for (double d : arr) { b.put((byte) 0xcb); b.putDouble(d); }
        return trim(b);
    }

    static byte[] encodeDouble(long id, long timestampMicros, int type, double v) {
        ByteBuffer b = ByteBuffer.allocate(40);
        b.put((byte) 0x94); putInt(b, id); putU64(b, timestampMicros); putInt(b, type); b.put((byte) 0xcb); b.putDouble(v);
        return trim(b);
    }

    private static void putInt(ByteBuffer b, long v) {
        if (v >= 0 && v < 128) b.put((byte) v);
        else if (v < 0 && v >= -32) b.put((byte) v);
        else if (v >= 0 && v < 65536) { b.put((byte) 0xcd); b.putShort((short) v); }
        else putU64(b, v);
    }
    private static void putU64(ByteBuffer b, long v) { b.put((byte) 0xcf); b.putLong(v); }
    private static byte[] trim(ByteBuffer b) { byte[] o = new byte[b.position()]; b.flip(); b.get(o); return o; }

    static final class Reader {
        private final ByteBuffer b;
        Reader(byte[] d) { b = ByteBuffer.wrap(d); }
        boolean hasMore() { return b.hasRemaining(); }
        int readArrayHeader() {
            int t = b.get() & 0xFF;
            if ((t & 0xF0) == 0x90) return t & 0x0F;
            if (t == 0xdc) return b.getShort() & 0xFFFF;
            throw new IllegalStateException("not an array");
        }
        long readLong() {
            int t = b.get() & 0xFF;
            if (t < 0x80) return t;
            if (t >= 0xE0) return (byte) t;
            switch (t) {
                case 0xcc: return b.get() & 0xFF;
                case 0xcd: return b.getShort() & 0xFFFF;
                case 0xce: return b.getInt() & 0xFFFFFFFFL;
                case 0xcf: case 0xd3: return b.getLong();
                case 0xd0: return b.get();
                case 0xd1: return b.getShort();
                case 0xd2: return b.getInt();
                default: throw new IllegalStateException("not an int: " + t);
            }
        }
    }
}
