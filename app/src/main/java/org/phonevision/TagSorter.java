package org.phonevision;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Limelight-style target sort order. Index 0 after sorting is the "primary" target (tv/tx/ty/ta/tid). */
public final class TagSorter {
    private TagSorter() {}

    public enum Mode {
        LARGEST("Largest"), SMALLEST("Smallest"), HIGHEST("Highest"), LOWEST("Lowest"),
        LEFTMOST("Leftmost"), RIGHTMOST("Rightmost"), CLOSEST_TO_CROSSHAIR("Closest to crosshair"),
        NEAREST("Nearest (distance)"), LOWEST_ID("Lowest ID"), HIGHEST_ID("Highest ID");
        public final String label;
        Mode(String l) { label = l; }
        public static Mode parse(String name) { try { return valueOf(name); } catch (Exception e) { return LARGEST; } }
    }

    /** tx: + right (deg), ty: + up (deg), ta: % of image, dist: meters or <= 0 if unknown. */
    public static final class Cand {
        public final int id; public final double tx, ty, ta, dist; public final Object ref;
        public Cand(int id, double tx, double ty, double ta, double dist, Object ref) { this.id = id; this.tx = tx; this.ty = ty; this.ta = ta; this.dist = dist; this.ref = ref; }
    }

    /** Sorts in place. A visible priority tag (priorityId >= 0) always goes first, like Limelight. */
    public static void sort(List<Cand> l, Mode m, int priorityId) {
        Comparator<Cand> c;
        switch (m) {
            case SMALLEST: c = Comparator.comparingDouble((Cand x) -> x.ta); break;
            case HIGHEST: c = Comparator.comparingDouble((Cand x) -> -x.ty); break;
            case LOWEST: c = Comparator.comparingDouble((Cand x) -> x.ty); break;
            case LEFTMOST: c = Comparator.comparingDouble((Cand x) -> x.tx); break;
            case RIGHTMOST: c = Comparator.comparingDouble((Cand x) -> -x.tx); break;
            case CLOSEST_TO_CROSSHAIR: c = Comparator.comparingDouble((Cand x) -> Math.hypot(x.tx, x.ty)); break;
            case NEAREST: c = Comparator.comparingDouble((Cand x) -> x.dist > 0 ? x.dist : Double.MAX_VALUE); break;
            case LOWEST_ID: c = Comparator.comparingInt((Cand x) -> x.id); break;
            case HIGHEST_ID: c = Comparator.comparingInt((Cand x) -> -x.id); break;
            default: c = Comparator.comparingDouble((Cand x) -> -x.ta); break;   // LARGEST
        }
        final Comparator<Cand> base = c.thenComparingInt(x -> x.id);
        Collections.sort(l, (a, b) -> {
            boolean pa = priorityId >= 0 && a.id == priorityId, pb = priorityId >= 0 && b.id == priorityId;
            if (pa != pb) return pa ? -1 : 1;
            return base.compare(a, b);
        });
    }
}
