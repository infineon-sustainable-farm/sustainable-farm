<p align="center">
  <img src="frontend/public/logo.webp" alt="Sustainable Farm logo" width="180" />
</p>

# sustainable-farm

Digital twin and decision system for a sustainable dried mango supply chain from Burkina Faso to Germany, developed under the Infineon/BIT Excellence Program.

One web application with one module per domain: machinery, plants, product transformation, sales and marketing, visitor management, water supply and energy supply, on shared referentials for farms, people and alerts. The backend is a modular monolith (Spring Boot, one PostgreSQL database); the frontend is a React single-page application with one shell, one component kit and three languages (English, German, French).

## Tech stack

Backend (`backend/pom.xml`):

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=flat&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat&logo=apachemaven&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=flat&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-baseline-CC0200?style=flat&logo=flyway&logoColor=white)
![springdoc-openapi](https://img.shields.io/badge/springdoc--openapi-3.1.1-6BA639?style=flat&logo=openapiinitiative&logoColor=white)
![ArchUnit](https://img.shields.io/badge/ArchUnit-1.5.1-444444?style=flat)

Frontend (`frontend/package.json`):

![React](https://img.shields.io/badge/React-19-20232A?style=flat&logo=react&logoColor=61DAFB)
![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=flat&logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?style=flat&logo=tailwindcss&logoColor=white)
![React Router](https://img.shields.io/badge/React_Router-7-CA4245?style=flat&logo=reactrouter&logoColor=white)
![TanStack Query](https://img.shields.io/badge/TanStack_Query-5-FF4154?style=flat&logo=reactquery&logoColor=white)
![i18next](https://img.shields.io/badge/react--i18next-18-26A69A?style=flat&logo=i18next&logoColor=white)
![Lucide](https://img.shields.io/badge/Lucide_React-1-F56565?style=flat&logo=lucide&logoColor=white)
![Vitest](https://img.shields.io/badge/Vitest-5-6E9F18?style=flat&logo=vitest&logoColor=white)

Infrastructure:

![Docker Compose](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat&logo=nginx&logoColor=white)
![Node](https://img.shields.io/badge/Node-22-339933?style=flat&logo=nodedotjs&logoColor=white)

## Quick start

```bash
cp .env.example .env    # set SPRING_PROFILES_ACTIVE=dev for demo data
docker compose up -d --build
```

Then open http://localhost:3000. Details, ports and tests: [Setup](docs/SETUP.md).

## Documentation

| Document | Content |
|---|---|
| [Docs index](docs/README.md) | Every document and the reading order |
| [Architecture](docs/ARCHITECTURE.md) | Modules, shared referentials, module rules, database, API, frontend shell |
| [Setup](docs/SETUP.md) | Prerequisites, configuration, start, tests, the ARC Core gate |
| [Contributing](docs/CONTRIBUTING.md) | Branches, module ownership, commits |
| [Decision records](docs/adr/README.md) | Why the architecture is what it is |
