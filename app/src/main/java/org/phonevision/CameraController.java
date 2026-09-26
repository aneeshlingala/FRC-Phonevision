package org.phonevision;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Camera2 wrapper: preview + YUV frames with manual exposure/ISO/focus and all image "enhancement" turned off. */
public class CameraController {
    public interface FrameListener { void onFrame(Image image, long timestampNs); }

    private final Context ctx; private final Settings s; private final CameraManager mgr;
    private HandlerThread thread; private Handler h;
    private CameraDevice dev; private CameraCaptureSession session; private ImageReader reader; private Surface previewSurface;
    private CameraCharacteristics chars; private String camId;
    public volatile boolean timestampRealtime = false;
    public volatile String info = "camera closed";
    /** Called (on the camera thread) when the camera disconnects or errors, so the app can restart it. */
    public volatile Runnable onLost;

    /** What the camera hardware can do (read without opening the camera). */
    public static class Info {
        public String name = "?", level = "?"; public boolean manual, timestampRealtime;
        public long expMinNs, expMaxNs; public int isoMin, isoMax; public float minFocusDiopters;
    }

    public Info inspect() {
        Info i = new Info();
        try {
            String id = findBack(); CameraCharacteristics c = mgr.getCameraCharacteristics(id); i.name = "camera " + id;
            Integer lv = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            i.level = lv == null ? "?" : lv == 0 ? "LIMITED" : lv == 1 ? "FULL" : lv == 2 ? "LEGACY" : lv == 3 ? "LEVEL_3" : "EXTERNAL";
            int[] caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            if (caps != null) for (int x : caps) if (x == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) i.manual = true;
            Range<Long> er = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            if (er != null) { i.expMinNs = er.getLower(); i.expMaxNs = er.getUpper(); }
            Range<Integer> ir = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            if (ir != null) { i.isoMin = ir.getLower(); i.isoMax = ir.getUpper(); }
            Float mf = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE); i.minFocusDiopters = mf == null ? 0 : mf;
            Integer src = c.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE);
            i.timestampRealtime = src != null && src == CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME;
        } catch (Exception e) { i.level = "ERROR " + e.getMessage(); }
        return i;
    }

    /** Values the sensor really used in the last frame (proof that manual exposure/ISO/focus took effect). */
    public volatile long resExposureNs = -1, resAtMs = 0; public volatile int resIso = -1, resAeMode = -1; public volatile float resFocus = -1;
    private final CameraCaptureSession.CaptureCallback resultCb = new CameraCaptureSession.CaptureCallback() {
        @Override public void onCaptureCompleted(CameraCaptureSession cs, CaptureRequest rq, TotalCaptureResult r) {
            Long e = r.get(CaptureResult.SENSOR_EXPOSURE_TIME); Integer iso = r.get(CaptureResult.SENSOR_SENSITIVITY);
            Integer ae = r.get(CaptureResult.CONTROL_AE_MODE); Float f = r.get(CaptureResult.LENS_FOCUS_DISTANCE);
            resExposureNs = e == null ? -1 : e; resIso = iso == null ? -1 : iso; resAeMode = ae == null ? -1 : ae; resFocus = f == null ? -1 : f;
            resAtMs = android.os.SystemClock.elapsedRealtime();
        }
    };

    public CameraController(Context c, Settings s) { ctx = c; this.s = s; mgr = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE); }

    private String findBack() throws CameraAccessException {
        for (String id : mgr.getCameraIdList()) {
            Integer f = mgr.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
            if (f != null && f == CameraCharacteristics.LENS_FACING_BACK) return id;
        }
        return mgr.getCameraIdList()[0];
    }

    public List<Size> listSizes() {
        try {
            CameraCharacteristics c = mgr.getCameraCharacteristics(findBack());
            StreamConfigurationMap m = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            List<Size> out = new ArrayList<>();
            for (Size z : m.getOutputSizes(ImageFormat.YUV_420_888))
                if (z.getWidth() >= 640 && z.getWidth() <= 1920) out.add(z);
            Collections.sort(out, (a, b) -> b.getWidth() * b.getHeight() - a.getWidth() * a.getHeight());
            return out;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    /** Rough intrinsics from the lens spec, used until the user calibrates. */
    public Settings.Calib estimateCalib(int w, int h) {
        Settings.Calib c = new Settings.Calib(); c.estimated = true;
        double f = 4.0, sw = 5.0;
        try {
            f = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)[0];
            SizeF ps = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE); sw = ps.getWidth();
        } catch (Exception ignored) {}
        c.fx = c.fy = f / sw * w; c.cx = w / 2.0; c.cy = h / 2.0; return c;
    }

    @SuppressLint("MissingPermission")
    public void start(SurfaceTexture st, FrameListener l) {
        try {
            thread = new HandlerThread("camera"); thread.start(); h = new Handler(thread.getLooper());
            camId = findBack(); chars = mgr.getCameraCharacteristics(camId);
            Integer src = chars.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE);
            timestampRealtime = src != null && src == CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME;
            reader = ImageReader.newInstance(s.width, s.height, ImageFormat.YUV_420_888, 3);
            reader.setOnImageAvailableListener(r -> {
                Image im = r.acquireLatestImage();
                if (im == null) return;
                try { l.onFrame(im, im.getTimestamp()); } finally { im.close(); }
            }, h);
            st.setDefaultBufferSize(s.width, s.height);
            previewSurface = new Surface(st);
            mgr.openCamera(camId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice d) { dev = d; createSession(); }
                @Override public void onDisconnected(CameraDevice d) { d.close(); info = "camera disconnected"; Runnable r = onLost; if (r != null) r.run(); }
                @Override public void onError(CameraDevice d, int e) { d.close(); info = "camera error " + e; Runnable r = onLost; if (r != null) r.run(); }
            }, h);
        } catch (Exception e) { info = "open failed: " + e.getMessage(); }
    }

    private void createSession() {
        try {
            dev.createCaptureSession(Arrays.asList(previewSurface, reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession cs) { session = cs; applySettings(); }
                @Override public void onConfigureFailed(CameraCaptureSession cs) { info = "session configure failed"; }
            }, h);
        } catch (Exception e) { info = "session failed: " + e.getMessage(); }
    }

    /** (Re)applies exposure/ISO/focus. Safe to call any time. */
    public void applySettings() {
        if (session == null || dev == null) return;
        try {
            CaptureRequest.Builder b = dev.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            b.addTarget(previewSurface); b.addTarget(reader.getSurface());
            int[] caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            boolean manual = false;
            for (int c : caps) if (c == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) manual = true;
            if (manual) {
                Range<Long> er = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                Range<Integer> ir = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                long exp = er.clamp((long) (s.exposureMs * 1e6));
                b.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                b.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                b.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exp);
                b.set(CaptureRequest.SENSOR_SENSITIVITY, ir.clamp(s.iso));
                b.set(CaptureRequest.SENSOR_FRAME_DURATION, 33_333_333L);
                info = "manual exposure " + exp / 1e6 + " ms, ISO " + ir.clamp(s.iso);
            } else {
                b.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                info = "MANUAL_SENSOR unsupported: using auto exposure";
            }
            Float minFocus = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
            if (minFocus != null && minFocus > 0) {
                b.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                b.set(CaptureRequest.LENS_FOCUS_DISTANCE, Math.min(s.focusDiopters, minFocus));
            }
            b.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            b.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
            b.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_FAST);
            b.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
            b.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
            session.setRepeatingRequest(b.build(), resultCb, h);
        } catch (Exception e) { info = "apply failed: " + e.getMessage(); }
    }

    public void stop() {
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (dev != null) dev.close(); } catch (Exception ignored) {}
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        if (previewSurface != null) previewSurface.release();
        if (thread != null) thread.quitSafely();
        session = null; dev = null; reader = null; previewSurface = null; thread = null;
    }
}
