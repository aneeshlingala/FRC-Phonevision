package org.phonevision;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns live measurements into the setup-screen checklist. Threshold logic is plain static methods. */
public class SelfCheck {
    public enum Level { OK, WARN, FAIL, INFO }

    public static class Item {
        public final String name, detail; public final Level level;
        Item(String n, Level l, String d) { name = n; level = l; detail = d; }
    }

    public static class Inputs {
        public boolean camPermission, nativeOk, framesFlowing, ntConnected, charging, batteryExempt, abi64 = true;
        public String nativeMsg = "", ntStatus = "";
        public CameraController.Info cam = new CameraController.Info();
        public float reqExposureMs; public int reqIso;
        public long actExposureNs = -1; public int actIso = -1, actAeMode = -1;
        public double procAvgMs; public int fps;
        public double tempC = Double.NaN, tempStartC = Double.NaN; public int thermal;
    }

    // ---- thresholds (unit-tested)
    public static Level fpsLevel(int fps) { return fps >= 25 ? Level.OK : fps >= 15 ? Level.WARN : Level.FAIL; }
    public static Level procLevel(double ms) { return ms <= 25 ? Level.OK : ms <= 45 ? Level.WARN : Level.FAIL; }
    public static Level tempLevel(double c, int thermal) {
        if (thermal >= 3 || c >= 43) return Level.FAIL;
        if (thermal >= 2 || c >= 38) return Level.WARN;
        return Level.OK;
    }
    /** Compare what we asked for with what the sensor reported. */
    public static Level exposureLevel(long reqNs, long actNs, int aeMode) {
        if (actNs < 0) return Level.INFO;
        if (aeMode != 0) return Level.FAIL;                 // 0 = CONTROL_AE_MODE_OFF
        double r = actNs / (double) Math.max(1, reqNs);
        return (r >= 0.8 && r <= 1.25) ? Level.OK : (r >= 0.5 && r <= 2.0) ? Level.WARN : Level.FAIL;
    }
    public static long clamp(long v, long lo, long hi) { return Math.max(lo, Math.min(hi, v)); }

    public static List<Item> run(Inputs in) {
        List<Item> o = new ArrayList<>();
        CameraController.Info c = in.cam;
        o.add(new Item("Camera permission", in.camPermission ? Level.OK : Level.FAIL, in.camPermission ? "granted" : "not granted - tap Grant below"));

        o.add(new Item("Camera2 hardware level", c.level.equals("LEGACY") ? Level.WARN : Level.INFO,
                c.name + ": " + c.level + (c.level.equals("LEGACY") ? " - manual controls are usually missing" : "")));

        boolean shortOk = c.manual && c.expMinNs > 0 && c.expMinNs <= 4_000_000L;
        o.add(new Item("Manual exposure supported", c.manual ? (shortOk ? Level.OK : Level.WARN) : Level.WARN,
                c.manual ? String.format(Locale.US, "exposure %.2f-%.0f ms, ISO %d-%d, min focus %.1f D", c.expMinNs / 1e6, c.expMaxNs / 1e6, c.isoMin, c.isoMax, c.minFocusDiopters)
                         : "NOT supported: app falls back to auto exposure (blurrier, less accurate)"));

        if (c.manual) {
            long req = clamp((long) (in.reqExposureMs * 1e6), c.expMinNs, c.expMaxNs);
            Level l = exposureLevel(req, in.actExposureNs, in.actAeMode);
            String d = in.actExposureNs < 0 ? "waiting for camera frames..."
                    : String.format(Locale.US, "asked %.2f ms / ISO %d, sensor used %.2f ms / ISO %d%s", req / 1e6, in.reqIso, in.actExposureNs / 1e6, in.actIso,
                        l == Level.FAIL && in.actAeMode != 0 ? " (auto-exposure is overriding!)" : "");
            o.add(new Item("Manual exposure test (live)", l, d));
        }

        boolean nat = in.nativeOk && in.abi64;
        o.add(new Item("AprilTag native library", nat ? Level.OK : Level.FAIL,
                in.abi64 ? in.nativeMsg : "phone has no 64-bit ABI; lib is built for arm64-v8a only. " + in.nativeMsg));

        if (!in.framesFlowing) {
            o.add(new Item("Frame rate / detector speed", Level.INFO, "waiting for frames..."));
        } else {
            o.add(new Item("Frame rate", fpsLevel(in.fps), in.fps + " fps" + (in.fps < 25 ? " - lower resolution / raise decimate / cool the phone" : "")));
            o.add(new Item("Detector speed", procLevel(in.procAvgMs), String.format(Locale.US, "%.0f ms per frame%s", in.procAvgMs, in.procAvgMs > 25 ? " - raise Decimate or lower resolution" : "")));
        }

        Level tl = Double.isNaN(in.tempC) ? Level.INFO : tempLevel(in.tempC, in.thermal);
        String rise = (Double.isNaN(in.tempStartC) || Double.isNaN(in.tempC)) ? "" : String.format(Locale.US, " (%+.1f C since app start)", in.tempC - in.tempStartC);
        o.add(new Item("Heat", tl, String.format(Locale.US, "battery %.1f C, thermal status %d%s%s", in.tempC, in.thermal, rise,
                tl == Level.FAIL ? " - phone will throttle; add airflow/heatsink" : "")));

        o.add(new Item("Power", in.charging ? Level.OK : Level.WARN, in.charging ? "powered (charging)" : "ON BATTERY - power it from a regulated USB supply for matches"));
        o.add(new Item("Background running", in.batteryExempt ? Level.OK : Level.WARN, in.batteryExempt ? "battery optimization off for this app" : "not exempt from battery optimization - tap the button below"));
        o.add(new Item("roboRIO link (NT4)", in.ntConnected ? Level.OK : Level.WARN, in.ntStatus));
        return o;
    }
}
