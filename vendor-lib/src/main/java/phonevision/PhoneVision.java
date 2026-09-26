package phonevision;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.networktables.DoubleArraySubscriber;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.networktables.TimestampedDoubleArray;
import edu.wpi.first.wpilibj.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads AprilTag results published by the PhoneVision Android app over NetworkTables 4
 * (topic /PhoneVision/&lt;name&gt;/data). Timestamps are already in FPGA time and corrected for capture latency.
 *
 * <pre>PhoneVision vision = new PhoneVision("phone");</pre>
 */
public class PhoneVision implements AutoCloseable {
  /** One detected tag; cameraToTag uses WPILib conventions (camera X fwd, Y left, Z up). */
  public record Tag(
      int id, Transform3d cameraToTag, double reprojError, double ambiguity, double decisionMargin) {
    public double distanceMeters() {
      return cameraToTag.getTranslation().getNorm();
    }
  }

  /** Field-relative CAMERA pose solved on the phone from several tags at once. */
  public record FieldPose(Pose3d cameraPose, int tagCount, double reprojErrorPx) {}

  /** One camera frame. timestampSeconds is capture time in FPGA time (same base as Timer.getFPGATimestamp()). */
  public record Frame(
      int seq,
      double timestampSeconds,
      double latencyMs,
      double fps,
      List<Tag> tags,
      Optional<FieldPose> fieldPose) {}

  static final int HEADER = 13;
  static final int TAG_STRIDE = 11;

  private final DoubleArraySubscriber sub;
  private final List<Frame> pending = new ArrayList<>();
  private Frame latest;

  /** @param cameraName the "Camera name" set in the app (default "phone"). */
  public PhoneVision(String cameraName) {
    sub =
        NetworkTableInstance.getDefault()
            .getDoubleArrayTopic("/PhoneVision/" + cameraName + "/data")
            .subscribe(new double[0], PubSubOption.keepDuplicates(true), PubSubOption.pollStorage(50));
  }

  public PhoneVision() {
    this("phone");
  }

  private void poll() {
    for (TimestampedDoubleArray v : sub.readQueue()) {
      Frame f = parse(v.value, v.timestamp / 1e6);
      if (f == null) continue;
      latest = f;
      pending.add(f);
      if (pending.size() > 50) pending.remove(0);
    }
  }

  /** Most recent frame (may contain zero tags), if any has ever arrived. */
  public Optional<Frame> getLatest() {
    poll();
    return Optional.ofNullable(latest);
  }

  /** Every frame received since the last call, oldest first. Use this so no measurement is skipped. */
  public List<Frame> drainFrames() {
    poll();
    List<Frame> out = new ArrayList<>(pending);
    pending.clear();
    return out;
  }

  /** True if the phone has published within the last 0.5 s. */
  public boolean isConnected() {
    poll();
    return latest != null && Timer.getFPGATimestamp() - latest.timestampSeconds() < 0.5;
  }

  /** Package-visible for testing. */
  static Frame parse(double[] a, double timestampSeconds) {
    if (a.length < HEADER) return null;
    int n = (int) a[3];
    if (n < 0 || a.length < HEADER + TAG_STRIDE * n) return null;
    Optional<FieldPose> fp = Optional.empty();
    int multi = (int) a[4];
    if (multi >= 2) {
      Pose3d p =
          new Pose3d(new Translation3d(a[5], a[6], a[7]), new Rotation3d(new Quaternion(a[8], a[9], a[10], a[11])));
      fp = Optional.of(new FieldPose(p, multi, a[12]));
    }
    List<Tag> tags = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      int o = HEADER + TAG_STRIDE * i;
      Transform3d t =
          new Transform3d(
              new Translation3d(a[o + 1], a[o + 2], a[o + 3]),
              new Rotation3d(new Quaternion(a[o + 4], a[o + 5], a[o + 6], a[o + 7])));
      tags.add(new Tag((int) a[o], t, a[o + 8], a[o + 9], a[o + 10]));
    }
    return new Frame((int) a[0], timestampSeconds, a[1], a[2], tags, fp);
  }

  @Override
  public void close() {
    sub.close();
  }
}
