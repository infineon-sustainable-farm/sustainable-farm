# Database Migrations & Multi-Module Coordination

## One physical database, many modules

All business modules run in the same JVM (see `docs/ARCHITECTURE.md`) and talk to
the **same PostgreSQL database** `sustainable_farm` (`POSTGRES_DB`). Every module is
a full copy of the same backend, so **after merge there is only ONE application**:
one `application.properties`, one `db/migration` folder, and one Flyway history
table.

This branch keeps the original configuration untouched (`ddl-auto=update`, see
`backend/src/main/resources/application.properties`): Hibernate still reconciles the
schema as before, and **Flyway is added on top as a purely additive layer**.
Migration files live in `backend/src/main/resources/db/migration/`.

## Reserved version ranges (the decisive rule)

Flyway compares **version numbers**, not file names: `V1__baseline.sql` and
`V1__init.sql` are both version `1` and collide. At startup on the same history
table, Flyway rejects such a duplicate:

> Found more than one migration with version 1

To guarantee that visiting/other modules never collide after merge, every module
reserves a range of version numbers (block of 100) and only publishes migrations
inside it:

| Module | Reserved range | Status |
|---|---|---|
| `visitormanagement` (this branch) | **V100 – V199** (`V100__baseline` … `V103__staff`) | reserved high |
| `watersupply` | V1 – V9 (currently `V1__init`) | untouched, keeps its numbers |
| `cropstorage`, `energysupply`, `machinery`, ... | V200+, V300+, ... (to be assigned) | negotiate on first migration |

Rules:

1. **Never reuse a version number** already used by another module.
2. **Each module starts inside its own block.** Visitormanagement deliberately chose
   a **high range (V100+)** so that even if other modules grow (e.g. water planned up
   to V9, maybe more), there is no overlap today and plenty of headroom later.
3. **If another module's numbering approaches the start of a reserved block**
   (e.g. reaches V99 while visitormanagement starts at V100), the owner of that block
   raises its range (e.g. to V200+) and **this document is updated accordingly**.
4. Renumbering migrations that are **already applied** requires cleaning the history
   table first (see "When a local history entry drifts").
5. Keep migration files under unique names in the shared `db/migration` folder, and
   keep DDL **idempotent** (`CREATE TABLE IF NOT EXISTS`, guarded
   `ALTER ... ADD COLUMN IF NOT EXISTS`).

> Note: since this branch ships `V100–V103`, its files were renamed in place from
> the original `V1–V4` in order to clear the V1 range for the legacy modules.

## Adopting the pre-existing (legacy) schema

The database existed before Flyway was versioned. `baseline-on-migrate=true` with
`baseline-version=0` starts Flyway from version 0, and `V100__baseline.sql` uses
`IF NOT EXISTS` so the existing database is adopted safely — nothing is dropped.

## When a local history entry drifts

If a migration file is edited (or renamed) *after* it was applied locally, Flyway
refuses to start. Delete the offending entries, then re-run the app so the
migration re-applies:

```
docker exec sustainable-farm-db psql -U farm_admin -d sustainable_farm \
  -c "DELETE FROM flyway_schema_history WHERE version IN ('1','2','3','4');"
```

## Test scope

Unit/integration tests run against in-memory H2
(`application-test.properties`): Flyway is disabled and Hibernate uses
`create-drop`. The real migrations are verified manually against the dev
PostgreSQL (`docker compose up -d backend`, check startup logs). A
Testcontainers-based test exercising Postgres-specific migrations and locking is a
possible follow-up.

## Required dependencies

Flyway + PostgreSQL in this Spring Boot 4 / Spring Framework 7 project:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```