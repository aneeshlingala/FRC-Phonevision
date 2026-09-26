package phonevision;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.RobotController;

/**
 * Limelight-style static helpers for the PhoneVision Android app (NetworkTables 4).
 *
 * <pre>
 * if (PhoneVisionHelpers.getTV()) {
 *   double tx = PhoneVisionHelpers.getTX();          // degrees, + = target to the right
 *   double ty = PhoneVisionHelpers.getTY();          // degrees, + = target up
 *   double d  = PhoneVisionHelpers.getDistanceMeters();               // 3D camera->tag, from the solved pose
 *   double d2 = PhoneVisionHelpers.getDistanceFromTY(0.50, 1.45, 25); // classic ty formula
 * }
 * </pre>
 * Every getter has an overload taking the camera name (the "Camera name" set in the app, default "phone").
 * Values are read from /PhoneVision/&lt;name&gt;/{tv,tx,ty,ta,tid,tl,cl,td,tc,hb}.
 */
public final class PhoneVisionHelpers {
  private PhoneVisionHelpers() {}

  private static String defaultName = "phone";

  /** Change the camera name used by the no-argument getters. */
  public static void setDefaultCameraName(String name) {
    defaultName = name;
  }

  private static NetworkTable table(String name) {
    return NetworkTableInstance.getDefault().getTable("PhoneVision").getSubTable(name);
  }

  private static double get(String name, String key) {
    return table(name).getEntry(key).getDouble(0.0);
  }

  // ---------------------------------------------------------------- basic Limelight outputs
  /** True if a valid tag is in view (Limelight "tv"). */
  public static boolean getTV(String name) {
    return get(name, "tv") >= 0.5;
  }

  /** Horizontal offset from the crosshair to the target, degrees. Positive = target is to the right. */
  public static double getTX(String name) {
    return get(name, "tx");
  }

  /** Vertical offset from the crosshair to the target, degrees. Positive = target is up. */
  public static double getTY(String name) {
    return get(name, "ty");
  }

  /** Target area, percent of the image (0-100). */
  public static double getTA(String name) {
    return get(name, "ta");
  }

  /** AprilTag ID of the primary target, or -1 if none. */
  public static int getTID(String name) {
    return (int) Math.round(table(name).getEntry("tid").getDouble(-1.0));
  }

  /** Number of tags currently detected. */
  public static int getTagCount(String name) {
    return (int) Math.round(get(name, "tc"));
  }

  /** Pipeline (processing) latency on the phone, ms (Limelight "tl"). */
  public static double getLatencyPipeline(String name) {
    return get(name, "tl");
  }

  /** Capture latency, ms: mid-exposure until the phone started processing (Limelight "cl"). */
  public static double getLatencyCapture(String name) {
    return get(name, "cl");
  }

  /** tl + cl in milliseconds. */
  public static double getLatencyTotalMs(String name) {
    return getLatencyPipeline(name) + getLatencyCapture(name);
  }

  // ---------------------------------------------------------------- distance
  /**
   * 3D distance (meters) from the camera to the primary tag, from the pose solved on the phone.
   * Needs a calibrated camera and the correct tag size set in the app. Returns -1 if unavailable.
   */
  public static double getDistanceMeters(String name) {
    return getTV(name) ? get(name, "td") : -1.0;
  }

  /**
   * Classic Limelight distance: d = (targetHeight - cameraHeight) / tan(cameraPitch + ty).
   * Result is horizontal distance along the floor in meters. Returns -1 if the geometry is invalid.
   *
   * @param cameraHeightMeters lens height above the floor
   * @param targetHeightMeters height of the tag's center above the floor
   * @param cameraPitchDegrees camera tilt up from horizontal (positive = up)
   */
  public static double getDistanceFromTY(
      String name, double cameraHeightMeters, double targetHeightMeters, double cameraPitchDegrees) {
    if (!getTV(name)) return -1.0;
    double t = Math.tan(Math.toRadians(cameraPitchDegrees + getTY(name)));
    if (Math.abs(t) < 1e-6) return -1.0;
    double d = (targetHeightMeters - cameraHeightMeters) / t;
    return d > 0 ? d : -1.0;
  }

  // ---------------------------------------------------------------- timing / health
  /** Capture time of the latest frame in FPGA seconds (same base as Timer.getFPGATimestamp()). */
  public static double getCaptureTimestampSeconds(String name) {
    return table(name).getEntry("hb").getLastChange() / 1e6;
  }

  /** True if the phone has published within the last 0.5 s. */
  public static boolean isConnected(String name) {
    long last = table(name).getEntry("hb").getLastChange();
    return last > 0 && RobotController.getFPGATime() - last < 500_000;
  }

  // ---------------------------------------------------------------- one-shot snapshot
  /** Values of one frame (read back-to-back; use hb/seq if you need strict atomicity). */
  public record Result(
      boolean valid,
      double tx,
      double ty,
      double ta,
      int tid,
      double distanceMeters,
      double latencyMs,
      double captureTimestampSeconds) {}

  public static Result getResult(String name) {
    return new Result(
        getTV(name),
        getTX(name),
        getTY(name),
        getTA(name),
        getTID(name),
        getDistanceMeters(name),
        getLatencyTotalMs(name),
        getCaptureTimestampSeconds(name));
  }

  // ---------------------------------------------------------------- default-name overloads
  public static boolean getTV() { return getTV(defaultName); }
  public static double getTX() { return getTX(defaultName); }
  public static double getTY() { return getTY(defaultName); }
  public static double getTA() { return getTA(defaultName); }
  public static int getTID() { return getTID(defaultName); }
  public static int getTagCount() { return getTagCount(defaultName); }
  public static double getLatencyPipeline() { return getLatencyPipeline(defaultName); }
  public static double getLatencyCapture() { return getLatencyCapture(defaultName); }
  public static double getLatencyTotalMs() { return getLatencyTotalMs(defaultName); }
  public static double getDistanceMeters() { return getDistanceMeters(defaultName); }
  public static double getDistanceFromTY(double camHeightM, double targetHeightM, double camPitchDeg) {
    return getDistanceFromTY(defaultName, camHeightM, targetHeightM, camPitchDeg);
  }
  public static double getCaptureTimestampSeconds() { return getCaptureTimestampSeconds(defaultName); }
  public static boolean isConnected() { return isConnected(defaultName); }
  public static Result getResult() { return getResult(defaultName); }
}
