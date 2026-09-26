package org.phonevision;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pose estimation from tag corners, output in WPILib conventions:
 *  camera frame X fwd / Y left / Z up, tag frame X out of tag / Y right / Z up, field frame per WPILib layout.
 *
 * Single tag : IPPE_SQUARE (both solutions), best one refined with Levenberg-Marquardt. ambiguity = best/second error.
 * Multi tag  : all corners of all known tags solved jointly (SQPNP + LM) against the field layout -> camera pose in field.
 */
public class PoseSolver {
    public static class Det { public int id; public double[] corners = new double[8]; public double margin; }

    public static class Result {
        public double tx, ty, tz, qw, qx, qy, qz;   // single tag: camera->tag; multi: field->camera
        public double reprojErr, ambiguity;          // px RMS-ish; single-tag ambiguity in [0,1], lower is better
        public int tagCount = 1;
    }

    private final Mat K = Mat.eye(3, 3, CvType.CV_64F);
    private final MatOfDouble D = new MatOfDouble(0, 0, 0, 0, 0);

    public PoseSolver(Settings.Calib c) {
        K.put(0, 0, c.fx); K.put(1, 1, c.fy); K.put(0, 2, c.cx); K.put(1, 2, c.cy);
        D.fromArray(c.dist);
    }

    // OpenCV camera frame -> WPILib camera frame (rows), and (tag frame of apriltag lib -> WPILib tag frame)^T
    private static final double[] C = {0, 0, 1, -1, 0, 0, 0, -1, 0};
    private static final double[] CT = {0, -1, 0, 0, 0, -1, 1, 0, 0};
    private static final double[] AT = {0, 1, 0, 0, 0, -1, -1, 0, 0};

    /** corners: apriltag order BL, BR, TR, TL (8 values). Returns null on failure. */
    public Result solveSingle(double[] corners, double tagSize) {
        double s = tagSize / 2;
        MatOfPoint3f obj = new MatOfPoint3f(new Point3(-s, s, 0), new Point3(s, s, 0), new Point3(s, -s, 0), new Point3(-s, -s, 0));
        MatOfPoint2f img = new MatOfPoint2f(new Point(corners[0], corners[1]), new Point(corners[2], corners[3]),
                new Point(corners[4], corners[5]), new Point(corners[6], corners[7]));
        List<Mat> rv = new ArrayList<>(), tv = new ArrayList<>();
        Mat err = new Mat(), rvecGuess = new Mat(), tvecGuess = new Mat(); // guesses unused (useExtrinsicGuess=false)
        int n = Calib3d.solvePnPGeneric(obj, img, K, D, rv, tv, false, Calib3d.SOLVEPNP_IPPE_SQUARE, rvecGuess, tvecGuess, err);
        if (n < 1) { obj.release(); img.release(); err.release(); rvecGuess.release(); tvecGuess.release(); releaseAll(rv); releaseAll(tv); return null; }
        double e0 = err.get(0, 0)[0], e1 = n > 1 ? err.get(1, 0)[0] : Double.MAX_VALUE;
        int best = (n > 1 && e1 < e0) ? 1 : 0;
        Mat rvec = rv.get(best).clone(), tvec = tv.get(best).clone();
        releaseAll(rv); releaseAll(tv); err.release(); rvecGuess.release(); tvecGuess.release();
        Calib3d.solvePnPRefineLM(obj, img, K, D, rvec, tvec);
        Mat R = new Mat(); Calib3d.Rodrigues(rvec, R);
        double[] r = new double[9]; R.get(0, 0, r);
        double[] t = new double[3]; tvec.get(0, 0, t);
        Result out = new Result();
        out.tx = C[0] * t[0] + C[1] * t[1] + C[2] * t[2];
        out.ty = C[3] * t[0] + C[4] * t[1] + C[5] * t[2];
        out.tz = C[6] * t[0] + C[7] * t[1] + C[8] * t[2];
        setQuat(mul(mul(C, r), AT), out);
        double eb = Math.min(e0, e1), eo = Math.max(e0, e1);
        out.reprojErr = eb;
        out.ambiguity = (n > 1 && eo > 1e-9) ? eb / eo : 0;
        obj.release(); img.release(); rvec.release(); tvec.release(); R.release();
        return out;
    }

    /** solvePnPGeneric/solvePnP hand back native Mat objects; without this they only get freed whenever the
     *  GC happens to run their finalizer, which under sustained per-frame use let native memory pile up and
     *  caused periodic stalls. Release explicitly, right after use, every frame. */
    private static void releaseAll(List<Mat> l) { for (Mat m : l) if (m != null) m.release(); }

