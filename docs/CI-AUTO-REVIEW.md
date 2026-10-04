# Smart CI: auto-review + auto-merge of `*/init` PRs

Goal: no more manual branch-by-branch reviews. The CI reviews every PR to
`develop`, publishes a report, and merges whitelisted PRs by itself.

## Pipeline

```
PR (opened/push/reopen) targeting develop
│
├── "CI" workflow                            (required checks)
│   ├── Backend tests (PostgreSQL)   <- tests on REAL Postgres (no more silent H2)
│   ├── Flyway migrations (PostgreSQL) <- migrations applied to a fresh database
│   └── Frontend build
│
└── "Auto-review" workflow
    └── Quality gate (auto-review)    (required check, merge verdict)
        ├── scripts/audit-shared-files.sh --cross-check
        │     real merge conflicts, diverged/deleted shared files,
        │     module-local endpoints convention, cross-conflicts between branches
        ├── scripts/ci-guards.sh  (on the DIFF: existing debt never blocks)
        └── Report published as a PR comment (updated on every push)
            │
            ├── [RED] blocking violation -> red check, the report says WHAT to fix
            └── [GREEN] or [YELLOW] passed
                └── auto-merge (job): if PR created by PANK4SS AND branch */init
                    -> gh pr merge --squash --auto --delete-branch
                    -> merges as soon as ALL required checks are green
```

Note: the project does not use Flyway yet. When migrations are introduced,
add a CI job that applies them to a fresh PostgreSQL database and add it to
the required checks above.

## Blocking guards (`scripts/ci-guards.sh`)

| ID | Pattern | Review reference |
|----|---------|------------------|
| F1 | `@CrossOrigin` added | PR-26 |
| F2 | Hardcoded URL/IP (`localhost`, `10.x`, `192.168.x`) in code | PR-26, PR-42 |
| F3 | Password hash/secret inside a Flyway migration | PR-33 |
| F4 | Demo accounts/data in a migration | PR-33 |
| F5 | Starter removed or `java.version` downgraded in `pom.xml` | PR-29, PR-42 |
| F6 | JPA entity bound to `@RequestBody` (mass assignment) | PR-26, PR-27, PR-43 |
| F7 | `@ManyToOne` without `LAZY` | PR-43 |
| F8 | `CommandLineRunner` without `@Profile` | PR-26 |
| F9 | Second `@RestControllerAdvice` without `@Order` | PR-42 |
| F10 | `@Id` without `@GeneratedValue` (client-controlled PK) | PR-43 |
| F11 | Hardcoded literal secret | PR-33 |

Warnings (yellow, non-blocking): unbounded `findAll()` (W1), entity without
`@Version` (W2), raw exception -> 500 (W3), `fetch()` outside the API layer (W4).

## One-time GitHub setup

1. **Settings -> General -> Pull Requests**: enable **Allow auto-merge** and
   automatically delete head branches (the workflow also passes
   `--delete-branch`).
2. **Settings -> Branches -> Branch protection rule** on `develop`:
   - Require a pull request before merging
   - Require status checks to pass:
     - `Quality gate (auto-review)`
     - `Backend tests (PostgreSQL)`
     - `Frontend build`
   - Require branches to be up to date before merging
   - Do **not** require approvals for `*/init` PRs (GitHub forbids PR authors
     from approving their own PRs - the "reviewer" role is played by the
     `Quality gate` check).
3. Changing the allowed identity/suffix: update `AUTO_REVIEW_LOGIN` and the
   `if` of the `auto-merge` job in `.github/workflows/auto-review.yml` (two
   places, since the `env` context is not available in a job-level `if`).

## Accepted limits (reduced human review, not zero)

The PR report permanently carries the non-automatable points: business
semantics (invariants, real vs planned volumes, QC gates), test relevance,
ingestion idempotency, PUT semantics. The CI is a filter on known defect
patterns, not a functional reviewer.

## Extending the guards

Add the pattern in `scripts/ci-guards.sh` (`fail`/`warn` functions), document
it in the table above. Guards only see the `merge-base..head` diff: a defect
pre-existing on the base is never reported.
