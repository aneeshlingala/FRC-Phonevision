package phonevision;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns PhoneVision frames into field-relative robot poses for a pose estimator.
 *
 * <pre>
 * var est = new PhoneVisionEstimator(vision, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField),
 *     new Transform3d(new Translation3d(0.3, 0.0, 0.5), new Rotation3d(0, Math.toRadians(-20), 0)));
 * // periodic():
 * for (var e : est.update())
 *   poseEstimator.addVisionMeasurement(e.pose().toPose2d(), e.timestampSeconds(), e.stdDevs());
 * </pre>
 *
 * The layout here must be the same field the app is set to (used only for the single-tag fallback).
 */
public class PhoneVisionEstimator {
  public record Estimate(
      Pose3d pose,
      double timestampSeconds,
      Matrix<N3, N1> stdDevs,
      int tagCount,
      double avgDistanceMeters,
      boolean multiTag) {}

  private final PhoneVision vision;
  private final AprilTagFieldLayout layout;
  private final Transform3d robotToCamera;

  /** Prefer the phone's joint multi-tag solve when it has one. */
  public boolean useMultiTag = true;
  /** Reject a multi-tag solve whose reprojection RMS (pixels) is above this. */
  public double maxMultiTagReprojPx = 2.0;
  /** Single-tag: reject if the two PnP solutions are too similar (0 = clear, 1 = coin flip). */
  public double maxAmbiguity = 0.2;
  /** Ignore tags farther than this (single-tag) / drop multi-tag solves with a larger average distance. */
  public double maxDistanceMeters = 5.0;
  /** Base std devs {x m, y m, theta rad}, multiplied by (1 + d^2 / 30). */
  public double[] singleTagStdDevs = {4, 4, 8};
  public double[] multiTagStdDevs = {0.5, 0.5, 1};

  /** @param robotToCamera phone camera pose on the robot (WPILib: X fwd, Y left, Z up). */
  public PhoneVisionEstimator(PhoneVision vision, AprilTagFieldLayout layout, Transform3d robotToCamera) {
    this.vision = vision;
    this.layout = layout;
    this.robotToCamera = robotToCamera;
  }

  /** Call every loop; returns one estimate per new frame that had usable data. */
  public List<Estimate> update() {
    List<Estimate> out = new ArrayList<>();
    for (PhoneVision.Frame f : vision.drainFrames()) estimate(f).ifPresent(out::add);
    return out;
  }

  public Optional<Estimate> estimate(PhoneVision.Frame frame) {
    if (useMultiTag && frame.fieldPose().isPresent()) {
      PhoneVision.FieldPose fp = frame.fieldPose().get();
      double d = averageDistance(frame);
      if (fp.reprojErrorPx() <= maxMultiTagReprojPx && d <= maxDistanceMeters) {
        Pose3d robot = fp.cameraPose().transformBy(robotToCamera.inverse());
        return Optional.of(
            new Estimate(robot, frame.timestampSeconds(), stdDevs(multiTagStdDevs, d), fp.tagCount(), d, true));
      }
    }
    return singleTag(frame);
  }

  private static double averageDistance(PhoneVision.Frame f) {
    if (f.tags().isEmpty()) return 0;
    double s = 0;
    for (PhoneVision.Tag t : f.tags()) s += t.distanceMeters();
    return s / f.tags().size();
  }

  private Optional<Estimate> singleTag(PhoneVision.Frame frame) {
    double sx = 0, sy = 0, sz = 0, sw = 0, dSum = 0, closest = Double.MAX_VALUE;
    Pose3d closestPose = null;
    int n = 0;
    for (PhoneVision.Tag t : frame.tags()) {
      Optional<Pose3d> tagPose = layout.getTagPose(t.id());
      if (tagPose.isEmpty() || t.ambiguity() > maxAmbiguity) continue;
      double d = t.distanceMeters();
      if (d > maxDistanceMeters) continue;
      Pose3d robot =
          tagPose.get().transformBy(t.cameraToTag().inverse()).transformBy(robotToCamera.inverse());
      double w = 1.0 / Math.max(d * d, 1e-3);
      sx += robot.getX() * w;
      sy += robot.getY() * w;
      sz += robot.getZ() * w;
      sw += w;
      dSum += d;
      n++;
      if (d < closest) {
        closest = d;
        closestPose = robot;
      }
    }
    if (n == 0) return Optional.empty();
    Pose3d pose = new Pose3d(new Translation3d(sx / sw, sy / sw, sz / sw), closestPose.getRotation());
    double avg = dSum / n;
    return Optional.of(
        new Estimate(pose, frame.timestampSeconds(), stdDevs(n > 1 ? multiTagStdDevs : singleTagStdDevs, avg), n, avg, false));
  }

  private static Matrix<N3, N1> stdDevs(double[] base, double d) {
    double scale = 1 + d * d / 30.0;
    return VecBuilder.fill(base[0] * scale, base[1] * scale, base[2] * scale);
  }
}
