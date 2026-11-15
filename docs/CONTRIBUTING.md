# Contributing

## Git Workflow

The repository uses a three-level branch model:

```
main (protected)
  └── develop (protected; force-push and deletion blocked)
        └── feature/<module-name>/<short-description>
```

- **`main`** — protected branch. Receives finished work only. Do not push to it directly.
- **`develop`** — protected branch. Force-push and deletion are blocked. This is the integration branch; all pull requests target `develop`.
- **Feature branches** — created from `develop` for every piece of work.

### Branch Naming Convention

```
feature/<module-name>/<short-description>
```

- `<module-name>` must match one of the nine module folders exactly:
  `cropstorage`, `energysupply`, `machinery`, `plants`, `producttransformation`, `salesmarketing`, `sitesecurity`, `visitormanagement`, `watersupply`
- `<short-description>` is a short kebab-case summary of the change.

Example branch name: `feature/plants/add-plant-endpoints`

## Pull Requests

- Pull requests are opened **against `develop`**.
- **1 approval is required** before merging.
- **Self-approval does not count** — the author cannot satisfy the approval requirement with their own review.
- **Stale approvals are dismissed** when new commits are pushed to the PR: the required approval must be given again after the latest changes.

### PR Scope and Sequencing

- The first PR in a schema/data change sequence must be limited to the schema and baseline data. Preserve V1–V3 unchanged; do not include business-behavior changes in this PR.
- Follow-up PRs may introduce business behavior, but must separate it feature by feature so each PR has a focused, reviewable scope.

## Module Ownership

Each module has an assigned owner, and contributors must work within their own module:

- Backend module code lives in `backend/src/main/java/com/infineonbit/sustainablefarm/modules/<module>/` (folder shorthand: `modules/<module>/`).
- Frontend module code lives in `frontend/src/features/<module>/` (folder shorthand: `features/<module>/`).

Rules:

1. **Only modify files inside your assigned module folder** (`modules/<module>/` on the backend, `features/<module>/` on the frontend). Do not touch another module's folder in a PR.
2. **Cross-module changes are made by the module's owner.** If your work needs a change in a module you do not own, ask its owner to make it from their own `/init` branch.
3. **Shared locations and the repository layout are reserved to the project admin.** This covers the backend `core/` package, the frontend `frontend/src/shared/` folder, build and Docker files, `docs/`, `scripts/`, `.github/`, and any new folder at the root of the repository. Ask the admin for these changes.

4. **`docs/` is the general project documentation, not a per-module documentation.** It only holds the documents listed as `doc` in `.github/module-owners.conf`, with no sub-folder. Module documentation (integration contracts, audits, notes) lives in the module folder, for example `modules/<module>/README.md`.
5. **The repository layout is fixed.** Only the entries listed as `root` in `.github/module-owners.conf` may exist at the root of the repository. This applies to everyone, admins included: a new root entry or general document is first added to that list.

### Enforcement

The `Module ownership` check (`.github/workflows/module-ownership.yml`, running `scripts/module-ownership-guard.sh`) blocks any PR that breaks these rules. Owners are listed in `.github/module-owners.conf`. For a non-admin author, the PR must come from `feature/<module>/init` for a module they own, and every changed path must be one of:

| Allowed path | Notes |
|---|---|
| `backend/src/main/java/.../modules/<module>/` | Module backend code |
| `backend/src/test/java/.../modules/<module>/` | Module backend tests |
| `backend/src/main/resources/<module>/`, `backend/src/test/resources/<module>/` | Module resources |
| `backend/src/main/resources/seed/<module>-*` | Module seed data (loaded under the `dev` profile only) |
| `backend/src/main/resources/db/migration/V<n>__<module>_<description>.sql` | New migrations only; existing migrations are never edited |
| `frontend/src/features/<module>/` | Module frontend code |

Run the check locally before opening a PR:

```bash
scripts/module-ownership-guard.sh <your-github-login> feature/<module>/init
```
