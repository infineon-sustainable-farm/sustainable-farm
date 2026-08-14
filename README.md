# Sustainable Farm - Product Transformation Application

**BIT × Infineon Excellence Program**  
**Project:** Sustainable Farm - Product Transformation System  
**Technology Stack:** React + Spring Boot + PostgreSQL  
**Owner:** Abdoul Ben Fatao SANON  
**Status:** Implementation Phase (Weeks 5-8)

---

## Project Overview

This application implements the Product Transformation system for sustainable dried mango production in Burkina Faso. It provides end-to-end batch traceability from harvest to export, integrating with the Plants workstream for harvest data and supporting HACCP compliance for EU market requirements.

---

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | React | 18.x |
| Frontend Language | JavaScript | ES6+ |
| Backend | Spring Boot | 3.x |
| Backend Language | Java | 17+ |
| Database | PostgreSQL | 15+ |
| Build Tool | Maven | 3.9+ |
| Version Control | Git + GitHub | - |

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
│   └── README.md
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
│   │   │   │       └── config/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── data.sql
│   │   └── test/
│   └── pom.xml
├── database/                    # Database scripts and migrations
│   ├── migrations/
│   ├── seeds/
│   └── schema.sql
├── docs/                        # Project documentation
│   ├── api/
│   ├── architecture/
│   └── database/
├── .gitignore
├── README.md
└── docker-compose.yml
```

---

## Data Model

The application implements the validated MERISE MCD from Week 5 Phase 3, consisting of:

### Core Processing Entities (6)
- **BATCH** - Central traceability entity
- **WASH_SORT_RECORD** - Washing and sorting stage data
- **DRYING_RUN** - Drying process data (core transformation)
- **PACKAGING_RECORD** - Packaging stage data
- **QC_CHECKPOINT** - Quality control checkpoint data
- **COMPLIANCE_RECORD** - HACCP compliance data

### Supporting Entities (3)
- **RAW_INTAKE** - Raw material intake from Plants
- **EQUIPMENT** - Machinery and equipment data
- **OPERATOR** - Personnel data

### Integration Entities (2)
- **HARVEST_EVENT** - Harvest data from Plants workstream
- **HISTORICAL_HARVEST** - Aggregated historical harvest data

---

## Business Requirements

The system implements the following functional requirements (FR-01 through FR-09):

- **FR-01:** Raw material intake tracking
- **FR-02:** Batch creation and traceability
- **FR-03:** Process stage recording
- **FR-04:** Resource consumption tracking
- **FR-05:** Quality control checkpoints
- **FR-06:** Storage integration
- **FR-07:** Packaging and lot coding
- **FR-08:** Export readiness documentation
- **FR-09:** Historical data analysis

---

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Maven 3.9+

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

# Run migrations
psql sustainable_farm < database/schema.sql
```

---

## API Documentation

API documentation will be available at `http://localhost:8080/swagger-ui.html` once the backend is running.

---

## Development Workflow

1. **Feature Branch:** Create a feature branch from `main`
2. **Development:** Implement changes following the validated data model
3. **Testing:** Run unit and integration tests
4. **Commit:** Commit with conventional commit messages
5. **Pull Request:** Create PR for code review
6. **Merge:** Merge to `main` after approval

---

## Validation Principles

**Critical Rule:** Do not invent new business requirements while coding.

The Week 3 and Week 4 validated artifacts are the functional baseline. If an implementation decision conflicts with those documents, flag the conflict before modifying the architecture.

---

## Status

**Current Phase:** Implementation (Weeks 5-8)  
**Last Updated:** 2026-08-14  
**Data Model Validation:** 10.0/10 (Week 5 Phase 3)  
**UML Consistency:** 10.0/10 (Week 5 Phase 3)

---

## License

Proprietary - BIT × Infineon Excellence Program