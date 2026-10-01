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

## Module Ownership

Each module has an assigned owner, and contributors must work within their own module:

- Backend module code lives in `backend/src/main/java/com/infineonbit/sustainablefarm/modules/<module>/` (folder shorthand: `modules/<module>/`).
- Frontend module code lives in `frontend/src/features/<module>/` (folder shorthand: `features/<module>/`).

Rules:

1. **Only modify files inside your assigned module folder** (`modules/<module>/` on the backend, `features/<module>/` on the frontend). Do not touch another module's folder in a PR.
2. **Cross-module changes require prior coordination with that module's owner.** If your change genuinely needs to alter a module you do not own, coordinate with its owner first (for example, have them review or co-author the change).
3. Shared locations that are not owned by any single module — the backend `core/` package (`com.infineonbit.sustainablefarm.core`) and the frontend `frontend/src/shared/` folder — affect all modules and likewise require coordination before being changed.
