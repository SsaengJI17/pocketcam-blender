# Project Log

This file is the shared handoff log between the project owner, ChatGPT, Codex, and any other coding assistant.

Use this file to record what was requested, what was changed, what still needs review, and what the next assistant should do.

## Rules

1. Always read this file before starting a task.
2. Add a new entry after each meaningful work session.
3. Do not delete previous entries.
4. Keep entries factual and concise.
5. Link related PRs, commits, issues, and files when possible.
6. If something was not tested, say so explicitly.
7. If a task was partially completed, list the remaining work.

## Entry template

```markdown
## YYYY-MM-DD HH:MM KST - Actor

### Request

What the user or previous assistant asked for.

### Work completed

- List actual changes made.
- Mention files changed.

### Verification

- List tests or manual checks performed.
- If not tested, write: Not tested.

### Known issues

- List known limitations, bugs, or risks.

### Next recommended action

- One clear next step.
```

---

## 2026-05-20 KST - ChatGPT

### Request

Initialize the repository so Codex can understand the project and start implementation work.

### Work completed

- Confirmed repository exists: `SsaengJI17/pocketcam-blender`.
- Added `README.md` with project goal, architecture, MVP order, protocol draft, and development principles.
- Added `ROADMAP.md` with staged release goals.
- Added `LICENSE` using MIT License.
- Added `docs/protocol.md` with protocol v1 draft.
- Added `docs/codex-instructions.md` with implementation guidance for Codex.
- Added this shared collaboration log: `PROJECT_LOG.md`.

### Verification

- Repository access was confirmed through the GitHub connector.
- File creation operations returned commit SHAs.
- No code has been implemented or tested yet.

### Known issues

- No actual Blender add-on code exists yet.
- No Android project exists yet.
- No Python test sender exists yet.
- Protocol and coordinate conversion are still drafts.

### Next recommended action

Ask Codex to implement the Version 0.1.0 Blender receiver MVP described in `docs/codex-instructions.md`.

---

## 2026-05-20 KST - Codex

### Request

Implement the focused Version 0.1.0 Blender Receiver MVP from `AGENTS.md`.

### Work completed

- Added Blender add-on package under `blender-addon/pocketcam_blender/`.
- Added UDP receiver start/stop support on port `8765`.
- Added UTF-8 JSON protocol v1 packet parsing for `rotation` in `[x, y, z, w]` order.
- Added shared state that stores only the latest valid pose.
- Added Blender timer-based camera rotation application on the main thread.
- Added 3D View sidebar UI with Start Server and Stop Server controls.
- Added `scripts/send_test_pose.py` UDP test sender.
- Added `protocol/pose-message.schema.json`.
- Added `blender-addon/README.md` and updated `README.md` with install and test instructions.

### Verification

- Ran Python syntax compilation for the add-on modules and test sender.
- Parsed `protocol/pose-message.schema.json` with `python -m json.tool`.
- Imported the protocol parser outside Blender and parsed a minimal valid pose packet.
- Ran a localhost UDP receiver smoke test and confirmed a valid packet updates latest pose state.
- Ran the test sender once against localhost UDP port `8765`.
- Blender UI/runtime behavior was not manually tested in Blender.

### Known issues

- Incoming quaternions are treated as Blender-space rotations only.
- Android/ARCore coordinate conversion is not implemented yet.
- No Android sender, ARCore support, binary protocol, smoothing, recording, or packaging is included.
- Manual Blender 4.x validation is still needed.

### Next recommended action

Open Blender 4.x, enable the add-on, start the server from the PocketCam sidebar, and verify camera rotation with `scripts/send_test_pose.py`.

---

## 2026-05-20 KST - Codex

### Request

Implement the Version 0.2.0 Android Rotation Sender MVP and push it to a new feature branch.

### Work completed

- Added a native Kotlin Android project under `android-app/`.
- Added Rotation Vector Sensor tracking and conversion from Android `[w, x, y, z]` quaternion output to protocol v1 `[x, y, z, w]`.
- Added UDP UTF-8 JSON pose packet sending with default target `127.0.0.1:8765`.
- Added editable host and port fields, Start Sending and Stop Sending buttons, and basic status readouts.
- Added `android-app/README.md` with Android Studio build, Blender receiver, USB debugging, ADB reverse, and Wi-Fi/LAN test instructions.
- Updated `README.md` with Android sender MVP notes.

### Verification

- Reviewed Kotlin and Android project files for focused MVP scope.
- Ran `git diff --check`.
- Parsed Android XML resources and manifest with PowerShell XML parsing.
- Confirmed local `gradle` and `kotlinc` commands are not available in this environment.
- Not built locally because Android Gradle dependencies/SDK sync were not available in this environment.
- Not tested on a physical Android device.

### Known issues

- The app sends UDP packets, while `adb reverse tcp:8765 tcp:8765` forwards TCP ports; Wi-Fi/LAN testing may be required until a TCP transport is added.
- ARCore, 6DoF position tracking, binary protocol, smoothing, recording, and release packaging are not implemented.
- Manual Android Studio sync, device install, sensor validation, and Blender end-to-end testing are still needed.

### Next recommended action

Open `android-app/` in Android Studio, run the app on a physical Android device, and verify packets against the Blender UDP receiver over Wi-Fi/LAN or a future TCP transport.

---

## 2026-05-20 18:21 KST - Codex

### Request

Fix GitHub issue #3 by replacing unresolved Android Kotlin `singleLine()` calls, verify `assembleDebug`, then commit and push a fix branch.

### Work completed

