<p align="center">
  <img src="frontend/public/logo.webp" alt="Sustainable Farm logo" width="180" />
</p>

# sustainable-farm

Digital twin & AI-powered decision system for a sustainable dried mango supply chain from Burkina Faso to Germany, developed under the Infineon/BIT Excellence Program.

The system is built as a modular monolith: a Spring Boot backend with one Java package per business module, a React single-page application mirroring those modules as feature folders, and PostgreSQL for persistence — all orchestrated with Docker Compose.

## Tech Stack

Backend (from `backend/pom.xml`):

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=flat&logo=springboot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat&logo=apachemaven&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=flat&logo=postgresql&logoColor=white)
![SpringDoc OpenAPI](https://img.shields.io/badge/SpringDoc_OpenAPI-2.8.5-6BA639?style=flat&logo=openapiinitiative&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-annotated-D2000F?style=flat&logo=lombok&logoColor=white)

Frontend (from `frontend/package.json`):

![React](https://img.shields.io/badge/React-19-20232A?style=flat&logo=react&logoColor=61DAFB)
![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=flat&logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?style=flat&logo=tailwindcss&logoColor=white)
![Lucide React](https://img.shields.io/badge/Lucide_React-1-F96854?style=flat&logo=lucide&logoColor=white)
![Recharts](https://img.shields.io/badge/Recharts-3-444?style=flat)
![ESLint](https://img.shields.io/badge/ESLint-10-4B32C3?style=flat&logo=eslint&logoColor=white)

Infrastructure:

![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat&logo=nginx&logoColor=white)

## Documentation

- [Architecture](docs/ARCHITECTURE.md) — modular monolith structure, module layering, backend and frontend layout
- [Setup](docs/SETUP.md) — prerequisites, Docker Compose startup, service verification
- [Contributing](docs/CONTRIBUTING.md) — git workflow, branch naming, PR rules, module ownership
