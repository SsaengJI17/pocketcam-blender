# Codex Instructions

This document tells Codex how to work in this repository.

## Project goal

Build an open-source system that uses an Android phone as a virtual camera controller for Blender.

The system consists of:

1. Android application written in Kotlin.
2. Blender add-on written in Python.
3. A documented transport protocol.

## Priority order

1. Blender receiver MVP.
2. Python test sender.
3. Android rotation-only sender.
4. ARCore 6DoF sender.
5. Recording and smoothing tools.
6. Packaging and release automation.

## Technology choices

### Android

- Kotlin
- Android Studio
- SensorManager
- ARCore SDK
- OkHttp WebSocket or TCP sockets

### Blender

- Python 3
- bpy
- mathutils
- socket
- threading

## Coding principles

- Keep code simple and well documented.
- Prefer small, testable modules.
- Avoid premature optimization.
- Preserve backward compatibility for protocol version 1.
- Use type hints where practical.

## Communication protocol

See `docs/protocol.md` and `protocol/pose-message.schema.json`.

## Blender add-on architecture

Suggested modules:

```text
blender-addon/
  __init__.py
  receiver.py
  state.py
  coordinate.py
  smoothing.py
  operators.py
  ui.py
```

## Android architecture

Suggested modules:

```text
android-app/
  app/
    src/main/java/.../
      MainActivity.kt
      PoseSender.kt
      SensorTracker.kt
      ArCoreTracker.kt
      SettingsRepository.kt
```

## First implementation task

Implement a Blender add-on that:

1. Starts a local UDP server on port 8765.
2. Receives JSON pose packets.
3. Stores only the latest pose.
4. Applies rotation to the active camera using a timer.
5. Provides Start and Stop buttons in the 3D View sidebar.

## Definition of done for MVP

- Blender camera follows test quaternion data in real time.
- No crashes after repeated start/stop.
- Works in Blender 4.x.
- README instructions are sufficient to reproduce.

## Non-goals for the first implementation

- ARCore support.
- Binary protocol.
- Advanced smoothing.
- Multi-device support.
- Lens controls.

## Commit conventions

Use concise commit messages, for example:

- Add UDP pose receiver
- Apply quaternion to active camera
- Add Android rotation sender

## Testing expectations

- Manual testing in Blender.
- Unit tests for coordinate conversion where practical.
- Confirm stale packets are ignored.
