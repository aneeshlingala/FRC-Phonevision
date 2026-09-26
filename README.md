# PhoneVision – Limelight-style AprilTag camera on an Android phone (NetworkTables 4)

```
app/          Android Studio project (Camera2 + AprilRobotics apriltag via NDK, OpenCV calibration/pose)
vendor-lib/   ONE-TIME drop-in WPILib vendor library  (install.sh / install.bat, PhoneVision.json, maven/)
              dropin/PhoneVisionHelpers.java = same helper as a single file (fallback)
tools/        mock_robot.py (fake roboRIO on a laptop), Nt4ClientDemo.java
```

## 1. Robot side: install the vendor library once
```
cd vendor-lib
./install.sh 2026            # Windows: install.bat 2026     (use your WPILib year)
```
This puts the jar in `~/wpilib/<year>/maven` and the vendordep in `~/wpilib/<year>/vendordeps` (Windows: `C:\Users\Public\wpilib\<year>`). It's the same offline layout WPILib uses.
Then in any robot project: VS Code → WPILib → **Manage Vendor Libraries → Install new libraries (offline) → PhoneVision**
(or `./install.sh 2026 /path/to/project` to also drop `PhoneVision.json` into that project's `vendordeps/`). Fallback: copy `dropin/PhoneVisionHelpers.java` into your project (package `phonevision`).

```java
import phonevision.PhoneVisionHelpers;

if (PhoneVisionHelpers.getTV()) {                       // tag in view
  double tx = PhoneVisionHelpers.getTX();               // deg, + = target to the right of the crosshair
  double ty = PhoneVisionHelpers.getTY();               // deg, + = target up
  double ta = PhoneVisionHelpers.getTA();               // % of image
  int    id = PhoneVisionHelpers.getTID();              // primary tag id (-1 none)
  double d3 = PhoneVisionHelpers.getDistanceMeters();   // 3D camera->tag distance from the solved pose
  double dy = PhoneVisionHelpers.getDistanceFromTY(camHeightM, tagCenterHeightM, camPitchDeg); // classic ty formula
}
double latencyMs = PhoneVisionHelpers.getLatencyTotalMs();          // tl + cl
double captureTime = PhoneVisionHelpers.getCaptureTimestampSeconds(); // FPGA time, for pose estimators
boolean ok = PhoneVisionHelpers.isConnected();
```
Other getters: `getTagCount, getLatencyPipeline (tl), getLatencyCapture (cl), getResult()`; every getter has a `(String cameraName)` overload (default name `"phone"`, change with `setDefaultCameraName`).
The jar also contains the optional pose classes `PhoneVision` / `PhoneVisionEstimator` (field pose for `addVisionMeasurement`).

## 2. Phone side
Build: Android Studio + NDK + CMake, open `app/`, Run. Then in the app: set **team number** (robot = 10.TE.AM.2) or the **roboRIO USB address 172.22.11.2** button; **Calibrate** (checkerboard, 20-30 captures, RMS < 0.5 px) – tx/ty/distance are only as accurate as this; set **tag size** (0.1651 m); short exposure, fixed focus.
NT topics (all stamped with capture time): `/PhoneVision/<cam>/{tv,tx,ty,ta,tid,tl,cl,td,tc,hb}` + `/data` (pose array). `td` = -1 when unavailable.

### Sorting and filtering fiducials (like Limelight)
In the side panel, **"Filtering and Sorting"**:
* **Sort mode**: Largest, Smallest, Highest, Lowest, Leftmost, Rightmost, Closest to crosshair, Nearest (distance), Lowest ID, Highest ID. Whichever tag sorts to position 0 is the **primary target** and drives `tv/tx/ty/ta/tid`, exactly like Limelight's target sort.
* **Priority tag ID** (in the AprilTag section): if that ID is visible, it always sorts first regardless of the chosen mode.
* **Allow only these IDs** / **Block these IDs**: comma-separated, with ranges (`"1, 3-6, 12"`). A block list always wins if an ID is in both. Blank allow list = every ID is allowed.
Tag ID is shown on-screen over every detected tag, and the primary tag's ID is shown in the status panel, the match-mode HUD, and published as `tid`.

## What I checked in this pass
This zip started from a repo you uploaded. Before adding anything, I compiled it and found it did **not build**:
* `settings.gradle`/`app/build.gradle` wired the **robot-side vendor library** (`vendor-lib`, which needs WPILib on its classpath) in as a dependency of the **Android app module** — the app could not build at all. Removed; `vendor-lib` is now only built for the robot, via `install.sh`/`build_jar.sh`, completely separate from the phone app.
* A hand-added filter/sort patch didn't compile (missing imports, a shadowed variable), and had a bug that would have **crashed the app** the first time a filtered-out tag appeared, because the on-screen tag boxes and their text labels were built from two lists of different lengths. Rewrote filtering/sorting using the tested `TagFilter`/`TagSorter` classes described above; boxes and labels are now always built from the same final list, so this can't happen again.
* Added a try/catch around the whole per-frame pipeline: an unexpected error on one frame now shows up as a status message ("PIPELINE ERROR (recovering)") instead of crashing the app mid-match.
* Added basic range clamping in `Settings` (threads, decimate, board size, tag/square size, etc.) so a bad manual entry can't hang the native detector or the calibrator.
I compiled the whole app against the real Android and OpenCV Java APIs and re-ran the filter/sort unit tests (18 cases) against the exact files in this zip. I have not built or run the app on a device — see "NOT verified" below.

### Setup & self-check screen (opens at every start, and via the SETUP / SELF-CHECK button)
This is where you find out, on the actual phone, the things I could not test from a PC. It shows live pass/warn/fail rows:
* **Camera2 hardware level + manual exposure support**: exposure/ISO ranges and min focus distance from the camera itself.
* **Manual exposure test (live)**: compares what the app asked for with what the sensor *actually used* in the last frame (also catches auto-exposure overriding you).
* **AprilTag native library**: shows whether the NDK library loaded and the phone's ABI. A failed load no longer crashes the app; detection is just disabled and the row turns red.
* **Frame rate and detector speed** (ms per frame), with advice (lower resolution / raise Decimate).
* **Heat**: battery temperature, Android thermal status, and rise since app start. **Run 60 s heat / speed test** measures sustained fps, temperature rise and throttling and gives PASS/WARN/FAIL. Run it with the phone mounted where it will live.
* **Power** (on battery vs powered), **battery-optimization exemption**, **roboRIO NT4 link**.
The Robot link, Camera (resolution / exposure / ISO / focus), and Match / keep-alive settings live in this screen, so you tune them while watching the checks. Tick "Show this screen at start" off once everything is green. The main side panel keeps calibration and detector settings.

### Staying alive during a match
* Screen stays on while the app is open (`FLAG_KEEP_SCREEN_ON`), shows over the lock screen, and can turn the screen on.
* A **foreground service + partial wake lock** keeps the process alive at foreground priority.
* **Watchdog**: if frames stop for 3 s, or the camera disconnects/errors, the camera restarts itself. NT reconnects automatically.
* **MATCH MODE** button (or "Start in match mode"): hides the panel, dims the screen (brightness slider), immersive fullscreen, big green tx/ty/distance readout + battery %/temp/thermal warnings. **Long-press** anywhere to exit.
* "Allow background running" opens the battery-optimization exemption. Also set Screen timeout = Never, disable the lock screen, and use Android **Screen pinning** so nobody exits the app.

### USB reality check
The app connects to the roboRIO's NT server over IP, so the USB link must appear to the phone as a network interface. The status panel lists active interfaces; if a `172.22.11.x` (roboRIO USB-B) interface shows up, NT4 connects. Whether it appears depends on your ROM having USB network *host* drivers (rndis_host/cdc_ncm) – many don't, and I can't test it. Reliable alternative: USB-Ethernet OTG adapter into the robot radio/switch (use team number). Follow current FRC rules for radios/power on the robot.

## What I verified (PC only)
* Native apriltag JNI builds and detects (fixed a compile bug); corner order confirmed vs. the official tag image.
* NT4 client vs a real ntcore server: all topics, timestamps within ~1 ms of intended, one-frame multi-message writes.
* tx/ty math vs exact geometry through a distorted lens: <0.0001 deg. Pose pipeline end-to-end on the 2026 layout: multi-tag median 0.7 cm / 0.12 deg.
* App compiled against real Android + OpenCV Java APIs (found/fixed `readAllBytes` crash on Android < 13, latency sign error, watchdog bug).
* Setup-screen check logic: 13 unit tests pass (`tools/SelfCheckTest.java`); the screen itself compiles but has not been seen on a device.
* Helper library: 13 behavioral tests pass on a fake NT layer; installer tested with a fake HOME.
* `TagFilter`/`TagSorter` (ID allow/block parsing, all 10 sort modes, priority-tag override): 18 unit tests pass.
## NOT verified
* Running on the Redmi 6 Pro (Camera2 manual controls, thermals, first Gradle/NDK build), foreground-service behavior on your ROM, a real USB link. The new Setup screen is how you test the first three on the phone.
* The jar was compiled against small stand-ins for WPILib's NT/geometry classes, and the vendordep on GradleRIO itself. If VS Code rejects it, use `dropin/PhoneVisionHelpers.java` or run `build_jar.sh` with your real WPILib jars.
