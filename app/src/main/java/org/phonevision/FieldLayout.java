package org.phonevision;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/** Loads a WPILib AprilTag field layout JSON (same format as AprilTagFieldLayout). */
public class FieldLayout {
    private final Map<Integer, double[]> tags = new HashMap<>(); // id -> x,y,z, 3x3 rotation (row major)

    public int size() { return tags.size(); }
    public boolean has(int id) { return tags.containsKey(id); }

    public static FieldLayout parse(String json) throws JSONException {
        FieldLayout f = new FieldLayout();
        JSONArray arr = new JSONObject(json).getJSONArray("tags");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject t = arr.getJSONObject(i), p = t.getJSONObject("pose");
            JSONObject tr = p.getJSONObject("translation"), q = p.getJSONObject("rotation").getJSONObject("quaternion");
            double w = q.getDouble("W"), x = q.getDouble("X"), y = q.getDouble("Y"), z = q.getDouble("Z");
            double n = Math.sqrt(w * w + x * x + y * y + z * z); w /= n; x /= n; y /= n; z /= n;
            f.tags.put(t.getInt("ID"), new double[]{tr.getDouble("x"), tr.getDouble("y"), tr.getDouble("z"),
                1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w),
                2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w),
                2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)});
        }
        return f;
    }

    /** Field-frame corners in apriltag order (BL, BR, TR, TL) as 12 doubles, or null if the tag is unknown.
     *  WPILib tag frame: X out of the tag face, Y to the viewer's right, Z up. */
    public double[] corners(int id, double size) {
        double[] t = tags.get(id);
        if (t == null) return null;
        double s = size / 2;
        double[][] local = {{0, -s, -s}, {0, s, -s}, {0, s, s}, {0, -s, s}};
        double[] out = new double[12];
        for (int k = 0; k < 4; k++)
            for (int r = 0; r < 3; r++)
                out[k * 3 + r] = t[r] + t[3 + r * 3] * local[k][0] + t[4 + r * 3] * local[k][1] + t[5 + r * 3] * local[k][2];
        return out;
    }
}
