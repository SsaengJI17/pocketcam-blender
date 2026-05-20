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
