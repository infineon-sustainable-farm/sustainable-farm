# Sustainable Farm - Product Transformation Application

**BIT × Infineon Excellence Program**  
**Project:** Sustainable Farm - Product Transformation System  
**Architecture:** Monolithic Application  
**Current Branch:** feature/producttransformation/init  
**Technology Stack:** React + Spring Boot + PostgreSQL  
**Owner:** Abdoul Ben Fatao SANON  
**Status:** Implementation Phase (Weeks 5-8)

---

## Project Overview

This application implements the Product Transformation system for sustainable dried mango production in Burkina Faso. It provides end-to-end batch traceability from harvest to export, integrating with the Plants workstream for harvest data and supporting HACCP compliance for EU market requirements.

**Architecture Note:** This is a monolithic application with a single Spring Boot backend, PostgreSQL database, and React frontend. The architecture has been simplified from a microservices approach to reduce deployment complexity and infrastructure costs.

---

## Technology Stack

|| Layer | Technology | Version |
||-------|-----------|---------|
|| Frontend | React | 18.x |
|| Frontend Language | JavaScript | ES6+ |
|| Backend | Spring Boot | 3.x |
|| Backend Language | Java | 17+ |
|| Database | PostgreSQL | 15+ |
|| Build Tool | Maven | 3.9+ |
|| Version Control | Git + GitHub | - |

---

## Project Structure

```
sustainable-farm/
├── frontend/                    # React application
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── utils/
│   │   └── App.js
│   ├── package.json
│   └── Dockerfile
├── backend/                     # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/sustainablefarm/
│   │   │   │       ├── controller/
│   │   │   │       ├── service/
│   │   │   │       ├── repository/
│   │   │   │       ├── model/
│   │   │   │       ├── dto/
│   │   │   │       ├── exception/
│   │   │   │       └── config/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   └── test/
│   ├── pom.xml
│   └── Dockerfile
├── database/                    # Database scripts and migrations
│   └── schema.sql
├── docs/                        # Project documentation
├── .gitignore
├── README.md
├── .env.example
└── docker-compose.yml
```

---

## Data Model

The application implements the validated MERISE MCD from Week 5 Phase 3, consisting of:

### Core Processing Entities (6)
|- **BATCH** - Central traceability entity
|- **WASH_SORT_RECORD** - Washing and sorting stage data
|- **DRYING_RUN** - Drying process data (core transformation)
|- **PACKAGING_RECORD** - Packaging stage data
|- **QC_CHECKPOINT** - Quality control checkpoint data
|- **COMPLIANCE_RECORD** - HACCP compliance data

### Supporting Entities (3)
|- **RAW_INTAKE** - Raw material intake from Plants
|- **EQUIPMENT** - Machinery and equipment data
|- **OPERATOR** - Personnel data

### Integration Entities (2)
|- **HARVEST_EVENT** - Harvest data from Plants workstream
|- **HISTORICAL_HARVEST** - Aggregated historical harvest data

---

## Business Requirements

The system implements the following functional requirements (FR-01 through FR-09):

|- **FR-01:** Raw material intake tracking
|- **FR-02:** Batch creation and traceability
|- **FR-03:** Process stage recording
|- **FR-04:** Resource consumption tracking
|- **FR-05:** Quality control checkpoints
|- **FR-06:** Storage integration
|- **FR-07:** Packaging and lot coding
|- **FR-08:** Export readiness documentation
|- **FR-09:** Historical data analysis

---

## Branch Governance

This application follows a structured branch governance strategy for monolithic development:

```
main (stable reference)
 │
 └── feature/producttransformation/init (current initialization branch)
      ├── feature/producttransformation/backend-validation
      ├── feature/producttransformation/frontend-integration
      ├── feature/producttransformation/api-completion
      └── feature/producttransformation/testing
```

**Rule:** Never develop directly on main. All development happens in feature branches under `feature/producttransformation/*`.

---

## Getting Started

### Prerequisites

|- Java 17+
|- Node.js 18+
|- PostgreSQL 15+
|- Maven 3.9+
|- Docker (optional, for containerized deployment)

### Backend Setup

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will run on `http://localhost:8080`

### Frontend Setup

```bash
cd frontend
npm install
npm start
```

The frontend will run on `http://localhost:3000`

### Database Setup

```bash
# Create database
createdb sustainable_farm

# Run schema
psql sustainable_farm < database/schema.sql
```

### Docker Setup (Optional)

```bash
# Start all services
docker-compose up --build

# Stop services
docker-compose down
```

---

## API Documentation

API documentation will be available at `http://localhost:8080/swagger-ui.html` once the backend is running.

---

## Development Workflow

1. **Feature Branch:** Create a feature branch from `feature/producttransformation/init`
2. **Development:** Implement changes following the validated data model
3. **Testing:** Run unit and integration tests
4. **Commit:** Commit with conventional commit messages
5. **Local Validation:** Test locally before considering any remote operations
6. **Merge:** Merge to main after approval (local development only for now)

---

## Validation Principles

**Critical Rule:** Do not invent new business requirements while coding.

The Week 3 and Week 4 validated artifacts are the functional baseline. If an implementation decision conflicts with those documents, flag the conflict before modifying the architecture.

---

## Architecture Notes

**Monolithic Design Decisions:**
- Single Spring Boot application containing all product transformation features
- Single PostgreSQL database with all related tables
- Simplified deployment and maintenance
- Reduced infrastructure costs
- Easier team collaboration for the current phase

**Future Considerations:**
- If the application scales significantly, certain modules could be extracted into separate services
- Current monolithic approach supports the development timeline and team structure
- Database schema is designed to support future modularization if needed

---

## Status

**Current Phase:** Implementation (Weeks 5-8)  
**Last Updated:** 2026-08-19  
**Data Model Validation:** 10.0/10 (Week 5 Phase 3)  
**UML Consistency:** 10.0/10 (Week 5 Phase 3)  
**Architecture:** Monolithic (simplified from microservices approach)  
**Backend Foundation:** Complete (11 entities, services, controllers, DTOs)  
**Build Status:** ✅ Compiling successfully

---

## License

Proprietary - BIT × Infineon Excellence Program