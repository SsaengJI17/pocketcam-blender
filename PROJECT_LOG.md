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