    /**
     * Limelight-style angles of the tag centre relative to the crosshair (principal point), degrees:
     * tx positive = target to the RIGHT, ty positive = target UP. Lens distortion is removed first and the
     * centre is the intersection of the tag's diagonals (exact under perspective).
     */
    public double[] centerAngles(double[] c) {
        MatOfPoint2f src = new MatOfPoint2f(new Point(c[0], c[1]), new Point(c[2], c[3]), new Point(c[4], c[5]), new Point(c[6], c[7]));
        MatOfPoint2f dst = new MatOfPoint2f();
        Calib3d.undistortPoints(src, dst, K, D);
        Point[] p = dst.toArray();
        src.release(); dst.release();
        double x1 = p[0].x, y1 = p[0].y, x2 = p[2].x, y2 = p[2].y, x3 = p[1].x, y3 = p[1].y, x4 = p[3].x, y4 = p[3].y;
        double den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4), px, py;
        if (Math.abs(den) < 1e-12) { px = (x1 + x2 + x3 + x4) / 4; py = (y1 + y2 + y3 + y4) / 4; }
        else {
            px = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / den;
            py = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / den;
        }
        return new double[]{Math.toDegrees(Math.atan(px)), -Math.toDegrees(Math.atan(py))};
    }

    /** Joint solve of all given detections against the layout. Needs >= 2 tags known to the layout. */
    public Result solveField(List<Det> dets, FieldLayout layout, double tagSize) {
        List<Point3> op = new ArrayList<>(); List<Point> ip = new ArrayList<>(); int used = 0;
        for (Det d : dets) {
            double[] c = layout.corners(d.id, tagSize);
            if (c == null) continue;
            for (int k = 0; k < 4; k++) { op.add(new Point3(c[k * 3], c[k * 3 + 1], c[k * 3 + 2])); ip.add(new Point(d.corners[k * 2], d.corners[k * 2 + 1])); }
            used++;
        }
        if (used < 2) return null;
        MatOfPoint3f obj = new MatOfPoint3f(); obj.fromList(op);
        MatOfPoint2f img = new MatOfPoint2f(); img.fromList(ip);
        Mat rvec = new Mat(), tvec = new Mat();
        if (!Calib3d.solvePnP(obj, img, K, D, rvec, tvec, false, Calib3d.SOLVEPNP_SQPNP)) { obj.release(); img.release(); rvec.release(); tvec.release(); return null; }
        Calib3d.solvePnPRefineLM(obj, img, K, D, rvec, tvec);
        // reprojection RMS
        MatOfPoint2f proj = new MatOfPoint2f();
        Calib3d.projectPoints(obj, rvec, tvec, K, D, proj);
        Point[] a = proj.toArray(), b = img.toArray(); double se = 0;
        for (int i = 0; i < a.length; i++) se += Math.pow(a[i].x - b[i].x, 2) + Math.pow(a[i].y - b[i].y, 2);
        Mat R = new Mat(); Calib3d.Rodrigues(rvec, R);
        double[] r = new double[9]; R.get(0, 0, r);
        double[] t = new double[3]; tvec.get(0, 0, t);
        // camera position in field = -R^T t ; orientation of WPILib camera axes in field = R^T * C^T
        double[] rt = {r[0], r[3], r[6], r[1], r[4], r[7], r[2], r[5], r[8]};
        Result out = new Result();
        out.tx = -(rt[0] * t[0] + rt[1] * t[1] + rt[2] * t[2]);
        out.ty = -(rt[3] * t[0] + rt[4] * t[1] + rt[5] * t[2]);
        out.tz = -(rt[6] * t[0] + rt[7] * t[1] + rt[8] * t[2]);
        setQuat(mul(rt, CT), out);
        out.reprojErr = Math.sqrt(se / a.length); out.tagCount = used;
        obj.release(); img.release(); rvec.release(); tvec.release(); proj.release(); R.release();
        return out;
    }

    static double[] mul(double[] a, double[] b) {
        double[] o = new double[9];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++)
            o[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
        return o;
    }

    /** Rotation matrix (row-major) -> quaternion (w,x,y,z). */
    static void setQuat(double[] m, Result o) {
        double tr = m[0] + m[4] + m[8];
        if (tr > 0) { double s = Math.sqrt(tr + 1) * 2; o.qw = s / 4; o.qx = (m[7] - m[5]) / s; o.qy = (m[2] - m[6]) / s; o.qz = (m[3] - m[1]) / s; }
        else if (m[0] > m[4] && m[0] > m[8]) { double s = Math.sqrt(1 + m[0] - m[4] - m[8]) * 2; o.qw = (m[7] - m[5]) / s; o.qx = s / 4; o.qy = (m[1] + m[3]) / s; o.qz = (m[2] + m[6]) / s; }
        else if (m[4] > m[8]) { double s = Math.sqrt(1 + m[4] - m[0] - m[8]) * 2; o.qw = (m[2] - m[6]) / s; o.qx = (m[1] + m[3]) / s; o.qy = s / 4; o.qz = (m[5] + m[7]) / s; }
        else { double s = Math.sqrt(1 + m[8] - m[0] - m[4]) * 2; o.qw = (m[3] - m[1]) / s; o.qx = (m[2] + m[6]) / s; o.qy = (m[5] + m[7]) / s; o.qz = s / 4; }
    }
}
