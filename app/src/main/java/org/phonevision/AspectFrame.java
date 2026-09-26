package org.phonevision;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/** FrameLayout that keeps a fixed aspect ratio, fitting inside the available space. */
public class AspectFrame extends FrameLayout {
    public volatile double aspect = 16.0 / 9.0;
    public AspectFrame(Context c) { super(c); }
    @Override protected void onMeasure(int ws, int hs) {
        int w = View.MeasureSpec.getSize(ws), h = View.MeasureSpec.getSize(hs);
        if (w > h * aspect) w = (int) (h * aspect); else h = (int) (w / aspect);
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
    }
}
