package org.phonevision;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** Draws detected tag outlines / calibration corners over the preview. Coordinates are image pixels. */
public class Overlay extends View {
    public volatile List<double[]> quads = new ArrayList<>(); // 8 corner coords
    public volatile List<String> labels = new ArrayList<>();
    public volatile float[] points = null;
    public volatile int imgW = 1280, imgH = 720;
    private final Paint line = new Paint(), txt = new Paint(), dot = new Paint();

    public Overlay(Context c) {
        super(c);
        line.setColor(Color.GREEN); line.setStrokeWidth(4); line.setStyle(Paint.Style.STROKE);
        txt.setColor(Color.YELLOW); txt.setTextSize(36); txt.setShadowLayer(4, 0, 0, Color.BLACK);
        dot.setColor(Color.CYAN); dot.setStyle(Paint.Style.FILL);
    }

    @Override protected void onDraw(Canvas c) {
        float sx = getWidth() / (float) imgW, sy = getHeight() / (float) imgH;
        List<double[]> q = quads; List<String> l = labels;
        for (int i = 0; i < q.size(); i++) {
            double[] p = q.get(i);
            for (int k = 0; k < 4; k++) {
                int n = (k + 1) % 4;
                c.drawLine((float) p[k * 2] * sx, (float) p[k * 2 + 1] * sy, (float) p[n * 2] * sx, (float) p[n * 2 + 1] * sy, line);
            }
            c.drawText(l.get(i), (float) p[0] * sx, (float) p[1] * sy - 8, txt);
        }
        float[] pts = points;
        if (pts != null) for (int i = 0; i + 1 < pts.length; i += 2) c.drawCircle(pts[i] * sx, pts[i + 1] * sy, 6, dot);
    }
}
