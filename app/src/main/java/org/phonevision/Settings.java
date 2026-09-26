package org.phonevision;

import android.content.Context;
import android.content.SharedPreferences;

/** All persistent settings. Fields are public; call save() after changing. */
public class Settings {
    private final SharedPreferences p;
    public int team = 0;
    public String hostOverride = "";       // empty => 10.TE.AM.2
    public String camName = "phone";     // NT topic: /PhoneVision/<camName>/data
    public int minMargin = 30;             // reject weak detections (decision margin)
    public int maxHamming = 0;
    public boolean multiTag = true;
    public int priorityId = -1;            // -1 = largest tag wins tx/ty/ta/tid
    public boolean startInMatchMode = false;
    public boolean showSetupAtStart = true;
    public float dimBrightness = 0.02f;    // match-mode screen brightness (0..1)
    public String field = "2026-rebuilt-welded"; // asset name or "custom" (external field.json)
    public int width = 1280, height = 720;
    public float exposureMs = 4f;
    public int iso = 400;
    public float focusDiopters = 0.2f;     // 0.2 D = focus at 5 m
    public double tagSizeM = 0.1651;       // 6.5 in FRC tag
    public float decimate = 2f;
    public float sigma = 0f;
    public int threads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1); // leave one core free for camera/UI
    public int boardCols = 9, boardRows = 6; // INNER corners of the checkerboard
    public double squareM = 0.025;
    // --- Fiducial filtering / sorting (Limelight-style) ---
    public String idAllow = "";                 // empty = all IDs allowed; else "1, 3-6, 12"
    public String idBlock = "";                 // blocked IDs always lose, even if also allowed
    public TagSorter.Mode sortMode = TagSorter.Mode.LARGEST;
    /** Parsed once and cached; call refreshIdFilters() after changing idAllow/idBlock. Reparsing every
     *  frame (regex split + HashSet build, 30x/sec) was a real source of per-frame lag. */
    public volatile java.util.Set<Integer> idAllowSet = TagFilter.parse(idAllow);
    public volatile java.util.Set<Integer> idBlockSet = TagFilter.parse(idBlock);
    public void refreshIdFilters() { idAllowSet = TagFilter.parse(idAllow); idBlockSet = TagFilter.parse(idBlock); }
    /** True after the settings screen has been completed once; from then on it only opens via the gear button. */
    public boolean onboardingDone = false;

    public Settings(Context c) {
        p = c.getSharedPreferences("pv", Context.MODE_PRIVATE);
        team = p.getInt("team", team); hostOverride = p.getString("host", hostOverride);
        camName = p.getString("cam", camName); minMargin = p.getInt("marg", minMargin); maxHamming = p.getInt("ham", maxHamming);
        multiTag = p.getBoolean("multi", multiTag); priorityId = p.getInt("prio", priorityId);
        startInMatchMode = p.getBoolean("match", startInMatchMode); showSetupAtStart = p.getBoolean("setup", showSetupAtStart); dimBrightness = p.getFloat("dim", dimBrightness); field = p.getString("field", field); width = p.getInt("w", width); height = p.getInt("h", height);
        exposureMs = p.getFloat("exp", exposureMs); iso = p.getInt("iso", iso);
        focusDiopters = p.getFloat("focus", focusDiopters);
        tagSizeM = Double.longBitsToDouble(p.getLong("tag", Double.doubleToLongBits(tagSizeM)));
        decimate = p.getFloat("dec", decimate); sigma = p.getFloat("sig", sigma); threads = p.getInt("thr", threads);
        boardCols = p.getInt("bc", boardCols); boardRows = p.getInt("br", boardRows);
        squareM = Double.longBitsToDouble(p.getLong("sq", Double.doubleToLongBits(squareM)));
        idAllow = p.getString("idAllow", idAllow);
        idBlock = p.getString("idBlock", idBlock);
        sortMode = TagSorter.Mode.parse(p.getString("sortMode", sortMode.name()));
        onboardingDone = p.getBoolean("onboardingDone", onboardingDone);
        sanitize();
        refreshIdFilters();
    }

    /** Clamp anything a user could type into an unsafe value, so a bad entry can never crash or hang the pipeline. */
    private void sanitize() {
        if (threads < 1) threads = 1; if (threads > 8) threads = 8;
        if (decimate < 1f) decimate = 1f; if (decimate > 8f) decimate = 8f;
        if (sigma < 0f) sigma = 0f;
        if (boardCols < 3) boardCols = 3; if (boardRows < 3) boardRows = 3;
        if (!(squareM > 0)) squareM = 0.025;
        if (!(tagSizeM > 0)) tagSizeM = 0.1651;
        if (width < 160) width = 160; if (height < 120) height = 120;
        if (minMargin < 0) minMargin = 0; if (maxHamming < 0) maxHamming = 0;
    }

    public void save() {
        p.edit().putInt("team", team).putString("host", hostOverride).putString("cam", camName).putInt("marg", minMargin).putInt("ham", maxHamming).putBoolean("multi", multiTag).putInt("prio", priorityId).putBoolean("match", startInMatchMode).putBoolean("setup", showSetupAtStart).putFloat("dim", dimBrightness).putString("field", field)
            .putInt("w", width).putInt("h", height).putFloat("exp", exposureMs).putInt("iso", iso)
            .putFloat("focus", focusDiopters).putLong("tag", Double.doubleToLongBits(tagSizeM))
            .putFloat("dec", decimate).putFloat("sig", sigma).putInt("thr", threads)
            .putInt("bc", boardCols).putInt("br", boardRows).putLong("sq", Double.doubleToLongBits(squareM))
            .putString("idAllow", idAllow).putString("idBlock", idBlock).putString("sortMode", sortMode.name())
            .putBoolean("onboardingDone", onboardingDone)
            .apply();
    }

    public String host() {
        if (!hostOverride.trim().isEmpty()) return hostOverride.trim();
        return "10." + (team / 100) + "." + (team % 100) + ".2";
    }

    // ---- calibration storage, one entry per resolution ----
    public Calib loadCalib() {
        String s = p.getString("cal_" + width + "x" + height, null);
        return s == null ? null : Calib.parse(s);
    }
    public void saveCalib(Calib c) { p.edit().putString("cal_" + width + "x" + height, c.toString()).apply(); }
    public void clearCalib() { p.edit().remove("cal_" + width + "x" + height).apply(); }

    /** Pinhole intrinsics + 5 distortion coefficients. */
    public static class Calib {
        public double fx, fy, cx, cy; public double[] dist = new double[5]; public double rms = -1; public boolean estimated;
        public String toString() {
            return fx + "," + fy + "," + cx + "," + cy + "," + dist[0] + "," + dist[1] + "," + dist[2] + "," + dist[3] + "," + dist[4] + "," + rms;
        }
        static Calib parse(String s) {
            String[] a = s.split(","); Calib c = new Calib();
            c.fx = Double.parseDouble(a[0]); c.fy = Double.parseDouble(a[1]); c.cx = Double.parseDouble(a[2]); c.cy = Double.parseDouble(a[3]);
            for (int i = 0; i < 5; i++) c.dist[i] = Double.parseDouble(a[4 + i]);
            c.rms = Double.parseDouble(a[9]); return c;
        }
    }
}
