# PocketCam Blender

PocketCam Blender is an open-source virtual camera control system that uses an Android phone as a handheld motion controller for Blender cameras.

The long-term goal is to provide a production-ready Android app and Blender add-on that can stream phone orientation or ARCore 6DoF pose data to Blender with low latency, then apply, record, smooth, and bake camera motion for animation, previz, virtual production, and AI video motion-reference workflows.

## Project status

This repository is in the planning and MVP implementation stage.

Current priority:

1. Define the repository structure and protocol.
2. Build a Blender add-on that can receive pose packets from a local test sender.
3. Build an Android rotation-only sender.
4. Extend the Android app to ARCore 6DoF tracking.
5. Add recording, smoothing, calibration, and release packaging.

## Core architecture

```text
Android phone
  Sensors / ARCore pose tracking
        |
        | USB ADB reverse / Wi-Fi / TCP / UDP / WebSocket
        v
Blender Python add-on
  Pose receiver / coordinate converter / smoothing / keyframe baker
        |
        v
Blender Camera
  Real-time position, rotation, FOV, and recorded animation
```

## Components

```text
android-app/       Android app source and notes
blender-addon/     Blender add-on source and notes
docs/              Design documents and implementation guidance
protocol/          Pose message schema and protocol notes
scripts/           Local test tools and development helpers
```

## Blender Receiver MVP

The first working MVP lives in `blender-addon/pocketcam_blender`.

It provides a Blender 4.x add-on that:

- Starts and stops a UDP server on port `8765`.
- Receives UTF-8 JSON pose packets.
- Parses protocol v1 quaternion rotations in `[x, y, z, w]` order.
- Stores only the latest valid pose.
- Applies the latest rotation to the active scene camera.
- Adds controls under `View3D > Sidebar > PocketCam`.

For Version 0.1.0, incoming quaternions are treated as Blender-space rotations. Android and ARCore coordinate conversion are intentionally left for a later phase.

### Install the add-on for development

1. Open Blender 4.x.
2. Copy `blender-addon/pocketcam_blender` into Blender's user add-ons directory.
   - Windows example: `%APPDATA%\Blender Foundation\Blender\4.0\scripts\addons\pocketcam_blender`
3. Open `Edit > Preferences > Add-ons`.
4. Search for `PocketCam Blender Receiver`.
5. Enable the add-on.

### Send test pose packets

With a Blender scene open and the PocketCam server started from the sidebar, run:

```bash
python scripts/send_test_pose.py --host 127.0.0.1 --port 8765 --duration 10
```

To send one identity rotation packet:

```bash
python scripts/send_test_pose.py --once
```

## Target feature set

- Android rotation-only tracking using Rotation Vector Sensor.
- Android ARCore 6DoF tracking using camera pose.
- USB connection through `adb reverse`.
- Wi-Fi connection mode for quick testing.
- Blender camera live control.
- Recenter/calibrate controls.
- Axis and coordinate conversion controls.
- Motion smoothing and scale controls.
- Record start/stop.
- Keyframe baking.
- Take management.
- Installable Blender add-on ZIP.
- Release APK.
- Clear open-source documentation.

## Recommended MVP order

### Phase 1: Blender receiver MVP

Build the Blender add-on first. It should open a local UDP or WebSocket server, receive test pose messages, and apply the latest pose to the active camera.

The first test sender can be a simple Python script. This avoids depending on Android before the Blender side is working.

### Phase 2: Android rotation MVP

Build an Android app that reads the Rotation Vector Sensor and streams quaternion rotation to Blender.

This proves that the phone can control Blender camera orientation in real time.

### Phase 3: ARCore 6DoF MVP

Add ARCore tracking and stream camera translation plus quaternion rotation.

This turns the phone into a true handheld virtual camera.

### Phase 4: Production tools

Add recording, smoothing, scale controls, recentering, take management, and keyframe baking.

### Phase 5: Open-source release quality

Add installation guides, examples, release builds, contribution guidelines, and issue templates.

## First protocol draft

Pose messages should initially use JSON for readability. Binary packets can be added later for lower latency.

```json
{
  "type": "pose",
  "version": 1,
  "timestamp": 1730000000.123,
  "mode": "arcore",
  "position": [0.12, 1.45, -0.80],
  "rotation": [0.01, 0.72, 0.02, 0.69],
  "fov": 60.0,
  "tracking": "normal"
}
```

Rotation order for JSON v1:

```text
rotation = [x, y, z, w]
```

Position is expressed in the sender coordinate system. The Blender add-on is responsible for coordinate conversion.

## Development principles

- Prefer a working vertical slice over many half-finished features.
- Keep Android and Blender loosely coupled through a documented protocol.
- Keep the Blender receiver tolerant of missing optional fields.
- Always drop stale pose packets in live mode and apply only the latest pose.
- Keep calibration and coordinate conversion explicit and documented.
- Optimize after the JSON MVP works.

## Local connection concept

For low-latency USB testing, use ADB reverse:

```bash
adb reverse tcp:8765 tcp:8765
```

Then the Android app can connect to `127.0.0.1:8765`, while Blender listens on the PC at port `8765`.

## License

The repository currently uses the MIT License. This can be changed before the first public release if needed.
