package org.phonevision;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import java.io.File;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private Settings st; private CameraController cam; private Nt4Client nt; private FieldLayout layout; private String fieldMsg = "";
    private AspectFrame frame; private TextureView tv; private Overlay overlay;
    private TextView status, calStatus;
    private SurfaceTexture surface;
    private long tagHandle;
    private PoseSolver solver; private Settings.Calib activeCalib;
    private Calibrator calibrator;
    private volatile boolean calibMode = false;
    private final AtomicBoolean captureReq = new AtomicBoolean(false);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile String calMsg = "";
    private ScrollView settingsOverlay; private LinearLayout checkBox; private TextView checkSummary, stressText;
    private boolean nativeOk = true; private String nativeMsg = ""; private volatile double procAvg = 0; private double tempStart = Double.NaN;
    private CameraController.Info camInfo; private int checkTick = 0;
    private long stressStart = 0; private double stressT0; private int stressMin, stressSum, stressN, stressThermalMax;
    private TextView hud, miniStatus; private View gearButton; private boolean matchMode = false, resumed = false;
    // stats
    private volatile long lastFrameMs = SystemClock.elapsedRealtime(); private volatile double primTx, primTy, primDist; private volatile int primId = -1; private volatile String pipelineError = ""; private volatile int fps, tagsSeen; private volatile boolean multiUsed; private int seq = 0; private volatile float lastLatency, lastProc; private int frames; private long fpsT = SystemClock.elapsedRealtime();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        st = new Settings(this);
        OpenCVLoader.initLocal();
        cam = new CameraController(this, st);
        try { tagHandle = AprilTagNative.create(st.threads, st.decimate, st.sigma); nativeMsg = "apriltag loaded (" + Build.SUPPORTED_ABIS[0] + ")"; }
        catch (Throwable t) { nativeOk = false; nativeMsg = t.getClass().getSimpleName() + ": " + t.getMessage(); }
        tempStart = batteryTempC();
        calibrator = new Calibrator(st.boardCols, st.boardRows, st.squareM);
        restartNt(); loadField();
        buildUi();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(Build.VERSION.SDK_INT >= 33 ? new String[]{Manifest.permission.CAMERA, "android.permission.POST_NOTIFICATIONS"}
                    : new String[]{Manifest.permission.CAMERA}, 1);
        else startKeepAlive();
        cam.onLost = () -> ui.postDelayed(() -> { if (resumed) restartCamera(); }, 1000);
        ui.post(statusTick); ui.post(watchdog);
        // Settings is a one-time thing: it opens automatically the very first time the app is ever run
        // (so the user can enter a team number, calibrate, etc.), and after that only via the gear button -
        // it no longer opens itself on every launch, and it no longer stays docked on screen shrinking the
        // camera view. If they were mid-match-mode last time, honor that instead.
        if (!st.onboardingDone) showSettings(true);
        else if (st.startInMatchMode) setMatchMode(true);
        else showSettings(false);
    }

    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] g) { startKeepAlive(); restartCamera(); }

    private void startKeepAlive() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        try { startForegroundService(new Intent(this, KeepAliveService.class)); } catch (RuntimeException ignored) {}
    }

    /** If frames stop arriving (camera stalled, HAL hiccup, ROM suspended it) restart the camera automatically. */
    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            long n = SystemClock.elapsedRealtime();
            if (resumed && surface != null && n - lastFrameMs > 3000) { lastFrameMs = n; restartCamera(); }
            ui.postDelayed(this, 1000);
        }
    };

    private void setMatchMode(boolean on) {
        matchMode = on;
        if (on) settingsOverlay.setVisibility(View.GONE);
        gearButton.setVisibility(on ? View.GONE : View.VISIBLE);
        miniStatus.setVisibility(on ? View.GONE : (settingsOverlay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        hud.setVisibility(on ? View.VISIBLE : View.GONE);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = on ? st.dimBrightness : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(lp);
        getWindow().getDecorView().setSystemUiVisibility(on ? (View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE) : View.SYSTEM_UI_FLAG_VISIBLE);
        frame.setAlpha(on ? 0.35f : 1f);
    }

    private String powerInfo() {
        StringBuilder sb = new StringBuilder();
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b != null) {
            int lvl = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), tmp = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0), plug = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            sb.append(String.format(Locale.US, "battery %d%% %.1fC %s", lvl, tmp / 10.0, plug != 0 ? "charging/powered" : "ON BATTERY"));
        }
        if (Build.VERSION.SDK_INT >= 29) {
            int t = ((PowerManager) getSystemService(POWER_SERVICE)).getCurrentThermalStatus();
            if (t >= PowerManager.THERMAL_STATUS_MODERATE) sb.append("  THERMAL ").append(t >= PowerManager.THERMAL_STATUS_SEVERE ? "SEVERE (throttling!)" : "moderate");
        }
        return sb.toString();
    }
    @Override protected void onResume() { super.onResume(); resumed = true; lastFrameMs = SystemClock.elapsedRealtime(); restartCamera(); }
    @Override protected void onPause() { super.onPause(); resumed = false; cam.stop(); }
    @Override protected void onDestroy() { super.onDestroy(); nt.stop(); if (nativeOk) AprilTagNative.destroy(tagHandle); }

    private void restartNt() {
        if (nt != null) nt.stop();
        nt = new Nt4Client("PhoneVision-" + st.camName, "/PhoneVision/" + st.camName + "/");
        nt.setHost(st.host()); nt.start();
    }

    private void loadField() {
        try {
            String json;
            if (st.field.equals("custom")) {
                File f = new File(getExternalFilesDir(null), "field.json");
                json = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            } else {
                try (InputStream in = getAssets().open("fields/" + st.field + ".json")) {
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(); byte[] buf = new byte[4096]; int n;
                    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                    json = new String(bo.toByteArray(), StandardCharsets.UTF_8);
                }
            }
            layout = FieldLayout.parse(json); fieldMsg = layout.size() + " tags";
        } catch (Exception e) { layout = null; fieldMsg = "field load failed: " + e.getMessage(); }
    }

    private String netInfo() {
        StringBuilder sb = new StringBuilder("Network interfaces:");
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address) sb.append("\n  ").append(ni.getName()).append("  ").append(a.getHostAddress());
            }
        } catch (Exception e) { sb.append(" error"); }
        return sb.toString();
    }

    private void restartCamera() {
        if (surface == null || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        cam.stop();
        frame.aspect = st.width / (double) st.height; frame.requestLayout();
        overlay.imgW = st.width; overlay.imgH = st.height;
        refreshSolver();
        cam.start(surface, this::onFrame);
        tv.post(this::applyPreviewTransform);
    }

    /** Corrects the TextureView for the camera's fixed sensor mounting angle. Without this the raw
     * buffer is shown un-rotated: the preview looks stretched/warped and, since the AprilTag overlay
     * is drawn in that same raw pixel space, the tag box drifts away from the tag on screen. */
    private void applyPreviewTransform() {
        int vw = tv.getWidth(), vh = tv.getHeight();
        if (vw == 0 || vh == 0) return;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix m = new Matrix();
        RectF viewRect = new RectF(0, 0, vw, vh);
        float cx = viewRect.centerX(), cy = viewRect.centerY();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            RectF bufRect = new RectF(0, 0, vh, vw);
            bufRect.offset(cx - bufRect.centerX(), cy - bufRect.centerY());
            m.setRectToRect(viewRect, bufRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) vh / vw, (float) vw / vh);
            m.postScale(scale, scale, cx, cy);
            m.postRotate(90 * (rotation - 2), cx, cy);
        } else if (rotation == Surface.ROTATION_180) {
            m.postRotate(180, cx, cy);
        }
        tv.setTransform(m);
    }

    private void refreshSolver() {
        activeCalib = st.loadCalib();
        if (activeCalib == null) activeCalib = cam.estimateCalib(st.width, st.height);
        solver = new PoseSolver(activeCalib);
    }

    // ------------------------------------------------------------------ frame pipeline
    private void onFrame(Image img, long tsNs) {
        try {
        long t0 = SystemClock.elapsedRealtimeNanos();
            lastFrameMs = SystemClock.elapsedRealtime(); // watchdog heartbeat (all modes)
            Image.Plane yp = img.getPlanes()[0];
            ByteBuffer buf = yp.getBuffer(); int stride = yp.getRowStride(), w = img.getWidth(), h = img.getHeight();
            if (calibMode) {
                Mat gray = new Mat(h, w, CvType.CV_8UC1, buf, stride);
                int before = calibrator.count();
                calibrator.process(gray, captureReq.getAndSet(false));
                gray.release();
                if (calibrator.count() > before) calMsg = "Captured view " + calibrator.count();
                overlay.points = calibrator.lastCorners; overlay.quads = new ArrayList<>(); overlay.labels = new ArrayList<>();
                overlay.postInvalidate(); tick();
                return;
            }
            double[] d = nativeOk ? AprilTagNative.detect(tagHandle, buf, w, h, stride) : new double[0];
            List<PoseSolver.Det> raw = new ArrayList<>();
            for (int i = 0; i + 10 < d.length; i += 11) {
                if (d[i + 1] > st.maxHamming || d[i + 2] < st.minMargin) continue;
                PoseSolver.Det det = new PoseSolver.Det(); det.id = (int) d[i]; det.margin = d[i + 2];
                System.arraycopy(d, i + 3, det.corners, 0, 8); raw.add(det);
            }
            // ID allow/block list (Limelight-style; supports "1, 3-6, 12"). Parsed once when the text
            // changes (Settings.refreshIdFilters), not every frame - re-parsing with a regex split and a
            // fresh HashSet 30x/second was pure wasted work on the hot path.
            java.util.Set<Integer> allow = st.idAllowSet, block = st.idBlockSet;
            PoseSolver ps = solver; double size = st.tagSizeM;
            List<PoseSolver.Det> filtered = new ArrayList<>(); List<PoseSolver.Result> filteredRes = new ArrayList<>();
            List<TagSorter.Cand> cands = new ArrayList<>();
            for (PoseSolver.Det det : raw) {
                if (!TagFilter.accepts(allow, block, det.id)) continue;
                PoseSolver.Result r = ps.solveSingle(det.corners, size);
                double[] ang = ps.centerAngles(det.corners);
                double areaPct = 100.0 * quadArea(det.corners) / (w * (double) h);
                double dist = r == null ? -1 : Math.sqrt(r.tx * r.tx + r.ty * r.ty + r.tz * r.tz);
                int ref = filtered.size();
                filtered.add(det); filteredRes.add(r);
                cands.add(new TagSorter.Cand(det.id, ang[0], ang[1], areaPct, dist, ref));
            }
            // Sort exactly like Limelight: index 0 after sorting is the primary target (drives tv/tx/ty/ta/tid)
            TagSorter.sort(cands, st.sortMode, st.priorityId);
            List<PoseSolver.Det> good = new ArrayList<>(); List<PoseSolver.Result> res = new ArrayList<>();
            List<double[]> quads = new ArrayList<>(); List<String> labels = new ArrayList<>();
            for (TagSorter.Cand c : cands) {
                int ref = (Integer) c.ref; PoseSolver.Det det = filtered.get(ref); PoseSolver.Result r = filteredRes.get(ref);
                good.add(det); res.add(r); quads.add(det.corners);
                labels.add(r == null ? "id " + det.id : String.format(Locale.US, "id %d  %.2fm", det.id, Math.sqrt(r.tx * r.tx + r.ty * r.ty + r.tz * r.tz)));
            }
            if (good.size() > Nt4Client.MAX_TAGS) { good = good.subList(0, Nt4Client.MAX_TAGS); res = res.subList(0, Nt4Client.MAX_TAGS); }

            PoseSolver.Result field = null; FieldLayout lay = layout;
            if (st.multiTag && lay != null && good.size() >= 2) {
                field = ps.solveField(good, lay, size);
                if (field != null && field.reprojErr > 4.0) field = null; // inconsistent -> don't trust
            }
            overlay.points = null; overlay.quads = quads; overlay.labels = labels; overlay.postInvalidate();
            lastFrameMs = SystemClock.elapsedRealtime();

            // ---- primary target (Limelight semantics): whatever the chosen sort put first
            double tv = 0, tx = 0, ty = 0, ta = 0, tid = -1, td = -1;
            if (!good.isEmpty()) {
                PoseSolver.Det pd = good.get(0);
                double[] ang = ps.centerAngles(pd.corners);
                tv = 1; tx = ang[0]; ty = ang[1]; ta = 100.0 * quadArea(pd.corners) / (w * (double) h); tid = pd.id;
                PoseSolver.Result pr = res.get(0);
                if (pr != null) td = Math.sqrt(pr.tx * pr.tx + pr.ty * pr.ty + pr.tz * pr.tz);
            }
            primTx = tx; primTy = ty; primDist = td; primId = (int) tid;

            // ---- latency: cl = mid-exposure -> we got the frame, tl = our processing time
            long now = SystemClock.elapsedRealtimeNanos();
            double cl = cam.timestampRealtime ? Math.max(0, (t0 - tsNs) / 1e6 - st.exposureMs / 2) : 20.0;
            double tl = (now - t0) / 1e6;
            procAvg = procAvg == 0 ? tl : procAvg * 0.9 + tl * 0.1;
            lastProc = (float) tl; tagsSeen = good.size(); multiUsed = field != null;
            lastLatency = (float) (cl + tl);

            // ---- publish (one atomic array + Limelight-style scalars, same timestamp)
            int n = 0; for (PoseSolver.Result r : res) if (r != null) n++;
            double[] data = new double[13 + 11 * n];
            data[0] = seq; data[1] = lastLatency; data[3] = n; data[2] = fps;
            if (field != null) { data[4] = field.tagCount; data[5] = field.tx; data[6] = field.ty; data[7] = field.tz;
                data[8] = field.qw; data[9] = field.qx; data[10] = field.qy; data[11] = field.qz; data[12] = field.reprojErr; }
            int o = 13;
            for (int i = 0; i < good.size(); i++) {
                PoseSolver.Result r = res.get(i); if (r == null) continue;
                data[o++] = good.get(i).id; data[o++] = r.tx; data[o++] = r.ty; data[o++] = r.tz;
                data[o++] = r.qw; data[o++] = r.qx; data[o++] = r.qy; data[o++] = r.qz;
                data[o++] = r.reprojErr; data[o++] = r.ambiguity; data[o++] = good.get(i).margin;
            }
            double[] scalars = {tv, tx, ty, ta, tid, tl, cl, td, good.size(), seq};   // order = Nt4Client.SCALARS
            seq++;
            nt.publishFrame(data, scalars, lastLatency); // every frame, even with 0 tags, so tv=0 / hb keep flowing
            tick();
        } catch (Throwable t) { pipelineError = t.getClass().getSimpleName() + ": " + t.getMessage(); }
    }

    private static double quadArea(double[] c) {
        double s = 0;
        for (int i = 0; i < 4; i++) { int j = (i + 1) & 3; s += c[i * 2] * c[j * 2 + 1] - c[j * 2] * c[i * 2 + 1]; }
        return Math.abs(s) / 2;
    }

    private void tick() {
        frames++;
        long n = SystemClock.elapsedRealtime();
        if (n - fpsT >= 1000) { fps = frames; frames = 0; fpsT = n; }
    }

    private final Runnable statusTick = new Runnable() {
        @Override public void run() {
            String calTxt = activeCalib == null ? "?" : activeCalib.estimated ? "NOT CALIBRATED (estimated lens)" :
                    String.format(Locale.US, "calibrated (rms %.2f px)", activeCalib.rms);
            status.setText(String.format(Locale.US, "%s\n%d fps | proc %.0f ms | latency %.0f ms\ntags: %d %s\n%s\nNT4: %s\nfield: %s\n%dx%d",
                    calibMode ? "MODE: CALIBRATE" : "MODE: VISION", fps, lastProc, lastLatency, tagsSeen, multiUsed ? "(multi-tag pose)" : "",
                    calTxt, nt.status, fieldMsg, st.width, st.height)
                    + "\n" + cam.info + "\n" + powerInfo() + "\nprimary: id " + primId + "  tx " + String.format(Locale.US, "%.1f ty %.1f dist %.2f", primTx, primTy, primDist)
                    + (pipelineError.isEmpty() ? "" : "\nPIPELINE ERROR (recovering): " + pipelineError) + "\n" + netInfo());
            hud.setText(String.format(Locale.US, "id %d  tx %.1f  ty %.1f  dist %.2f m  tags %d  %d fps\nNT4: %s\n%s%s\n(long-press to exit match mode)",
                    primId, primTx, primTy, primDist, tagsSeen, fps, nt.isConnected() ? "CONNECTED" : nt.status, powerInfo(),
                    pipelineError.isEmpty() ? "" : "\nERROR: " + pipelineError));
            calStatus.setText("Views: " + calibrator.count() + " (need 15+, vary angle/distance)\n" + calMsg);
            checkTick++;
            if (checkTick % 4 == 0) { stressTick(); if (settingsOverlay.getVisibility() == View.VISIBLE) refreshChecks(); }
            if (miniStatus.getVisibility() == View.VISIBLE)
                miniStatus.setText(String.format(Locale.US, "%d fps  id %d  tx %.1f ty %.1f  dist %.2fm  tags %d\n%s",
                        fps, primId, primTx, primTy, primDist, tagsSeen, nt.isConnected() ? "NT4 connected" : nt.status));
            ui.postDelayed(this, 250);
        }
    };

    // ------------------------------------------------------------------ UI
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private Button button(String t, View.OnClickListener l) { Button b = new Button(this); b.setText(t); b.setOnClickListener(l); return b; }

    private TextView label(String t) { TextView v = new TextView(this); v.setText(t); v.setTextColor(Color.WHITE); v.setTextSize(12); return v; }

    private void textField(LinearLayout p, String name, String init, boolean numeric, java.util.function.Consumer<String> cb) {
        p.addView(label(name));
        EditText e = new EditText(this); e.setText(init); e.setTextColor(Color.WHITE); e.setSingleLine(true); e.setTextSize(14);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { try { cb.accept(s.toString()); st.save(); } catch (Exception ignored) {} }
        });
        p.addView(e);
    }

    /** value = min + progress*step */
    private void slider(LinearLayout p, String name, double min, double step, int steps, double init, java.util.function.DoubleConsumer cb) {
        TextView t = label(name + ": " + init); p.addView(t);
        SeekBar s = new SeekBar(this); s.setMax(steps); s.setProgress((int) Math.round((init - min) / step));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int v, boolean user) {
                double val = min + v * step; t.setText(String.format(Locale.US, "%s: %.2f", name, val));
                if (user) { cb.accept(val); st.save(); }
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        p.addView(s);
    }

    private void nativeConfigure() { if (nativeOk) AprilTagNative.configure(tagHandle, st.threads, st.decimate, st.sigma); }

    private double batteryTempC() {
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return b == null ? Double.NaN : b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0;
    }

    private boolean isCharging() {
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return b != null && b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
    }

    private int thermalStatus() {
        return Build.VERSION.SDK_INT >= 29 ? ((PowerManager) getSystemService(POWER_SERVICE)).getCurrentThermalStatus() : 0;
    }

    /**
     * Settings is a full-screen overlay drawn ON TOP of the camera view, never something that resizes it.
     * Previously the settings panel was permanently docked to the right of the screen (and grew wider still
     * when "setup" was open), which changed the camera view's layout margins every time it opened/closed or
     * even just when the on-screen keyboard appeared for a text field - the live preview would visibly
     * resize and shift on screen. The camera view's own layout params are never touched here, so it now
     * stays put, full size, at all times - like an actual camera app.
     */
    private void showSettings(boolean on) {
        settingsOverlay.setVisibility(on ? View.VISIBLE : View.GONE);
        gearButton.setVisibility(on || matchMode ? View.GONE : View.VISIBLE);
        miniStatus.setVisibility(on || matchMode ? View.GONE : View.VISIBLE);
        if (on) refreshChecks();
    }

    private void refreshChecks() {
        if (camInfo == null) camInfo = cam.inspect();
        SelfCheck.Inputs in = new SelfCheck.Inputs();
        in.camPermission = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        in.nativeOk = nativeOk; in.nativeMsg = nativeMsg; in.abi64 = Build.SUPPORTED_64_BIT_ABIS.length > 0;
        in.cam = camInfo; in.reqExposureMs = st.exposureMs; in.reqIso = st.iso;
        boolean fresh = SystemClock.elapsedRealtime() - cam.resAtMs < 2000;
        in.actExposureNs = fresh ? cam.resExposureNs : -1; in.actIso = cam.resIso; in.actAeMode = cam.resAeMode;
        in.framesFlowing = SystemClock.elapsedRealtime() - lastFrameMs < 2000; in.fps = fps; in.procAvgMs = procAvg;
        in.tempC = batteryTempC(); in.tempStartC = tempStart; in.thermal = thermalStatus(); in.charging = isCharging();
        in.batteryExempt = ((PowerManager) getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName());
        in.ntConnected = nt.isConnected(); in.ntStatus = nt.status;
        List<SelfCheck.Item> items = SelfCheck.run(in);
        int warn = 0, fail = 0;
        checkBox.removeAllViews();
        for (SelfCheck.Item it : items) {
            if (it.level == SelfCheck.Level.WARN) warn++; if (it.level == SelfCheck.Level.FAIL) fail++;
            TextView t = new TextView(this); t.setTextSize(12); t.setPadding(0, dp(3), 0, dp(3));
            int color = it.level == SelfCheck.Level.OK ? 0xFF6EE07A : it.level == SelfCheck.Level.WARN ? 0xFFFFD34D : it.level == SelfCheck.Level.FAIL ? 0xFFFF6B6B : 0xFFB0B0B0;
            t.setTextColor(color); t.setText("[" + it.level + "] " + it.name + "\n      " + it.detail);
            checkBox.addView(t);
        }
        checkSummary.setText(fail > 0 ? fail + " problem(s), " + warn + " warning(s)" : warn > 0 ? warn + " warning(s) - see below" : "All checks passed");
        checkSummary.setTextColor(fail > 0 ? 0xFFFF6B6B : warn > 0 ? 0xFFFFD34D : 0xFF6EE07A);
    }

    private void startStress() {
        stressStart = SystemClock.elapsedRealtime(); stressT0 = batteryTempC(); stressMin = 999; stressSum = 0; stressN = 0; stressThermalMax = 0;
        if (calibMode) calibMode = false;
        stressText.setText("Running 60 s test... keep the phone where it will be on the robot.");
    }

    private void stressTick() {
        if (stressStart == 0) return;
        long el = (SystemClock.elapsedRealtime() - stressStart) / 1000;
        if (el > 1) { stressMin = Math.min(stressMin, fps); stressSum += fps; stressN++; }
        stressThermalMax = Math.max(stressThermalMax, thermalStatus());
        if (el < 60) { stressText.setText("Running... " + el + "/60 s   fps " + fps + "   " + String.format(Locale.US, "%.1f C", batteryTempC())); return; }
        double dT = batteryTempC() - stressT0, avg = stressN == 0 ? 0 : stressSum / (double) stressN;
        boolean bad = stressMin < 15 || stressThermalMax >= 3, ok = stressMin >= 25 && dT < 6 && stressThermalMax < 2;
        stressText.setText(String.format(Locale.US, "%s: fps min %d avg %.0f, temp %+.1f C (now %.1f), thermal max %d, detector %.0f ms.%s",
                bad ? "FAIL" : ok ? "PASS" : "WARN", stressMin, avg, dT, batteryTempC(), stressThermalMax, procAvg,
                ok ? "" : " Add airflow / lower resolution / raise Decimate, then re-test."));
        stressText.setTextColor(bad ? 0xFFFF6B6B : ok ? 0xFF6EE07A : 0xFFFFD34D);
        stressStart = 0;
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(Color.BLACK);

        // ===== Camera view: full-size, centered, aspect-locked, and NEVER resized by the settings UI.
        // (Previously a permanently-docked side panel reserved a margin here, and that margin changed
        // width whenever setup opened/closed - the live preview would visibly jump/resize. Settings is
        // now a full-screen overlay drawn on top instead, so this box's layout params never change.)
        frame = new AspectFrame(this);
        tv = new TextureView(this); overlay = new Overlay(this);
        frame.addView(tv, new FrameLayout.LayoutParams(-1, -1)); frame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-1, -1); fl.gravity = Gravity.CENTER;
        root.addView(frame, fl);
        tv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture s, int w, int h) { surface = s; restartCamera(); }
            public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) { applyPreviewTransform(); }
            public boolean onSurfaceTextureDestroyed(SurfaceTexture s) { surface = null; return true; }
            public void onSurfaceTextureUpdated(SurfaceTexture s) {}
        });

        // ===== Settings: one full-screen overlay covering everything below (Vision/Calibrate mode,
        // self-check, robot, camera, AprilTag detector, filtering/sorting, calibration, match/keep-alive).
        // Shown once automatically on first run, and afterwards only via the small gear button - see
        // showSettings()/onCreate. Being an overlay (not a docked panel) means opening or closing it, or
        // the keyboard appearing for a text field inside it, can never resize or shift the camera view.
        settingsOverlay = new ScrollView(this); settingsOverlay.setBackgroundColor(0xF0101820);
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(dp(16), dp(16), dp(16), dp(16));
        settingsOverlay.addView(p);
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(-1, -1); root.addView(settingsOverlay, sl);

        TextView title = label("PHONEVISION SETTINGS"); title.setTextSize(18); p.addView(title);
        p.addView(label("This screen only opens automatically the first time; after that, tap the gear icon."));
        status = label(""); status.setTextSize(11); p.addView(status);

        LinearLayout modes = new LinearLayout(this);
        modes.addView(button("Vision", v -> { calibMode = false; }), new LinearLayout.LayoutParams(0, -2, 1));
        modes.addView(button("Calibrate", v -> { calibMode = true; calMsg = ""; }), new LinearLayout.LayoutParams(0, -2, 1));
        p.addView(modes);

        // --- self-check
        p.addView(label("\n— Setup & self-check —"));
        p.addView(label("Checks update live while you change the settings below. Point the camera at a lit scene."));
        checkSummary = label(""); checkSummary.setTextSize(14); p.addView(checkSummary);
        checkBox = new LinearLayout(this); checkBox.setOrientation(LinearLayout.VERTICAL); p.addView(checkBox);
        p.addView(button("Grant camera permission", v -> requestPermissions(new String[]{Manifest.permission.CAMERA}, 1)));
        p.addView(button("Run 60 s heat / speed test", v -> startStress()));
        stressText = label(""); p.addView(stressText);

        // --- robot
        p.addView(label("\n— Robot —"));
        textField(p, "Team number (robot = 10.TE.AM.2)", "" + st.team, true, x -> { st.team = Integer.parseInt(x); nt.setHost(st.host()); });
        textField(p, "Robot IP override (blank = use team number)", st.hostOverride, false, x -> { st.hostOverride = x; nt.setHost(st.host()); });
        p.addView(button("Use roboRIO USB address 172.22.11.2", v -> { st.hostOverride = "172.22.11.2"; st.save(); nt.setHost(st.host()); calMsg = "Host set to 172.22.11.2 (restart UI to see field)"; }));
        textField(p, "Camera name (NT: /PhoneVision/<name>/data)", st.camName, false, x -> { if (!x.trim().isEmpty()) { st.camName = x.trim(); restartNt(); } });
        p.addView(label("Field layout (tag positions for multi-tag pose)"));
        List<String> fields = new ArrayList<>();
        try { for (String f : getAssets().list("fields")) fields.add(f.replace(".json", "")); } catch (Exception ignored) {}
        fields.add("custom");
        Spinner fs = new Spinner(this); fs.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fields));
        fs.setSelection(Math.max(0, fields.indexOf(st.field)));
        fs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> a, View v, int pos, long id) { st.field = fields.get(pos); st.save(); loadField(); }
            public void onNothingSelected(AdapterView<?> a) {}
        });
        p.addView(fs);
        p.addView(label("'custom' reads Android/data/org.phonevision/files/field.json"));
        Switch mt = new Switch(this); mt.setText("Multi-tag field pose"); mt.setTextColor(Color.WHITE); mt.setChecked(st.multiTag);
        mt.setOnCheckedChangeListener((bv, on) -> { st.multiTag = on; st.save(); });
        p.addView(mt);

        // --- camera
        p.addView(label("\n— Camera —"));
        List<Size> sizes = cam.listSizes(); List<String> names = new ArrayList<>(); int sel = 0;
        for (int i = 0; i < sizes.size(); i++) { names.add(sizes.get(i).getWidth() + "x" + sizes.get(i).getHeight()); if (sizes.get(i).getWidth() == st.width && sizes.get(i).getHeight() == st.height) sel = i; }
        Spinner sp = new Spinner(this); sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names)); sp.setSelection(sel);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
                Size z = sizes.get(pos);
                if (z.getWidth() != st.width || z.getHeight() != st.height) { st.width = z.getWidth(); st.height = z.getHeight(); st.save(); restartCamera(); }
            }
            public void onNothingSelected(AdapterView<?> a) {}
        });
        p.addView(label("Resolution (calibration is per resolution)")); p.addView(sp);
        slider(p, "Exposure ms", 0.1, 0.1, 199, st.exposureMs, v -> { st.exposureMs = (float) v; cam.applySettings(); });
        slider(p, "ISO", 100, 50, 62, st.iso, v -> { st.iso = (int) v; cam.applySettings(); });
        slider(p, "Focus (diopters, 0=far)", 0, 0.1, 100, st.focusDiopters, v -> { st.focusDiopters = (float) v; cam.applySettings(); });

        // --- detector
        p.addView(label("\n— AprilTag —"));
        textField(p, "Tag size (m, black square edge-to-edge)", "" + st.tagSizeM, true, x -> st.tagSizeM = Double.parseDouble(x));
        textField(p, "Priority tag ID (-1 = none; always sorts first when visible)", "" + st.priorityId, false, x -> st.priorityId = Integer.parseInt(x.trim()));
        textField(p, "Min decision margin (higher = stricter)", "" + st.minMargin, true, x -> st.minMargin = Integer.parseInt(x));
        slider(p, "Decimate (higher = faster, less range)", 1, 0.5, 6, st.decimate, v -> { st.decimate = (float) v; nativeConfigure(); });
        slider(p, "Threads", 1, 1, 7, st.threads, v -> { st.threads = (int) v; nativeConfigure(); });
        slider(p, "Blur sigma", 0, 0.1, 20, st.sigma, v -> { st.sigma = (float) v; nativeConfigure(); });
        p.addView(label(""));
        p.addView(label("— Filtering and Sorting (like Limelight's target sort) —"));
        p.addView(label("Sort mode (index 0 = primary target -> tv/tx/ty/ta/tid):"));
        TagSorter.Mode[] sortModes = TagSorter.Mode.values(); String[] modeLabels = new String[sortModes.length];
        for (int i = 0; i < sortModes.length; i++) modeLabels[i] = sortModes[i].label;
        Spinner sortModeSpinner = new Spinner(this);
        sortModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modeLabels));
        sortModeSpinner.setSelection(st.sortMode.ordinal());
        sortModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { st.sortMode = sortModes[pos]; st.save(); }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        p.addView(sortModeSpinner);
        textField(p, "Allow only these IDs (blank = all). e.g. \"1, 3-6, 12\"", st.idAllow, false, x -> { st.idAllow = x; st.refreshIdFilters(); st.save(); });
        textField(p, "Block these IDs (always loses, even if allowed)", st.idBlock, false, x -> { st.idBlock = x; st.refreshIdFilters(); st.save(); });

        // --- calibration
        p.addView(label("\n— Calibration —"));
        calStatus = label(""); p.addView(calStatus);
        p.addView(label("Show a printed checkerboard (inner corners set below) from many angles/distances. Keep it flat and fill the frame at times."));
        LinearLayout cb = new LinearLayout(this);
        cb.addView(button("Capture", v -> { calibMode = true; captureReq.set(true); }), new LinearLayout.LayoutParams(0, -2, 1));
        cb.addView(button("Compute", v -> {
            Settings.Calib c = calibrator.compute();
            if (c == null) { calMsg = "Need at least 10 views"; return; }
            st.saveCalib(c); refreshSolver();
            calMsg = String.format(Locale.US, "Saved! rms=%.3f px (<0.5 is good)\nfx=%.1f fy=%.1f cx=%.1f cy=%.1f", c.rms, c.fx, c.fy, c.cx, c.cy);
        }), new LinearLayout.LayoutParams(0, -2, 1));
        cb.addView(button("Reset", v -> { calibrator.reset(); st.clearCalib(); refreshSolver(); calMsg = "Cleared"; }), new LinearLayout.LayoutParams(0, -2, 1));
        p.addView(cb);
        textField(p, "Board inner corners: columns", "" + st.boardCols, true, s -> { st.boardCols = Integer.parseInt(s); calibrator = new Calibrator(st.boardCols, st.boardRows, st.squareM); });
        textField(p, "Board inner corners: rows", "" + st.boardRows, true, s -> { st.boardRows = Integer.parseInt(s); calibrator = new Calibrator(st.boardCols, st.boardRows, st.squareM); });
        textField(p, "Square size (m)", "" + st.squareM, true, s -> { st.squareM = Double.parseDouble(s); calibrator = new Calibrator(st.boardCols, st.boardRows, st.squareM); });

        // --- match / persistence
        p.addView(label("\n— Match / keep-alive —"));
        p.addView(button("MATCH MODE (dim screen, hide UI)", v -> setMatchMode(true)));
        Switch sm = new Switch(this); sm.setText("Start in match mode"); sm.setTextColor(Color.WHITE); sm.setChecked(st.startInMatchMode);
        sm.setOnCheckedChangeListener((bv, on) -> { st.startInMatchMode = on; st.save(); });
        p.addView(sm);
        slider(p, "Match-mode brightness", 0.0, 0.01, 30, st.dimBrightness, v -> { st.dimBrightness = (float) v; });
        p.addView(button("Allow background running (battery)", v -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))); }
            catch (Exception e) { calMsg = "Open Settings > Battery > PhoneVision > Unrestricted"; }
        }));
        p.addView(label("Screen stays on while the app is open and a foreground service + wake lock keep it alive. Set Screen timeout: never, disable screen lock, and use Screen Pinning to prevent exits."));

        Switch ssw = new Switch(this); ssw.setText("Also open this screen on the next launch"); ssw.setTextColor(Color.WHITE); ssw.setChecked(st.showSetupAtStart);
        ssw.setOnCheckedChangeListener((bv, on) -> { st.showSetupAtStart = on; st.save(); });
        p.addView(ssw);
        p.addView(button("DONE", v -> { st.onboardingDone = true; st.save(); showSettings(false); }));
        settingsOverlay.setVisibility(View.GONE);

        // ===== small always-available controls, layered on top; none of these ever change frame's layout
        gearButton = button("\u2699", v -> showSettings(true));
        FrameLayout.LayoutParams gl = new FrameLayout.LayoutParams(dp(48), dp(48)); gl.gravity = Gravity.TOP | Gravity.RIGHT; gl.topMargin = dp(10); gl.rightMargin = dp(10);
        root.addView(gearButton, gl);

        miniStatus = new TextView(this); miniStatus.setTextColor(Color.WHITE); miniStatus.setTextSize(11); miniStatus.setBackgroundColor(0x88000000); miniStatus.setPadding(dp(8), dp(6), dp(8), dp(6));
        FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(-2, -2); ml.gravity = Gravity.TOP | Gravity.LEFT; ml.topMargin = dp(10); ml.leftMargin = dp(10);
        root.addView(miniStatus, ml);

        hud = new TextView(this); hud.setTextColor(Color.GREEN); hud.setTextSize(14); hud.setBackgroundColor(0x88000000); hud.setPadding(dp(8), dp(8), dp(8), dp(8)); hud.setVisibility(View.GONE);
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(-2, -2); hl.gravity = Gravity.TOP | Gravity.LEFT; root.addView(hud, hl);
        root.setOnLongClickListener(v -> { if (matchMode) setMatchMode(false); return true; });
        frame.setOnLongClickListener(v -> { if (matchMode) setMatchMode(false); return true; });
        hud.setOnLongClickListener(v -> { if (matchMode) setMatchMode(false); return true; });
        setContentView(root);
    }
}
