# Architecture

## Overview

This project is a **modular monolith**: a single Spring Boot application deployed as one JAR in one container, backed by a single PostgreSQL database. There are no microservices and no inter-service network communication between business modules. Instead, each business module is a Java package under `com.infineonbit.sustainablefarm.modules`, running in the same JVM process.

The frontend is a separate single-page application (React) served by nginx in its own container and communicating with the backend over HTTP.

> **Current state note:** the repository is a scaffold. All 9 backend module packages and all 9 frontend feature folders contain only `.gitkeep` placeholders. No controllers, services, repositories, entities, or DTOs have been written yet. Everything below describes the structure as it exists in the repository.

## Repository Layout

```
sustainable-farm/
├── backend/               Spring Boot application (Maven, Java)
├── frontend/              React single-page application (Vite)
├── docs/                  Project documentation
├── docker-compose.yml     postgres + backend + frontend services
├── .env.example           Template for environment variables
└── README.md
```

## Backend

Package root: `com.infineonbit.sustainablefarm`

- `SustainableFarmApplication` — the `@SpringBootApplication` entry point at the package root.
- `modules/` — one Java package per business module (see below).
- `core/` — shared package (see below).

### The 9 Modules

Each module lives under `com.infineonbit.sustainablefarm.modules.<module-name>`:

| Module package | Purpose (inferred from folder name) |
|---|---|
| `cropstorage` | Crop / harvest storage |
| `energysupply` | Energy supply |
| `machinery` | Machinery |
| `plants` | Plants / crop cultivation |
| `producttransformation` | Product processing / transformation |
| `salesmarketing` | Sales and marketing |
| `sitesecurity` | Site security |
| `visitormanagement` | Visitor management |
| `watersupply` | Water supply |

> **Note:** the purposes above are inferred solely from the folder names. The modules contain no code or comments that document their business logic. Several of the names (plants, water, processing, storage, energy, security, machinery, sales and marketing) also appear in the project description in the root `README.md`; `visitormanagement` does not appear there.

### Layered Structure per Module

Every module package is pre-created with the same five sub-packages:

| Sub-package | Role |
|---|---|
| `controller` | REST API layer; handles HTTP requests and responses. The project uses `spring-boot-starter-webmvc`, and `springdoc-openapi` exposes the endpoints as OpenAPI documentation. |
| `service` | Business logic layer between controllers and repositories. |
| `repository` | Persistence layer; Spring Data JPA repositories. |
| `entity` | JPA entities mapped to database tables (Hibernate; `spring.jpa.hibernate.ddl-auto=update`). |
| `dto` | Data transfer objects carried across the API boundary, decoupling API contracts from entities. |

The intended dependency direction implied by these layer names is: `controller` -> `service` -> `repository` -> `entity`, with `dto` objects crossing the API boundary. Since no module code exists yet, this direction cannot be verified from the codebase and is stated only as the conventional reading of the layer names.

### The `core` Package

`com.infineonbit.sustainablefarm.core` sits next to `modules/`, outside any single module. By its placement, it is the designated home for code shared across modules (cross-cutting concerns), as opposed to module-specific code.

> **Note:** the `core` package is currently **completely empty** (no files at all), and nothing in the repository documents its intended contents. Its purpose stated above is inferred from its location in the package hierarchy only.

### Backend Technology Facts (from `pom.xml` and `application.properties`)

- Spring Boot `4.1.0` (parent POM), packaged with Maven.
- `java.version` is set to `21` in `pom.xml`, matching the Docker build and runtime images (`maven:3.9-eclipse-temurin-21`, `eclipse-temurin:21-jre-alpine`).
- Starters: `webmvc`, `data-jpa`, `validation`, `actuator`; plus `postgresql` driver (runtime), `springdoc-openapi-starter-webmvc-ui` 2.8.5, and Lombok.
- Datasource is configured from environment variables: `DB_HOST`, `DB_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.
- JPA schema management: `spring.jpa.hibernate.ddl-auto=update`.
- One test exists (`SustainableFarmApplicationTests`) covering only application context startup.

## Frontend

React single-page application built with Vite; Tailwind CSS for styling.

```
frontend/src/
├── main.jsx                 Application entry point
├── App.jsx                  Root component
├── index.css                Tailwind theme definition
├── features/<module>/       One folder per backend module
│   ├── api/
│   ├── components/
│   └── hooks/
└── shared/                  Cross-feature code
    ├── api/
    ├── components/
    ├── hooks/
    └── utils/
```

### `src/features/<module>`

Nine feature folders exist, with names exactly matching the nine backend module packages: `cropstorage`, `energysupply`, `machinery`, `plants`, `producttransformation`, `salesmarketing`, `sitesecurity`, `visitormanagement`, `watersupply`. The frontend feature boundaries therefore mirror the backend module boundaries one-to-one.

Each feature folder contains three sub-folders:

- `api/` — code for calling the backend endpoints of that module.
- `components/` — UI components of that module.
- `hooks/` — React hooks of that module.

All feature folders currently contain only `.gitkeep` placeholders.

### `src/shared`

Cross-feature code used by multiple features: `api/`, `components/`, `hooks/`, `utils/`. It is the frontend counterpart of the backend's cross-module shared area. All sub-folders currently contain only `.gitkeep` placeholders.

### Current Application Shell

`App.jsx` currently renders the project logo (`/logo.webp`), the heading "Sustainable Farm Platform", and a placeholder message — no feature UI exists yet.

### Styling

The Tailwind theme in `index.css` defines sans-serif fonts only — Inter (body) and Montserrat (headings) — plus semantic colors (`primary`, `success`, `warning`, `error`, `info`).

### Icon and Chart Libraries

Two additional frontend dependencies are available to every feature folder: `lucide-react` (icon library) and `recharts` (charts / data visualization). They are installed at the project level rather than per feature, so they are a shared UI capability, not tied to any single module.

### Production Build

The frontend Dockerfile builds the Vite bundle with `node:22-alpine` (`npm run build` producing `dist/`) and serves it with `nginx:alpine` on port 80.

## Runtime Topology

```
Browser
  └── frontend container (nginx, published on host port 3000)
        └── backend container (published on host port 8080)
              └── postgres container (PostgreSQL 17, published on host port ${POSTGRES_PORT})
```

The backend container starts only after the PostgreSQL container passes its health check (`pg_isready`); the frontend container starts after the backend.
