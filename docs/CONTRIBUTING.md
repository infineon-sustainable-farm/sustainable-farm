---
title: Contributing
status: accepted
version: 2.0.0
owner: PANK4SS (admin)
last_updated: 2026-11-18
related: [ARCHITECTURE.md, SETUP.md, CI-AUTO-REVIEW.md]
---

# Contributing

The rules for changing sustainable-farm: branches and pull requests, who may change which folder, commit messages and the checks every commit and PR goes through.

## Related documents

| Document | Relationship |
|---|---|
| [Architecture](ARCHITECTURE.md) | The module boundaries that the ownership rules mirror |
| [Setup](SETUP.md) | How to run the checks locally |
| [CI auto-review](CI-AUTO-REVIEW.md) | The PR pipeline and its required checks |
| [ADR index](adr/README.md) | Conventions a change must follow |

## 1. Branches and pull requests

| Branch | Rule |
|---|---|
| `main` | Protected; receives finished work only |
| `develop` | Protected, no force-push, no deletion; target of every pull request |
| `feature/<module>/init` | The branch assigned to each module owner, created from `develop` |

| Rule | Detail |
|---|---|
| Branch policy | `.github/workflows/branch-policy.yml` closes a PR to `develop` or `main` whose branch does not end in `/init`, unless the admin (PANK4SS) opened it or it carries the `policy-exception` label |
| Approval | 1 approval required; self-approval does not count; new commits dismiss earlier approvals |
| Scope | One focused change per PR; backend and frontend of the same module change together when an API changes |
| Required checks | "Backend tests (PostgreSQL)", "Frontend build", "Quality gate (auto-review)", "Module ownership" ([CI auto-review](CI-AUTO-REVIEW.md)) |

## 2. Module ownership

Owners are listed in `.github/module-owners.conf`. A module owner changes only the folders of their module; the admin changes anything, including the shared folders.

| Folder | Owner |
|---|---|
| `backend/.../modules/<module>/`, `backend/src/test/java/.../modules/<module>/` | The module owner |
| `backend/src/main/resources/<module>/`, `backend/src/test/resources/<module>/` | The module owner |
| `backend/src/main/resources/seed/<module>-*` | The module owner (demo data, `dev` profile only) |
| `frontend/src/features/<module>/` | The module owner |
| New migration `backend/src/main/resources/db/migration/V<n>__<module>_<description>.sql` | The module owner; existing migrations, the baseline included, are never edited |
| `backend/.../core/` | Admin |
| `backend/.../modules/farm/`, `modules/staff/`, `modules/notification/` (shared referentials) | Admin (listed with owner PANK4SS) |
| `frontend/src/app/` (shell, router, launcher) | Admin |
| `frontend/src/shared/` (`ui`, `api`, `i18n`, `hooks`, `utils`) and `frontend/src/index.css` | Admin |
| `frontend/src/locales/<language>/common.json` | Admin |
| `frontend/src/locales/<language>/<module>.json` | Changed with the module's screens; outside `features/<module>/`, so through the admin for a non-admin author |
| `docs/`, `scripts/`, `.github/`, `.arc-core/`, build and Docker files, root files | Admin |

| Owner | Modules |
|---|---|
| PANK4SS (admin) | `machinery`, `farm`, `staff`, `notification` |
| wangredelwende-spec | `plants` |
| SABF226 | `producttransformation` |
| nishe956 | `salesmarketing` |
| ALIX6S | `visitormanagement` |
| brecheyn | `watersupply` |
| Dark1920 | `energysupply` |
| TaniaW777 | `sitesecurity` (excluded module) |
| christelle-dehoumon | `cropstorage` (excluded module) |

Rules:

1. Change only the folders of your module. A change needed in another module is made by its owner.
2. Another module is used only through its `modules.<x>.api` package or its events; a shared concept (farm, block, zone, person, alert, equipment, variety) is referenced by foreign key to its owner, never copied ([ADR 0003](adr/0003-communication-entre-modules.md), [ADR 0004](adr/0004-referentiels-partages.md)). A new method in a shared referential's `api` is asked from the admin.
3. Follow the database and API conventions of [ADR 0002](adr/0002-conventions-base-de-donnees-et-api.md): `BaseEntity`, enums as strings, `Money`, `/api/<module-kebab>/...`, record DTOs, the exceptions of `core.exception`, no module `@RestControllerAdvice`.
4. Screens use the shell (`nav` in `routes.jsx`), the kit of `shared/ui` and the tokens, and every text goes through i18n in English, German and French ([ADR 0001](adr/0001-app-shell-et-design-system.md)).
5. `docs/` is the general documentation only: the files listed as `doc` in `module-owners.conf`, no per-module document. Module documentation lives in the module folder (for example `modules/watersupply/README.md`).
6. The repository layout is fixed: only the entries listed as `root` in `module-owners.conf` may exist at the root, for everyone, admins included.

### Enforcement

`.github/workflows/module-ownership.yml` runs `scripts/module-ownership-guard.sh` on every PR, with the script and the owners file of the base branch. It checks the layout (`root` and `doc` lists) for every author and, for a non-admin author, that the PR comes from `feature/<module>/init` of an owned module and changes only that module's folders. Run it locally before opening a PR:

```bash
scripts/module-ownership-guard.sh <github-login> feature/<module>/init HEAD origin/develop
```

## 3. Before a commit

Run the checks of your change ([Setup](SETUP.md#4-run-the-checks)), then the gate:

```bash
arc-core gate
```

The Git hooks run the same gate on every commit. A refused commit is fixed, never forced: do not use `--no-verify`. The checks are declared in `.arc-core/proof.json` and `.arc-core/config.json`; both are committed.

## 4. Commits

Header: `<type>(<scope>): <subject>`, in lower case.

| Part | Rule |
|---|---|
| `type` | One of `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `build`, `ci`, `chore`, `style`, `revert` |
| `scope` | The module (`plants`, `watersupply`, ...) or `core`, `app`, `ui`, `i18n` |
| `subject` | What the commit does, imperative, no final period |
| Attribution | No AI attribution line of any kind (refused by `arc-core commit-msg`) |

Examples: `fix(api): keep the sort order after an edit`, `docs(adr): update the alert model`.
