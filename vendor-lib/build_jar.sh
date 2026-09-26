#!/usr/bin/env bash
# Optional: rebuild the jar against your real WPILib jars. Usage: WPILIB_CP="/path/wpilibj.jar:/path/ntcore.jar:/path/wpimath.jar:/path/apriltag.jar" ./build_jar.sh
set -e; cd "$(dirname "$0")"; rm -rf /tmp/pvbuild && mkdir -p /tmp/pvbuild
javac --release 17 -cp "$WPILIB_CP" -d /tmp/pvbuild src/main/java/phonevision/*.java
(cd /tmp/pvbuild && jar cf "$OLDPWD/maven/org/phonevision/PhoneVision-java/1.0.0/PhoneVision-java-1.0.0.jar" phonevision)
echo rebuilt jar
