---
title: Documentation index
status: accepted
version: 1.0.0
owner: PANK4SS (admin)
last_updated: 2026-11-18
related: [ARCHITECTURE.md, SETUP.md, CONTRIBUTING.md, CI-AUTO-REVIEW.md, adr/README.md]
---

# Documentation

The general documentation of sustainable-farm. Module documentation lives in the module folders (for example `backend/.../modules/watersupply/README.md`, `frontend/src/features/salesmarketing/README.md`, `frontend/src/shared/ui/README.md`).

| Order | Document | Answers |
|---|---|---|
| 1 | [Architecture](ARCHITECTURE.md) | How the system is built and which rules keep the modules apart |
| 2 | [Setup](SETUP.md) | How to run the stack and its checks |
| 3 | [Contributing](CONTRIBUTING.md) | Who may change what, how to commit |
| 4 | [Decision records](adr/README.md) | Why each structural decision was taken |
| 5 | [CI auto-review](CI-AUTO-REVIEW.md) | How pull requests are reviewed and merged |

A new document in `docs/` must first be added to the `doc` list of `.github/module-owners.conf` ([Contributing](CONTRIBUTING.md#2-module-ownership)).
