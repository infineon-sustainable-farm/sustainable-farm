# Sustainable Farm - Main Repository

**BIT × Infineon Excellence Program**  
**Repository Structure:** Multi-branch development strategy  
**Owner:** Abdoul Ben Fatao SANON  

---

## Branch Governance Strategy

This repository follows a structured branch governance strategy to isolate different workstreams and development activities.

### Branch Structure

```
main
 │
 ├── prod_trans_app (dedicated branch for Product Transformation application)
 │    ├── feat/backend-foundation
 │    ├── feat/database-schema
 │    ├── feat/api
 │    ├── feat/frontend
 │    └── feat/integration
 │
 └── autres branches du projet (other project branches)
```

### Branch Rules

**main →** Branche stable / référence  
**prod_trans_app →** Branche dédiée à l'application Product Transformation  
**feat/* →** Sous-branches temporaires pour les développements spécifiques

### Development Rule

**NE JAMAIS développer directement sur main.**  
Tous les développements doivent se faire dans des branches de fonctionnalités dédiées.

---

## Current Workstreams

### Product Transformation Application
- **Branch:** `prod_trans_app`
- **Status:** Implementation Phase (Weeks 5-8)
- **Technology Stack:** React + Spring Boot + PostgreSQL
- **Data Model:** Validated MERISE MCD (10.0/10 quality gate score)

---

## Getting Started

For Product Transformation application development, switch to the `prod_trans_app` branch:

```bash
git checkout prod_trans_app
```

---

## Repository Status

**Last Updated:** 2026-08-14  
**Branch Strategy:** Implemented  
**Main Branch:** Stable reference only