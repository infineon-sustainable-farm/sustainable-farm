# Setup

The full stack runs locally through Docker Compose: PostgreSQL, the Spring Boot backend, the nginx-served frontend, and Mailpit, a test mail server. All four services are defined in `docker-compose.yml` at the repository root.

## Prerequisites

- **Docker**
- **Docker Compose** (v2 plugin, invoked as `docker compose`)
- **Git** (to clone the repository)

A local JDK and Node.js are **not** required: both the backend (Maven/Temurin 21) and the frontend (Node 22) are built inside their Docker images.

## Steps

1. Clone the repository and enter it:

   ```bash
   git clone git@github.com:infineon-sustainable-farm/sustainable-farm.git
   cd sustainable-farm
   ```

   (SSH URL of the `origin` remote; use the HTTPS form `https://github.com/infineon-sustainable-farm/sustainable-farm.git` if you prefer.)

2. Create your environment file from the template:

   ```bash
   cp .env.example .env
   ```

   Then edit `.env` as needed. The variables (with template defaults) are:

   | Variable | Default in `.env.example` | Used by |
   |---|---|---|
   | `POSTGRES_DB` | `sustainable_farm` | PostgreSQL database name |
   | `POSTGRES_USER` | `farm_admin` | PostgreSQL user |
   | `POSTGRES_PASSWORD` | `changeme` | PostgreSQL password (change this) |
   | `POSTGRES_PORT` | `5432` | Host port published for PostgreSQL |
   | `SITESECURITY_BOOTSTRAP_ADMIN_EMAIL` | empty | Email of the first Site Security admin, created at startup if it does not exist |

   Email settings are optional: by default, Docker Compose sends every email to Mailpit. To use a real SMTP relay, set the `MAIL_*` variables described in `.env.example`.

3. Build and start all services:

   ```bash
   docker compose up -d --build
   ```

   This builds the backend and frontend images and pulls `postgres:17-alpine` and `axllent/mailpit`. The backend waits for the PostgreSQL health check (`pg_isready`) to pass and for Mailpit to start; the frontend starts after the backend.

## Verifying the Services

Check overall status first:

```bash
docker compose ps
```

All four containers should be `running` (or `Up`), and `sustainable-farm-db` should report `healthy`.

| Service | Container name | URL / port | How to verify |
|---|---|---|---|
| Frontend | `sustainable-farm-frontend` | http://localhost:3000 | Page loads showing the project logo and the heading "Sustainable Farm Platform" |
| Backend health | `sustainable-farm-backend` | http://localhost:8080/actuator/health | Returns JSON with `"status":"UP"` (Spring Boot Actuator default; no exposure overrides exist in `application.properties`) |
| Swagger UI | `sustainable-farm-backend` | http://localhost:8080/swagger-ui/index.html | Swagger UI loads (springdoc default path; `/swagger-ui.html` also redirects there). Note: no API controllers exist yet, so the UI will be empty |
| PostgreSQL | `sustainable-farm-db` | `localhost:${POSTGRES_PORT}` (5432 by default) | Container reports `healthy`; or run manually: `docker exec sustainable-farm-db pg_isready -U farm_admin -d sustainable_farm` |
| Mailpit | `sustainable-farm-mailpit` | http://localhost:8025 | The inbox loads and shows the emails sent by the backend. It delivers nothing to real addresses, and stopping the container clears the inbox |

To inspect logs of a service:

```bash
docker compose logs -f backend    # or: postgres, frontend, mailpit
```

## Stopping and Resetting

```bash
docker compose down      # Stop and remove containers; keeps the postgres_data volume
docker compose down -v   # Also deletes the postgres_data volume (all database data is lost)
```
