---
title: Setup
status: accepted
version: 2.0.0
owner: PANK4SS (admin)
last_updated: 2026-11-18
related: [ARCHITECTURE.md, CONTRIBUTING.md]
---

# Setup

How to run sustainable-farm locally and how to run its checks. The whole stack (PostgreSQL, Spring Boot backend, nginx frontend) runs with Docker Compose from `docker-compose.yml`; the tests run either with local tools or inside Docker.

## Related documents

| Document | Relationship |
|---|---|
| [Architecture](ARCHITECTURE.md) | What the containers and modules started here are |
| [Contributing](CONTRIBUTING.md) | Branches, commits and the gate that runs the checks below |

## 1. Prerequisites

| Tool | Required | Why |
|---|---|---|
| Docker with the Compose v2 plugin (`docker compose`) | Yes | Runs the stack; also runs the backend tests when no JDK is installed |
| Git | Yes | Clone and commit |
| JDK 21 | No | `scripts/backend-test.sh` uses `./mvnw` when `java` is on the `PATH`, otherwise Maven in the `maven:3.9-eclipse-temurin-21` image |
| Node.js 22 and npm | For frontend work only | Lint, tests, dev server (the CI uses Node 22; the image builds with `node:22-alpine`) |
| `arc-core`, `arc-design` | For committing | The Git hooks run the ARC Core gate (section 5) |

## 2. Configure

```bash
git clone git@github.com:infineon-sustainable-farm/sustainable-farm.git
cd sustainable-farm
cp .env.example .env
```

`.env` is never committed. The variables of `.env.example`:

| Variable | Default in `.env.example` | Role |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `sustainable_farm`, `farm_admin`, `changeme` | PostgreSQL database; the backend container builds `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from them |
| `POSTGRES_PORT` | `5432` | Host port of PostgreSQL |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/sustainable_farm`, ... | Only when the backend runs outside Docker |
| `BACKEND_PORT` | `8080` | Host port of the backend |
| `FRONTEND_PORT` | `3000` | Host port of the frontend |
| `SPRING_PROFILES_ACTIVE` | empty | Set `dev` to load the demo data set (`seed/demo-data.sql`: the Ferme de Banfora with fields A, B, C and a few rows for every module), once, on a database without a farm |
| `CORS_ALLOWED_ORIGINS` | ports 3000 and 5173 on `localhost` and `127.0.0.1` | Origins allowed to call the API |
| `WEATHER_LATITUDE`, `WEATHER_LONGITUDE` | `10.63`, `-4.77` | Site coordinates (Banfora) |
| `APP_MAIL_ENABLED`, `APP_MAIL_FROM` | commented out | Optional real e-mail, with `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` |
| `VITE_API_URL` | empty | Frontend build argument; empty means same origin, through the nginx proxy |

For a demo, set `SPRING_PROFILES_ACTIVE=dev`. The demo data is loaded at the first start only (when the `farm` table is empty) and its dates follow the current date. To start again from a clean demo, drop the database volume (`docker compose down -v`) and start again.

## 3. Start

```bash
docker compose up -d --build
docker compose ps
```

The backend waits for PostgreSQL to be healthy; the frontend waits for the backend health check. On start, Flyway applies the baseline migration and Hibernate validates the schema ([Architecture, database](ARCHITECTURE.md#26-database)).

| Service | URL (default ports) | Expected |
|---|---|---|
| Frontend | http://localhost:3000 | The launcher, one tile per module |
| Backend health | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Swagger UI | http://localhost:8080/swagger-ui.html | The endpoints of every module |
| OpenAPI JSON | http://localhost:8080/api-docs | The API description |
| PostgreSQL | `localhost:5432` | `docker exec sustainable-farm-db pg_isready -U farm_admin -d sustainable_farm` |

Replace 3000 and 8080 with `FRONTEND_PORT` and `BACKEND_PORT` when you changed them.

```bash
docker compose logs -f backend   # or postgres, frontend
docker compose down              # stop, keep the database volume
docker compose down -v           # stop and delete the database (needed after a model change)
```

### Frontend dev server

```bash
cd frontend
npm ci
npm run dev
```

Vite serves on http://localhost:5173 and proxies `/api` to http://localhost:8080 (`vite.config.js`), so the backend container must be running.

## 4. Run the checks

### Backend

```bash
scripts/backend-test.sh                    # whole suite on H2, default arguments -q -B test
scripts/backend-test.sh -q -B test -Dtest='*Farm*'
SF_TEST_PG=1 scripts/backend-test.sh       # same suite on a throwaway PostgreSQL 17 container
```

| Mode | Database | Use |
|---|---|---|
| Default | H2 in memory (`backend/src/test/resources/application-test.properties`) | Fast local runs |
| `SF_TEST_PG=1` | `postgres:17` container, removed at the end | Same database as the CI; a test that passes only on H2 is a bug |

Without a JDK the script runs Maven in Docker on a copy of `backend/` (the work tree stays clean) with the named volume `sf-m2` as Maven cache. The suite includes the ArchUnit test of the module boundaries.

### Frontend

```bash
cd frontend
npm ci
npm run lint          # ESLint (hard-coded text included) and the i18n parity check
npm run i18n:check    # i18n parity check only
npm test              # Vitest
npm run build         # production bundle
```

### Design and docs

```bash
scripts/design-check.sh                                   # no hard-coded visual value, brand icon set
arc-design check tokens --brand-dir .arc-core/design      # tokens and brand contract
arc-core check-docs                                       # a README, every relative link resolves
```

## 5. The ARC Core gate

Every commit goes through the ARC Core gate, installed as Git hooks (`arc-core status` says whether they are installed, `arc-core init` installs them). The gate runs the checks of `.arc-core/proof.json` that concern the staged files:

| Staged files | Checks |
|---|---|
| `backend/**` | `scripts/backend-test.sh` |
| `frontend/**` | `npm run lint`, `npm test` |
| `frontend/src/**`, `.arc-core/design/**` | `scripts/design-check.sh` |
| `.arc-core/design/**` | `arc-design check tokens` |
| `*.md` | `arc-core check-docs` |
| Any | gitleaks on the staged changes; osv-scanner when a manifest or lockfile changes |

Run it yourself before committing:

```bash
arc-core gate
```

Fix a refused commit; never bypass the hooks with `--no-verify`. The commit message rules are in [Contributing](CONTRIBUTING.md#4-commits).
