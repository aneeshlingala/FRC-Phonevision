#!/usr/bin/env bash
# One-time install of the PhoneVision vendor library into your WPILib offline vendor repo.
# Usage: ./install.sh [frcYear=2026] [optional: path to a robot project to add it to right now]
set -e
YEAR="${1:-2026}"; PROJECT="$2"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$HOME/wpilib/$YEAR"
mkdir -p "$ROOT/maven" "$ROOT/vendordeps"
cp -r "$HERE/maven/." "$ROOT/maven/"
sed "s/\"frcYear\": \"[0-9]*\"/\"frcYear\": \"$YEAR\"/" "$HERE/PhoneVision.json" > "$ROOT/vendordeps/PhoneVision.json"
echo "Installed to $ROOT (maven + vendordeps)."
if [ -n "$PROJECT" ]; then
  mkdir -p "$PROJECT/vendordeps"; cp "$ROOT/vendordeps/PhoneVision.json" "$PROJECT/vendordeps/"
  echo "Added to project: $PROJECT/vendordeps/PhoneVision.json"
else
  echo "Now in VS Code: WPILib > Manage Vendor Libraries > Install new libraries (offline) > PhoneVision."
fi
