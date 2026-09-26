package org.phonevision;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

/** Checkerboard camera calibration (OpenCV). Pattern size = number of INNER corners. */
public class Calibrator {
    private final Size pattern; private final double square;
    private final List<Mat> obj = new ArrayList<>(), img = new ArrayList<>();
    private Size imgSize;
    public volatile float[] lastCorners = null; // for overlay (x,y pairs)

    public Calibrator(int cols, int rows, double squareM) { pattern = new Size(cols, rows); square = squareM; }
    public int count() { return img.size(); }
    public void reset() { obj.clear(); img.clear(); lastCorners = null; }

    /** Looks for the board; if found and capture==true, stores the view. @return true if board found */
    public boolean process(Mat gray, boolean capture) {
        imgSize = gray.size();
        MatOfPoint2f c = new MatOfPoint2f();
        boolean found = Calib3d.findChessboardCorners(gray, pattern, c,
                Calib3d.CALIB_CB_ADAPTIVE_THRESH | Calib3d.CALIB_CB_NORMALIZE_IMAGE | Calib3d.CALIB_CB_FAST_CHECK);
        if (!found) { lastCorners = null; return false; }
        Imgproc.cornerSubPix(gray, c, new Size(11, 11), new Size(-1, -1),
                new TermCriteria(TermCriteria.EPS + TermCriteria.COUNT, 30, 0.01));
        float[] f = new float[(int) c.total() * 2];
        c.get(0, 0, f);
        lastCorners = f;
        if (capture) {
            List<Point3> pts = new ArrayList<>();
            for (int i = 0; i < (int) pattern.height; i++)
                for (int j = 0; j < (int) pattern.width; j++) pts.add(new Point3(j * square, i * square, 0));
            MatOfPoint3f o = new MatOfPoint3f(); o.fromList(pts);
            obj.add(o); img.add(c);
        }
        return true;
    }

    /** @return calibration, or null if not enough views. */
    public Settings.Calib compute() {
        if (img.size() < 10 || imgSize == null) return null;
        Mat K = Mat.eye(3, 3, CvType.CV_64F), D = Mat.zeros(5, 1, CvType.CV_64F);
        List<Mat> rv = new ArrayList<>(), tv = new ArrayList<>();
        double rms = Calib3d.calibrateCamera(obj, img, imgSize, K, D, rv, tv);
        Settings.Calib r = new Settings.Calib();
        r.fx = K.get(0, 0)[0]; r.fy = K.get(1, 1)[0]; r.cx = K.get(0, 2)[0]; r.cy = K.get(1, 2)[0];
        for (int i = 0; i < 5; i++) r.dist[i] = D.get(i, 0)[0];
        r.rms = rms; return r;
    }
}