- Created branch `fix/android-singleline` from `main`.
- Replaced both `singleLine()` calls with `isSingleLine = true` in `android-app/app/src/main/java/io/github/ssaengji17/pocketcam/MainActivity.kt`.
- Confirmed GitHub issue #3 describes the Android Kotlin compile failure in `MainActivity.kt`.

### Verification

- Ran `.\gradlew.bat assembleDebug` from `android-app/`.
- Confirmed `BUILD SUCCESSFUL in 14s`.

### Known issues

- No known issues for this fix.
- Physical Android device runtime behavior was not tested.

### Next recommended action

- Open a pull request from `fix/android-singleline` into `main` and keep it unmerged until reviewed.

---

## 2026-05-20 19:04 KST - Codex

### Request

Implement Version 0.2.0 ARCore 6DoF MVP on branch `feature/arcore-6dof-mvp`, keep UDP transport, update Android and Blender support, verify builds, and do not merge into `main`.

### Work completed

- Added ARCore SDK support to the Android app with camera permission, optional ARCore manifest metadata, and AndroidX Gradle property support.
- Added Android mode selection between Rotation Sensor mode and ARCore 6DoF mode.
- Added ARCore session/frame tracking through a `GLSurfaceView` renderer and UDP packets containing `mode: "arcore"`, `position`, `rotation`, and `tracking`.
- Preserved existing Rotation Vector Sensor mode and UDP transport.
- Extended the Blender protocol parser/state/receiver to accept optional `position`.
- Applied incoming position to the active camera when present while keeping rotation-only packets working.
- Added Blender `Position Scale` and `Recenter` controls.
- Added a minimal ARCore-to-Blender position conversion layer with TODO notes for later calibration refinement.
- Updated `README.md`, `docs/protocol.md`, `android-app/README.md`, and `blender-addon/README.md`.

### Verification

- Ran `.\gradlew.bat assembleDebug` from `android-app/` and confirmed `BUILD SUCCESSFUL in 3s`.
- Ran Python syntax compilation for Blender add-on modules and `scripts/send_test_pose.py`.
- Ran a parser smoke test for an ARCore packet with `position`.
- Ran a localhost UDP receiver smoke test for an ARCore packet and confirmed latest pose stores rotation and position.

### Known issues

- Physical ARCore device runtime behavior was not tested.
- Manual Blender UI/runtime behavior was not tested in Blender.
- ARCore/Android rotation coordinate conversion remains minimal and needs real-device calibration.
- UDP remains the only transport, so `adb reverse tcp:8765 tcp:8765` still does not forward current packets.

### Next recommended action

- Open a pull request from `feature/arcore-6dof-mvp` into `main`, then test on an ARCore-supported Android device with Blender running.

---

## 2026-05-20 20:42 KST - Codex

### Request

Fix ARCore startup failure from logcat: `Failed to register sensor to queue 0`, likely caused by app-managed sensor listeners conflicting with ARCore sensor registration.

### Work completed

- Refactored `RotationSensorTracker` to create a dedicated `SensorEventListener` on start and unregister plus null it on stop.
- Added detailed Android logging before and after Rotation Vector Sensor registration/unregistration.
- Explicitly stop app-managed rotation sensor listeners before starting ARCore mode.
- Added a short delay before ARCore startup so custom sensor unregistration can settle before `Session.resume()`.
- Moved ARCore `Session.resume()` so it runs only after the `GLSurfaceView` surface has been created.
- Added ARCore startup failure reporting with the user-facing message: `ARCore failed to access device sensors.`
- Kept Rotation Sensor mode behavior intact.

### Verification

- Ran `.\gradlew.bat assembleDebug` from `android-app/`.
- Confirmed `BUILD SUCCESSFUL in 5s`.

### Known issues

- Physical ARCore device runtime validation is still required to confirm the sensor queue conflict is resolved.
- `defaultDisplay` deprecation warning remains during Android compile.

### Next recommended action

- Install the debug APK on the ARCore device, switch to ARCore 6DoF mode, and verify logcat no longer reports `Failed to register sensor to queue 0`.

---

## 2026-05-20 21:13 KST - Codex

### Request

Refactor ARCore mode after the hidden 1x1 `GLSurfaceView` still caused `Failed to register sensor to queue 0`; use a real visible AR preview and a more standard ARCore lifecycle.

### Work completed

- Replaced the hidden 1x1 ARCore `GLSurfaceView` with a visible preview area in the Android UI.
- Moved the ARCore preview near the top of the app below the mode selector with match-width layout and 320dp height.
- Refactored ARCore startup so `Session` is created/configured on the main thread.
- Changed startup ordering to call `GLSurfaceView.onResume()` first, then call `Session.resume()` on the main thread only after the preview surface reports a real size.
- Removed GL-thread resume attempts from `onDrawFrame`; the GL thread now handles texture attachment, display geometry, `session.update()`, and pose extraction.
- Added UI status for ARCore availability, surface readiness, session resumed state, and tracking state.
- Improved ARCore failure reporting so `Last error` includes the exception class/message and prefixes known sensor queue failures with `ARCore failed to access device sensors.`
- Updated `android-app/README.md` to document the visible AR preview and ARCore status lines.

### Verification

- Ran `.\gradlew.bat assembleDebug` from `android-app/`.
- Confirmed `BUILD SUCCESSFUL in 1s`.

### Known issues

- Physical ARCore device runtime validation is still required to confirm the visible preview and lifecycle refactor resolve the sensor queue failure.
- `defaultDisplay` deprecation warning remains during Android compile.

### Next recommended action

- Install the debug APK on the ARCore device, start ARCore 6DoF mode, and confirm the preview surface is larger than 1x1 and `Session.resume()` no longer fails with `Failed to register sensor to queue 0`.
