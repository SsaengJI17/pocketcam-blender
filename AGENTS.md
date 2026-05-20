# AGENTS.md

This file provides shared instructions for AI coding agents working in this repository, including Codex, Antigravity, Claude Code, and other assistant-based development tools.

## Project overview

PocketCam Blender is an open-source virtual camera control system.

The goal is to use an Android phone as a real-time handheld controller for Blender cameras.

The system is split into:

1. Android app
   - Reads Rotation Vector Sensor data for rotation-only tracking.
   - Uses ARCore for 6DoF pose tracking.
   - Streams pose data to Blender over USB ADB reverse, Wi-Fi, UDP, TCP, or WebSocket.

2. Blender add-on
   - Receives pose packets.
   - Converts Android or ARCore coordinates to Blender coordinates.
   - Applies live motion to a Blender camera.
   - Records and bakes camera motion into keyframes.

3. Shared protocol
   - Defines pose packet format.
   - Keeps Android and Blender implementations loosely coupled.

## Read before working

Before making changes, read these files:

1. `PROJECT_LOG.md`
2. `README.md`
3. `ROADMAP.md`
4. `docs/protocol.md`
5. `docs/codex-instructions.md`

Even if a file name mentions Codex, the instructions apply to all AI coding agents unless this file says otherwise.

## Required workflow

1. Inspect the current repository state before editing.
2. Keep the task focused and avoid unrelated refactors.
3. Prefer small, reviewable commits or pull requests.
4. Update documentation when behavior or setup steps change.
5. After finishing meaningful work, append a new entry to `PROJECT_LOG.md`.
6. Clearly state what was tested and what was not tested.
7. If something is incomplete, document the remaining work.

## Project log requirement

Every coding session must add an entry to `PROJECT_LOG.md` using the existing template.

A log entry must include:

- Request
- Work completed
- Verification
- Known issues
- Next recommended action

Do not remove or rewrite previous log entries unless explicitly asked by the project owner.

## Current implementation priority

The first implementation target is Version 0.1.0: Blender Receiver MVP.

Implement the Blender side before the Android side.

The MVP should:

1. Create a Blender add-on under `blender-addon/`.
2. Start and stop a UDP server on port `8765`.
3. Receive UTF-8 JSON pose packets.
4. Parse protocol v1 rotation values in `[x, y, z, w]` quaternion format.
5. Store only the latest valid pose.
6. Apply the quaternion rotation to the active camera.
7. Provide Start Server and Stop Server controls in the Blender 3D View sidebar.
8. Include a Python test sender under `scripts/`.
9. Document installation and testing steps.

## Non-goals for the first MVP

Do not implement these in the first MVP unless explicitly requested:

- Android app
- ARCore tracking
- Binary protocol
- Advanced smoothing
- Multi-device support
- Lens and focus controls
- Take management
- Release automation

## Protocol expectations

Protocol v1 uses JSON.

Minimal valid packet:

```json
{
  "type": "pose",
  "version": 1,
  "rotation": [0.0, 0.0, 0.0, 1.0]
}
```

Full example:

```json
{
  "type": "pose",
  "version": 1,
  "timestamp": 1730000000.123,
  "mode": "rotation",
  "position": [0.0, 0.0, 0.0],
  "rotation": [0.0, 0.0, 0.0, 1.0],
  "fov": 50.0,
  "tracking": "normal"
}
```

Unknown fields must be ignored.
Missing optional fields must not crash the receiver.
Invalid packets should be skipped or logged without stopping the add-on.

## Coding standards

### Blender Python

- Use clear module boundaries.
- Avoid blocking Blender's main thread.
- Receive network data in a background thread.
- Apply camera updates on Blender's main thread via timer or modal operator.
- Store only the latest pose for live control.
- Stop sockets and threads cleanly.
- Avoid requiring external Python packages for the add-on MVP.

### Android Kotlin

- Use Kotlin.
- Keep sensor tracking, networking, and UI separated.
- Start with Rotation Vector Sensor before ARCore.
- Keep connection settings visible and editable.
- Prefer a simple, reliable UI over complex features.

### Documentation

- Keep setup instructions reproducible.
- Mention tested Blender and Android versions when known.
- Add troubleshooting notes when errors are discovered.

## Safety and repository hygiene

- Do not commit generated build artifacts unless they are release assets intentionally added by the owner.
- Do not commit local IDE files, private keys, tokens, keystores, or credentials.
- Do not change the license without explicit approval.
- Do not rename the project without explicit approval.
- Do not rewrite Git history.

## Suggested branch names

- `feature/blender-receiver-mvp`
- `feature/test-sender`
- `feature/android-rotation-sender`
- `feature/arcore-6dof`
- `docs/update-setup-guide`

## Suggested PR format

```markdown
## Summary

- What changed

## Verification

- What was tested
- What was not tested

## Notes

- Known issues or follow-up work
```

## Next recommended task

Implement the Blender Receiver MVP described above.
