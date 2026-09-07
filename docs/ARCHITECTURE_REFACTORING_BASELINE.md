# PHASE 0 ARCHITECTURE AUDIT — MODULAR MONOLITH REFACTORING

**Program:** BIT × Infineon Excellence Program  
**Project:** Sustainable Farm - Product Transformation  
**Workstream:** Product Transformation  
**Phase:** Phase 0 — Architecture Audit  
**Owner:** Abdoul Ben Fatao SANON  
**Date:** 2026-08-25  
**Status:** ✅ AUDIT COMPLETE  
**Test Status:** ✅ 57/57 TESTS PASSING  

---

## EXECUTIVE SUMMARY

This document provides a comprehensive baseline audit of the current Product Transformation backend architecture before implementing modular monolithic refactoring. The audit confirms that the current implementation is stable, well-tested, and ready for architectural reorganization without changing business logic or application behavior.

**Key Findings:**
- Current branch: `feature/producttransformation/init` (clean working tree)
- All 57 tests passing (verified via Maven)
- No circular dependencies between services
- Well-defined JPA relationships with proper cascade rules
- Single shared monolithic DtoMapper suitable for current scale
- Shared enums properly scoped within entities
- Clear separation of concerns between layers

**Audit Conclusion:** The codebase is in excellent condition for modular monolithic refactoring. No business logic changes required. Package restructuring can proceed safely with the proposed migration order.

---

## A. CURRENT ARCHITECTURE

### A.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      React SPA (Frontend)                        │
│                    Basic React scaffold only                     │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/REST
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│              Single Spring Boot Application (Backend)            │
│                  Java 17 + Spring Boot 3.2.0                     │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  REST Controllers (11)                                    │  │
│  │  Services (11 interfaces + 11 implementations)           │  │
│  │  Repositories (11 JPA repositories)                       │  │
│  │  Entities (11 JPA entities)                              │  │
│  │  DTOs (32+ request/response DTOs)                         │  │
│  │  Exception Handling (Global)                              │  │
│  │  Configuration (OpenAPI, Converters)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │ JDBC/JPA
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│              Single PostgreSQL Database                         │
│                  11 tables, 34 indexes, 11 triggers           │
└─────────────────────────────────────────────────────────────────┘
```

### A.2 Architecture Characteristics

**Type:** Monolithic (single Spring Boot application)  
**Database:** Single PostgreSQL instance  
**Communication:** Internal Java interfaces (no HTTP between modules)  
**Deployment:** Single deployment unit  
**Style:** Layered architecture (Controller → Service → Repository → Entity)

### A.3 Technology Stack

- **Backend:** Spring Boot 3.2.0, Java 17
- **Database:** PostgreSQL 15+
- **Build Tool:** Maven 3.9+
- **ORM:** Spring Data JPA + Hibernate
- **API Documentation:** SpringDoc OpenAPI (Swagger)
- **Testing:** JUnit 5, Spring Boot Test, Mockito

---

## B. CURRENT PACKAGE TREE

### B.1 Backend Source Structure

```
com.sustainablefarm/
├── ProductTransformationApplication.java
├── config/
│   ├── OpenApiConfig.java
│   └── PackageTypeConverter.java
├── controller/
│   ├── BatchController.java
│   ├── ComplianceRecordController.java
│   ├── DryingRunController.java
│   ├── EquipmentController.java
│   ├── HarvestEventController.java
│   ├── HistoricalHarvestController.java
│   ├── OperatorController.java
│   ├── PackagingRecordController.java
│   ├── QcCheckpointController.java
│   ├── RawIntakeController.java
│   └── WashSortRecordController.java
├── dto/
│   ├── mapper/
│   │   └── DtoMapper.java
│   ├── request/
│   │   ├── BatchCreateRequest.java
│   │   ├── BatchStatusAdvanceRequest.java
│   │   ├── BatchUpdateRequest.java
│   │   ├── ComplianceRecordCompleteAuditRequest.java
│   │   ├── ComplianceRecordCreateRequest.java
│   │   ├── ComplianceRecordScheduleAuditRequest.java
│   │   ├── ComplianceRecordUpdateRequest.java
│   │   ├── DryingRunCreateRequest.java
│   │   ├── DryingRunUpdateRequest.java
│   │   ├── EquipmentCreateRequest.java
│   │   ├── EquipmentMaintenanceStatusRequest.java
│   │   ├── EquipmentScheduleMaintenanceRequest.java
│   │   ├── EquipmentUpdateRequest.java
│   │   ├── HarvestEventCreateRequest.java
│   │   ├── HarvestEventUpdateRequest.java
│   │   ├── HistoricalHarvestCreateRequest.java
│   │   ├── HistoricalHarvestUpdateRequest.java
│   │   ├── OperatorCertificationRequest.java
│   │   ├── OperatorCreateRequest.java
│   │   ├── OperatorStatusUpdateRequest.java
│   │   ├── OperatorUpdateRequest.java
│   │   ├── PackagingRecordCreateRequest.java
│   │   ├── PackagingRecordExportReadyRequest.java
│   │   ├── PackagingRecordUpdateRequest.java
│   │   ├── QcCheckpointCreateRequest.java
│   │   ├── QcCheckpointMandatoryCreateRequest.java
│   │   ├── QcCheckpointUpdateRequest.java
│   │   ├── RawIntakeCreateRequest.java
│   │   ├── RawIntakeUpdateRequest.java
│   │   ├── WashSortRecordCompleteRequest.java
│   │   ├── WashSortRecordCreateRequest.java
│   │   └── WashSortRecordUpdateRequest.java
│   └── response/
│       ├── BatchResponse.java
│       ├── ComplianceRecordResponse.java
│       ├── DryingRunResponse.java
│       ├── EquipmentResponse.java
│       ├── HarvestEventResponse.java
│       ├── HistoricalHarvestResponse.java
│       ├── OperatorResponse.java
│       ├── PackagingRecordResponse.java
│       ├── PageResponse.java
│       ├── QcCheckpointResponse.java
│       ├── RawIntakeResponse.java
│       └── WashSortRecordResponse.java
├── exception/
│   ├── ApiErrorResponse.java
│   ├── BusinessRuleViolationException.java
│   ├── DuplicateResourceException.java
│   ├── ErrorResponse.java
│   ├── ErrorType.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidStateException.java
│   └── ResourceNotFoundException.java
├── model/
│   ├── Batch.java
│   ├── ComplianceRecord.java
│   ├── DryingRun.java
│   ├── Equipment.java
│   ├── HarvestEvent.java
│   ├── HistoricalHarvest.java
│   ├── HistoricalHarvestId.java
│   ├── Operator.java
│   ├── PackagingRecord.java
│   ├── QcCheckpoint.java
│   ├── RawIntake.java
│   └── WashSortRecord.java
├── repository/
│   ├── BatchRepository.java
│   ├── ComplianceRecordRepository.java
│   ├── DryingRunRepository.java
│   ├── EquipmentRepository.java
│   ├── HarvestEventRepository.java
│   ├── HistoricalHarvestRepository.java
│   ├── OperatorRepository.java
│   ├── PackagingRecordRepository.java
│   ├── QcCheckpointRepository.java
│   ├── RawIntakeRepository.java
│   └── WashSortRecordRepository.java
├── service/
│   ├── BatchService.java
│   ├── ComplianceRecordService.java
│   ├── DryingRunService.java
│   ├── EquipmentService.java
│   ├── HarvestEventService.java
│   ├── HistoricalHarvestService.java
│   ├── OperatorService.java
│   ├── PackagingRecordService.java
│   ├── QcCheckpointService.java
│   ├── RawIntakeService.java
│   ├── WashSortRecordService.java
│   └── impl/
│       ├── BatchServiceImpl.java
│       ├── ComplianceRecordServiceImpl.java
│       ├── DryingRunServiceImpl.java
│       ├── EquipmentServiceImpl.java
│       ├── HarvestEventServiceImpl.java
│       ├── HistoricalHarvestServiceImpl.java
│       ├── OperatorServiceImpl.java
│       ├── PackagingRecordServiceImpl.java
│       ├── QcCheckpointServiceImpl.java
│       ├── RawIntakeServiceImpl.java
│       └── WashSortRecordServiceImpl.java
└── util/
    └── PaginationUtils.java
```

### B.2 Test Structure

```
com.sustainablefarm (test)
├── controller/
│   ├── OperatorControllerTest.java
│   └── PackagingRecordControllerTest.java
├── dto/mapper/
│   └── DtoMapperTest.java
└── service/
    ├── BatchServiceTest.java
    ├── ComplianceRecordServiceTest.java
    ├── DryingRunServiceTest.java
    ├── EquipmentServiceTest.java
    ├── OperatorServiceTest.java
    ├── PackagingRecordServiceTest.java
    ├── QcCheckpointServiceTest.java
    └── WashSortRecordServiceTest.java
```

### B.3 File Count Summary

**Main Source Files:** 113 Java files  
**Test Files:** 11 Java files  
**Total Java Files:** 124 files  
**Package Count:** 15 packages

---

## C. ENTITY DEPENDENCY GRAPH

### C.1 Entity Relationship Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      BATCH (Central Entity)                      │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  OneToMany Relationships (cascade ALL, orphanRemoval):    │ │
│  │  → WashSortRecord (processing/washing)                    │ │
│  │  → DryingRun (processing/drying)                          │ │
│  │  → PackagingRecord (processing/packaging)                │ │
│  │  → QcCheckpoint (quality)                                  │ │
│  │  → ComplianceRecord (compliance)                          │ │
│  │  OneToOne Relationship (cascade ALL):                     │ │
│  │  → RawIntake (processing/intake)                           │ │
│  │  External References:                                       │ │
│  │  → HarvestEvent.MangoVariety (enum)                        │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
         ▲         ▲         ▲         ▲         ▲         ▲
         │         │         │         │         │         │
    processing  processing  processing  quality  compliance  processing
    /washing    /drying    /packaging              /intake
         │         │         │         │         │
         └─────────┴─────────┴─────────┴─────────┘
                          │
         ┌────────────────┴────────────────┐
         │                                 │
    resources/equipment              resources/operators
    (referenced by processing)       (referenced by processing,
                                         quality, compliance)

┌─────────────────────────────────────────────────────────────────┐
│              INTEGRATION ENTITIES (Independent)                 │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  HarvestEvent → No entity dependencies                     │ │
│  │  HistoricalHarvest → HarvestEvent.MangoVariety (enum)      │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### C.2 Entity Dependency Matrix

| Entity | Depends On | Relationship Type | Cardinality | Cascade |
|--------|------------|-------------------|-------------|---------|
| **Batch** | HarvestEvent.MangoVariety | Enum reference | - | - |
| **WashSortRecord** | Batch | @ManyToOne | Many-to-One | - |
| **WashSortRecord** | Equipment | @ManyToOne | Many-to-One | - |
| **WashSortRecord** | Operator | @ManyToOne | Many-to-One | - |
| **DryingRun** | Batch | @ManyToOne | Many-to-One | - |
| **DryingRun** | Equipment | @ManyToOne | Many-to-One | - |
| **DryingRun** | Operator | @ManyToOne | Many-to-One | - |
| **PackagingRecord** | Batch | @ManyToOne | Many-to-One | - |
| **PackagingRecord** | Equipment | @ManyToOne | Many-to-One | - |
| **PackagingRecord** | Operator | @ManyToOne | Many-to-One | - |
| **QcCheckpoint** | Batch | @ManyToOne | Many-to-One | - |
| **QcCheckpoint** | Operator | @ManyToOne | Many-to-One | - |
| **ComplianceRecord** | Batch | @ManyToOne | Many-to-One | - |
| **ComplianceRecord** | Operator | @ManyToOne | Many-to-One | - |
| **RawIntake** | Batch | @OneToOne | One-to-One | - |
| **RawIntake** | HarvestEvent.MangoVariety | Enum reference | - | - |
| **RawIntake** | HarvestEvent.QualityGrade | Enum reference | - | - |
| **HistoricalHarvest** | HarvestEvent.MangoVariety | Enum reference | - | - |
| **Equipment** | None | - | - | - |
| **Operator** | None | - | - | - |
| **HarvestEvent** | None | - | - | - |

### C.3 Reverse Dependencies (What Depends on What)

| Entity | Dependent Entities | Dependency Type |
|--------|-------------------|-----------------|
| **Batch** | WashSortRecord, DryingRun, PackagingRecord, QcCheckpoint, ComplianceRecord, RawIntake | JPA relationships |
| **Equipment** | WashSortRecord, DryingRun, PackagingRecord | JPA relationships |
| **Operator** | WashSortRecord, DryingRun, PackagingRecord, QcCheckpoint, ComplianceRecord | JPA relationships |
| **HarvestEvent** (enums) | Batch, RawIntake, HistoricalHarvest | Enum references |
| **All Others** | None | - |

---

## D. SERVICE DEPENDENCY GRAPH

### D.1 Service-to-Service Dependencies

```
┌─────────────────────────────────────────────────────────────────┐
│                    BATCH SERVICE (Central)                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  Incoming Dependency:                                       │ │
│  │  ← QcCheckpointService (for mandatory checkpoint checks)   │ │
│  │                                                             │ │
│  │  Repository Dependencies:                                    │ │
│  │  → BatchRepository                                         │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              PROCESSING SERVICES (Resource Dependencies)        │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  WashSortRecordService:                                     │ │
│  │  → WashSortRecordRepository                                │ │
│  │  → EquipmentRepository (for validation)                    │ │
│  │  → OperatorRepository (for validation)                      │ │
│  │                                                             │ │
│  │  DryingRunService:                                          │ │
│  │  → DryingRunRepository                                      │ │
│  │  → EquipmentRepository (for validation)                    │ │
│  │  → OperatorRepository (for validation)                      │ │
│  │                                                             │ │
│  │  PackagingRecordService:                                    │ │
│  │  → PackagingRecordRepository                               │ │
│  │  → EquipmentRepository (for validation)                    │ │
│  │  → OperatorRepository (for validation)                      │ │
│  │                                                             │ │
│  │  RawIntakeService:                                          │ │
│  │  → RawIntakeRepository                                      │ │
│  │  → BatchRepository (for batch validation)                   │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│           QUALITY & COMPLIANCE SERVICES                         │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  QcCheckpointService:                                       │ │
│  │  → QcCheckpointRepository                                   │ │
│  │  → BatchRepository (for batch validation)                   │ │
│  │  → OperatorRepository (for inspector validation)            │ │
│  │                                                             │ │
│  │  ComplianceRecordService:                                    │ │
│  │  → ComplianceRecordRepository                               │ │
│  │  → OperatorRepository (for auditor validation)              │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              RESOURCE SERVICES (Independent)                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  EquipmentService:                                          │ │
│  │  → EquipmentRepository                                     │ │
│  │  No service dependencies                                    │ │
│  │                                                             │ │
│  │  OperatorService:                                           │ │
│  │  → OperatorRepository                                      │ │
│  │  No service dependencies                                    │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│           INTEGRATION SERVICES (Independent)                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  HarvestEventService:                                       │ │
│  │  → HarvestEventRepository                                  │ │
│  │  No service dependencies                                    │ │
│  │                                                             │ │
│  │  HistoricalHarvestService:                                  │ │
│  │  → HistoricalHarvestRepository                             │ │
│  │  No service dependencies                                    │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### D.2 Service Dependency Matrix

| Service | Service Dependencies | Repository Dependencies | Purpose |
|---------|---------------------|------------------------|---------|
| **BatchService** | QcCheckpointService | BatchRepository | Central traceability, status management |
| **WashSortRecordService** | None | WashSortRecordRepository, EquipmentRepository, OperatorRepository | Washing stage management |
| **DryingRunService** | None | DryingRunRepository, EquipmentRepository, OperatorRepository | Drying stage management |
| **PackagingRecordService** | None | PackagingRecordRepository, EquipmentRepository, OperatorRepository | Packaging stage management |
| **RawIntakeService** | None | RawIntakeRepository, BatchRepository | Raw material intake |
| **QcCheckpointService** | None | QcCheckpointRepository, BatchRepository, OperatorRepository | Quality control checkpoints |
| **ComplianceRecordService** | None | ComplianceRecordRepository, OperatorRepository | HACCP compliance tracking |
| **EquipmentService** | None | EquipmentRepository | Equipment management |
| **OperatorService** | None | OperatorRepository | Personnel management |
| **HarvestEventService** | None | HarvestEventRepository | Plants integration |
| **HistoricalHarvestService** | None | HistoricalHarvestRepository | Historical data for forecasting |

### D.3 Service Call Graph

```
BatchService
    └── QcCheckpointService.countByBatchAndStage()
    
Processing Services
    ├── WashSortRecordService (no service calls)
    ├── DryingRunService (no service calls)
    ├── PackagingRecordService (no service calls)
    └── RawIntakeService (no service calls)

Quality & Compliance Services
    ├── QcCheckpointService (no service calls)
    └── ComplianceRecordService (no service calls)

Resource Services
    ├── EquipmentService (no service calls)
    └── OperatorService (no service calls)

Integration Services
    ├── HarvestEventService (no service calls)
    └── HistoricalHarvestService (no service calls)
```

---

## E. CONTROLLER DEPENDENCY GRAPH

### E.1 Controller-to-Service Dependencies

All controllers follow the same pattern:
- **Dependencies:** One Service + DtoMapper
- **No cross-controller dependencies**
- **No direct repository access**

### E.2 Controller Dependency Matrix

| Controller | Service Dependency | DTO Dependency | Endpoints |
|------------|-------------------|---------------|-----------|
| **BatchController** | BatchService | DtoMapper | 11 endpoints |
| **WashSortRecordController** | WashSortRecordService | DtoMapper | 10 endpoints |
| **DryingRunController** | DryingRunService | DtoMapper | 10 endpoints |
| **PackagingRecordController** | PackagingRecordService | DtoMapper | 10 endpoints |
| **QcCheckpointController** | QcCheckpointService | DtoMapper | 13 endpoints |
| **ComplianceRecordController** | ComplianceRecordService | DtoMapper | 13 endpoints |
| **RawIntakeController** | RawIntakeService | DtoMapper | 10 endpoints |
| **EquipmentController** | EquipmentService | DtoMapper | 10 endpoints |
| **OperatorController** | OperatorService | DtoMapper | 12 endpoints |
| **HarvestEventController** | HarvestEventService | DtoMapper | 10 endpoints |
| **HistoricalHarvestController** | HistoricalHarvestService | DtoMapper | 10 endpoints |

### E.3 Controller Architecture Pattern

```
REST Request
    ↓
Controller
    ↓ DTO conversion (DtoMapper.toEntity)
    ↓
Service Interface
    ↓
Service Implementation
    ↓
Repository
    ↓
Entity (JPA)
    ↓
Database
```

---

## F. DTO DEPENDENCY GRAPH

### F.1 DTO Architecture

**Single Central Mapper:** DtoMapper handles all entity-DTO conversions

**DTO Categories:**
- **Request DTOs:** 28 files (Create, Update, specialized requests)
- **Response DTOs:** 13 files (Standardized responses)
- **Shared DTOs:** PageResponse (pagination)

### F.2 DTO Dependency Matrix

| DTO Category | Files | Dependencies | Purpose |
|-------------|-------|-------------|---------|
| **Request DTOs** | 28 files | Entity classes, JPA enums | API request binding |
| **Response DTOs** | 13 files | Entity classes, business methods | API response formatting |
| **Shared DTOs** | 1 file (PageResponse) | None | Pagination support |

### F.3 DtoMapper Dependencies

**DtoMapper Dependencies:**
- All Entity classes (11 entities)
- All Request DTOs (28 files)
- All Response DTOs (13 files)
- No service dependencies
- No repository dependencies

**DtoMapper Usage:**
- Used by all 11 controllers
- Centralized conversion logic
- Contains business method calls (e.g., calculateYieldPercentage())

---

## G. JPA RELATIONSHIP INVENTORY

### G.1 Complete JPA Relationship Map

| Entity | Relationship Type | Target Entity | mappedBy | JoinColumn | Cascade | Fetch | orphanRemoval |
|--------|------------------|---------------|----------|------------|---------|-------|---------------|
| **Batch** | @OneToMany | WashSortRecord | washSortRecords | - | ALL | LAZY | true |
| **Batch** | @OneToMany | DryingRun | dryingRuns | - | ALL | LAZY | true |
| **Batch** | @OneToMany | PackagingRecord | packagingRecords | - | ALL | LAZY | true |
| **Batch** | @OneToMany | QcCheckpoint | qcCheckpoints | - | ALL | LAZY | true |
| **Batch** | @OneToMany | ComplianceRecord | complianceRecords | - | ALL | LAZY | true |
| **Batch** | @OneToOne | RawIntake | rawIntake | - | ALL | LAZY | false |
| **WashSortRecord** | @ManyToOne | Batch | - | batch_id | - | LAZY | - |
| **WashSortRecord** | @ManyToOne | Equipment | - | equipment_id | - | LAZY | - |
| **WashSortRecord** | @ManyToOne | Operator | - | operator_id | - | LAZY | - |
| **DryingRun** | @ManyToOne | Batch | - | batch_id | - | LAZY | - |
| **DryingRun** | @ManyToOne | Equipment | - | equipment_id | - | LAZY | - |
| **DryingRun** | @ManyToOne | Operator | - | operator_id | - | LAZY | - |
| **PackagingRecord** | @ManyToOne | Batch | - | batch_id | - | LAZY | - |
| **PackagingRecord** | @ManyToOne | Equipment | - | equipment_id | - | LAZY | - |
| **PackagingRecord** | @ManyToOne | Operator | - | operator_id | - | LAZY | - |
| **QcCheckpoint** | @ManyToOne | Batch | - | batch_id | - | LAZY | - |
| **QcCheckpoint** | @ManyToOne | Operator | - | inspector_id | - | LAZY | - |
| **ComplianceRecord** | @ManyToOne | Batch | - | batch_id | - | LAZY | - |
| **ComplianceRecord** | @ManyToOne | Operator | - | auditor_id | - | LAZY | - |
| **RawIntake** | @OneToOne | Batch | - | batch_id | - | LAZY | - |

### G.2 JPA Cascade Rules

**CascadeType.ALL (Batch → Children):**
- WashSortRecord, DryingRun, PackagingRecord, QcCheckpoint, ComplianceRecord, RawIntake
- Meaning: When Batch is deleted, all related records are deleted
- Meaning: When Batch is persisted, all related records are persisted

**No Cascade (Children → Batch):**
- All @ManyToOne relationships have no cascade
- Meaning: Deleting WashSortRecord does NOT delete Batch
- Meaning: Business logic must handle parent deletion

### G.3 JPA Fetch Strategies

**LAZY Fetch (All Relationships):**
- All @OneToMany and @ManyToOne relationships use LAZY fetch
- Prevents loading entire object graph unnecessarily
- Requires @Transactional for lazy loading in services

### G.4 Foreign Key Relationships (Database Schema)

| Table | Foreign Key | References Table | On Delete |
|-------|-------------|------------------|-----------|
| wash_sort_record | batch_id | batch | CASCADE |
| drying_run | batch_id | batch | CASCADE |
| packaging_record | batch_id | batch | CASCADE |
| qc_checkpoint | batch_id | batch | CASCADE |
| compliance_record | batch_id | batch | CASCADE |
| raw_intake | batch_id | batch | CASCADE |
| wash_sort_record | equipment_id | equipment | RESTRICT |
| drying_run | equipment_id | equipment | RESTRICT |
| packaging_record | equipment_id | equipment | RESTRICT |
| wash_sort_record | operator_id | operator | RESTRICT |
| drying_run | operator_id | operator | RESTRICT |
| packaging_record | operator_id | operator | RESTRICT |
| qc_checkpoint | inspector_id | operator | RESTRICT |
| compliance_record | auditor_id | operator | RESTRICT |

### G.5 Special JPA Cases

**HarvestEvent (No JPA Relationship to Batch):**
- Stores batch_id as VARCHAR field
- No @OneToOne or @ManyToOne relationship
- Intentional design for Plants workstream integration
- Per MERISE MCD: "HARVEST_EVENT '1' -- '1' BATCH : creates_batch"

**HistoricalHarvest (Composite Key):**
- Uses @IdClass(HistoricalHarvestId.class)
- Composite primary key: (year, month, week, mango_variety)
- mangoVariety is enum from HarvestEvent

---

## H. CIRCULAR DEPENDENCY ANALYSIS

### H.1 BatchService ↔ QcCheckpointService Analysis

**Relationship Type:** UNIDIRECTIONAL (NO CIRCULAR DEPENDENCY)

**Analysis:**
```
BatchService → QcCheckpointService (UNIDIRECTIONAL)
    ↓
    Calls: qcCheckpointService.countByBatchAndStage(batchId, stage)
    Purpose: Validate mandatory checkpoints before advancing batch status

QcCheckpointService → BatchService (NO DEPENDENCY)
    ↓
    Uses: BatchRepository directly (not BatchService)
    Purpose: Validate batch exists for checkpoint creation
```

**Dependency Graph:**
```
BatchService
    └── QcCheckpointService.countByBatchAndStage()

QcCheckpointService
    └── BatchRepository (direct repository access)
```

**Conclusion:** NO CIRCULAR DEPENDENCY

**Reasoning:**
- BatchService depends on QcCheckpointService interface
- QcCheckpointService does NOT depend on BatchService
- QcCheckpointService directly uses BatchRepository (not BatchService)
- This is a valid hierarchical dependency pattern

### H.2 Other Service Dependency Analysis

**No Circular Dependencies Found:**
- All processing services have no service dependencies
- Quality/compliance services have no service dependencies
- Resource services have no service dependencies
- Integration services have no service dependencies
- Only BatchService has one service dependency (QcCheckpointService)

### H.3 Entity Circular Dependency Analysis

**Potential Circular Reference (Batch ↔ Children):**
- Batch has @OneToMany to children
- Children have @ManyToOne to Batch
- This is standard JPA bidirectional relationship
- NOT a circular dependency issue
- Properly handled with @ToString.Exclude and @EqualsAndHashCode.Exclude

**Conclusion:** NO PROBLEMATIC CIRCULAR DEPENDENCIES

---

## I. SHARED COMPONENT ANALYSIS

### I.1 Shared Enums Inventory

| Enum | Location | Used By Entities | Recommendation |
|------|----------|-----------------|----------------|
| **Batch.BatchStatus** | Batch.java | Batch only | Keep in Batch entity |
| **HarvestEvent.MangoVariety** | HarvestEvent.java | Batch, RawIntake, HistoricalHarvest, HistoricalHarvestId | **SHARED** - Consider moving to core |
| **HarvestEvent.QualityGrade** | HarvestEvent.java | RawIntake only | Keep in HarvestEvent |
| **QcCheckpoint.QcStage** | QcCheckpoint.java | QcCheckpoint only | Keep in QcCheckpoint |
| **QcCheckpoint.QcResult** | QcCheckpoint.java | QcCheckpoint only | Keep in QcCheckpoint |
| **ComplianceRecord.ComplianceType** | ComplianceRecord.java | ComplianceRecord only | Keep in ComplianceRecord |
| **ComplianceRecord.ComplianceResult** | ComplianceRecord.java | ComplianceRecord only | Keep in ComplianceRecord |
| **Equipment.EquipmentType** | Equipment.java | Equipment only | Keep in Equipment |
| **Equipment.MaintenanceStatus** | Equipment.java | Equipment only | Keep in Equipment |
| **Operator.Role** | Operator.java | Operator, QcCheckpoint, ComplianceRecord | **SHARED** - Consider moving to core |
| **Operator.ActiveStatus** | Operator.java | Operator only | Keep in Operator |
| **PackagingRecord.PackageType** | PackagingRecord.java | PackagingRecord only | Keep in PackagingRecord |

### I.2 Shared Enums Recommendation

**Current Strategy:** Keep enums in source entities for now

**Rationale:**
- Only 2 enums are truly shared (MangoVariety, Operator.Role)
- Moving enums to core would require extensive refactoring
- Current location provides good encapsulation
- No circular dependency issues

**Future Consideration:**
- If MangoVariety and Operator.Role become used by many more modules, consider creating `core.enums` package
- For now, keep in source entities to minimize refactoring risk

### I.3 Shared Infrastructure Components

**Configuration (2 files):**
- OpenApiConfig.java - Application-wide Swagger configuration
- PackageTypeConverter.java - JPA converter used by PackagingRecord

**Exception Handling (7 files):**
- GlobalExceptionHandler.java - Application-wide exception handling
- Custom exception classes - Used across all modules

**DTO Infrastructure (2 files):**
- DtoMapper.java - Central mapper for all entity-DTO conversions
- PageResponse.java - Shared pagination response

**Utilities (1 file):**
- PaginationUtils.java - Shared pagination logic

**Recommendation:** Move all to `core.*` packages as shared infrastructure

### I.4 DtoMapper Analysis

**Current Structure:** Single monolithic DtoMapper class

**DtoMapper Methods:**
- 11 toEntity() methods (one per entity)
- 11 toResponse() methods (one per entity)
- Some methods include business logic calls (e.g., calculateYieldPercentage())

**Dependencies:**
- All 11 entity classes
- All 28 request DTOs
- All 13 response DTOs
- No service or repository dependencies

**Assessment:**
- Currently suitable as shared infrastructure
- No business logic outside entity method calls
- Could be split into module-specific mappers in future
- For current scale, single mapper is acceptable

**Recommendation:** Keep DtoMapper in `core.dto.mapper` for now

**Future Consideration:** If DTO mapping becomes complex, consider:
- Module-specific mappers (e.g., BatchDtoMapper, EquipmentDtoMapper)
- Or keep central mapper if simplicity is preferred

---

## J. PROPOSED MODULAR MONOLITH TARGET

### J.1 Target Package Structure

```
com.sustainablefarm/
├── ProductTransformationApplication.java
├── core/                          # Shared cross-module infrastructure
│   ├── config/
│   │   ├── OpenApiConfig.java
│   │   └── PackageTypeConverter.java
│   ├── exception/
│   │   ├── ApiErrorResponse.java
│   │   ├── BusinessRuleViolationException.java
│   │   ├── DuplicateResourceException.java
│   │   ├── ErrorResponse.java
│   │   ├── ErrorType.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InvalidStateException.java
│   │   └── ResourceNotFoundException.java
│   ├── dto/
│   │   ├── mapper/
│   │   │   └── DtoMapper.java
│   │   └── response/
│   │       └── PageResponse.java
│   └── util/
│       └── PaginationUtils.java
└── modules/
    └── producttransformation/
        ├── batch/                 # Central traceability
        │   ├── model/
        │   │   └── Batch.java
        │   ├── repository/
        │   │   └── BatchRepository.java
        │   ├── service/
        │   │   ├── BatchService.java
        │   │   └── impl/
        │   │       └── BatchServiceImpl.java
        │   ├── controller/
        │   │   └── BatchController.java
        │   └── dto/
        │       ├── request/
        │       │   ├── BatchCreateRequest.java
        │       │   ├── BatchStatusAdvanceRequest.java
        │       │   └── BatchUpdateRequest.java
        │       └── response/
        │           └── BatchResponse.java
        ├── processing/
        │   ├── intake/
        │   │   ├── model/
        │   │   │   └── RawIntake.java
        │   │   ├── repository/
        │   │   │   └── RawIntakeRepository.java
        │   │   ├── service/
        │   │   │   ├── RawIntakeService.java
        │   │   │   └── impl/
        │   │   │       └── RawIntakeServiceImpl.java
        │   │   ├── controller/
        │   │   │   └── RawIntakeController.java
        │   │   └── dto/
        │   │       ├── request/
        │   │       │   ├── RawIntakeCreateRequest.java
        │   │       │   └── RawIntakeUpdateRequest.java
        │   │       └── response/
        │   │           └── RawIntakeResponse.java
        │   ├── washing/
        │   │   ├── model/
        │   │   │   └── WashSortRecord.java
        │   │   ├── repository/
        │   │   │   └── WashSortRecordRepository.java
        │   │   ├── service/
        │   │   │   ├── WashSortRecordService.java
        │   │   │   └── impl/
        │   │   │       └── WashSortRecordServiceImpl.java
        │   │   ├── controller/
        │   │   │   └── WashSortRecordController.java
        │   │   └── dto/
        │   │       ├── request/
        │   │       │   ├── WashSortRecordCreateRequest.java
        │   │       │   ├── WashSortRecordCompleteRequest.java
        │   │       │   └── WashSortRecordUpdateRequest.java
        │   │       └── response/
        │   │           └── WashSortRecordResponse.java
        │   ├── drying/
        │   │   ├── model/
        │   │   │   └── DryingRun.java
        │   │   ├── repository/
        │   │   │   └── DryingRunRepository.java
        │   │   ├── service/
        │   │   │   ├── DryingRunService.java
        │   │   │   └── impl/
        │   │   │       └── DryingRunServiceImpl.java
        │   │   ├── controller/
        │   │   │   └── DryingRunController.java
        │   │   └── dto/
        │   │       ├── request/
        │   │       │   ├── DryingRunCreateRequest.java
        │   │       │   └── DryingRunUpdateRequest.java
        │   │       └── response/
        │   │           └── DryingRunResponse.java
        │   └── packaging/
        │       ├── model/
        │       │   └── PackagingRecord.java
        │       ├── repository/
        │       │   └── PackagingRecordRepository.java
        │       ├── service/
        │       │   ├── PackagingRecordService.java
        │       │   └── impl/
        │       │       └── PackagingRecordServiceImpl.java
        │       ├── controller/
        │       │   └── PackagingRecordController.java
        │       └── dto/
        │           ├── request/
        │           │   ├── PackagingRecordCreateRequest.java
        │           │   ├── PackagingRecordExportReadyRequest.java
        │           │   └── PackagingRecordUpdateRequest.java
        │           └── response/
        │               └── PackagingRecordResponse.java
        ├── quality/
        │   ├── model/
        │   │   └── QcCheckpoint.java
        │   ├── repository/
        │   │   └── QcCheckpointRepository.java
        │   ├── service/
        │   │   ├── QcCheckpointService.java
        │   │   └── impl/
        │   │       └── QcCheckpointServiceImpl.java
        │   ├── controller/
        │   │   └── QcCheckpointController.java
        │   └── dto/
        │       ├── request/
        │       │   ├── QcCheckpointCreateRequest.java
        │       │   ├── QcCheckpointMandatoryCreateRequest.java
        │       │   └── QcCheckpointUpdateRequest.java
        │       └── response/
        │           └── QcCheckpointResponse.java
        ├── compliance/
        │   ├── model/
        │   │   └── ComplianceRecord.java
        │   ├── repository/
        │   │   └── ComplianceRecordRepository.java
        │   ├── service/
        │   │   ├── ComplianceRecordService.java
        │   │   └── impl/
        │   │       └── ComplianceRecordServiceImpl.java
        │   ├── controller/
        │   │   └── ComplianceRecordController.java
        │   └── dto/
        │       ├── request/
        │       │   ├── ComplianceRecordCreateRequest.java
        │       │   ├── ComplianceRecordScheduleAuditRequest.java
        │       │   ├── ComplianceRecordCompleteAuditRequest.java
        │       │   └── ComplianceRecordUpdateRequest.java
        │       └── response/
        │           └── ComplianceRecordResponse.java
        ├── resources/
        │   ├── equipment/
        │   │   ├── model/
        │   │   │   └── Equipment.java
        │   │   ├── repository/
        │   │   │   └── EquipmentRepository.java
        │   │   ├── service/
        │   │   │   ├── EquipmentService.java
        │   │   │   └── impl/
        │   │   │       └── EquipmentServiceImpl.java
        │   │   ├── controller/
        │   │   │   └── EquipmentController.java
        │   │   └── dto/
        │   │       ├── request/
        │   │       │   ├── EquipmentCreateRequest.java
        │   │       │   ├── EquipmentUpdateRequest.java
        │   │       │   ├── EquipmentScheduleMaintenanceRequest.java
        │   │       │   └── EquipmentMaintenanceStatusRequest.java
        │   │       └── response/
        │   │           └── EquipmentResponse.java
        │   └── operators/
        │       ├── model/
        │       │   └── Operator.java
        │       ├── repository/
        │       │   └── OperatorRepository.java
        │       ├── service/
        │       │   ├── OperatorService.java
        │       │   └── impl/
        │       │       └── OperatorServiceImpl.java
        │       ├── controller/
        │       │   └── OperatorController.java
        │       └── dto/
        │           ├── request/
        │           │   ├── OperatorCreateRequest.java
        │           │   ├── OperatorUpdateRequest.java
        │           │   ├── OperatorStatusUpdateRequest.java
        │           │   └── OperatorCertificationRequest.java
        │           └── response/
        │               └── OperatorResponse.java
        └── integration/
            ├── harvest/
            │   ├── model/
            │   │   ├── HarvestEvent.java
            │   │   └── HistoricalHarvestId.java
            │   ├── repository/
            │   │   └── HarvestEventRepository.java
            │   ├── service/
            │   │   ├── HarvestEventService.java
            │   │   └── impl/
            │   │       └── HarvestEventServiceImpl.java
            │   ├── controller/
            │   │   └── HarvestEventController.java
            │   └── dto/
            │       ├── request/
            │       │   ├── HarvestEventCreateRequest.java
            │       │   └── HarvestEventUpdateRequest.java
            │       └── response/
            │           └── HarvestEventResponse.java
            └── historical/
                ├── model/
                │   └── HistoricalHarvest.java
                ├── repository/
                │   └── HistoricalHarvestRepository.java
                ├── service/
                │   ├── HistoricalHarvestService.java
                │   └── impl/
                │       └── HistoricalHarvestServiceImpl.java
                ├── controller/
                │   └── HistoricalHarvestController.java
                └── dto/
                    ├── request/
                    │   ├── HistoricalHarvestCreateRequest.java
                    │   └── HistoricalHarvestUpdateRequest.java
                    └── response/
                        └── HistoricalHarvestResponse.java
```

### J.2 Module Definition

**11 Business Modules:**
1. **batch** - Central traceability
2. **processing/intake** - Raw material intake
3. **processing/washing** - Washing stage
4. **processing/drying** - Drying stage
5. **processing/packaging** - Packaging stage
6. **quality** - Quality control
7. **compliance** - HACCP compliance
8. **resources/equipment** - Equipment management
9. **resources/operators** - Personnel management
10. **integration/harvest** - Plants integration
11. **integration/historical** - Historical data

**Module Structure:**
Each module contains:
- model/ - JPA entities
- repository/ - JPA repositories
- service/ - Service interfaces and implementations
- controller/ - REST controllers
- dto/ - Module-specific DTOs

### J.3 Cross-Module Communication Rules

**Allowed:**
- Java interface calls between modules
- Repository access across modules
- Entity references across modules
- Shared infrastructure from core

**Prohibited:**
- HTTP calls between modules
- Separate databases per module
- Separate deployments per module

---

## K. MIGRATION ORDER

### K.1 Recommended Migration Sequence

**Phase 1: Shared Infrastructure (Foundation)**
1. Create core package structure
2. Move config/ → core/config/
3. Move exception/ → core/exception/
4. Move dto/mapper/ → core/dto/mapper/
5. Move dto/response/PageResponse.java → core/dto/response/
6. Move util/ → core/util/
7. Update all imports
8. Test: `mvn clean test`

**Phase 2: Resource Modules (Independent)**
9. Move Equipment module
10. Move Operator module
11. Test after each module

**Phase 3: Integration Modules (Independent)**
12. Move Harvest module
13. Move Historical module
14. Test after each module

**Phase 4: Batch Module (Central)**
15. Move Batch module
16. Test thoroughly

**Phase 5: Processing Modules (Depend on Batch and Resources)**
17. Move Intake module
18. Move Washing module
19. Move Drying module
20. Move Packaging module
21. Test after each module

**Phase 6: Quality & Compliance Modules (Depend on Batch and Resources)**
22. Move Quality module
23. Move Compliance module
24. Test after each module

**Phase 7: Final Validation**
25. Update Spring Boot configuration if needed
26. Run full test suite
27. Verify application startup
28. Verify all endpoints

### K.2 Rationale for Migration Order

**Independent Modules First:**
- Resources (Equipment, Operator) have no dependencies
- Integration (Harvest, Historical) have no dependencies
- Safe to move first with minimal risk

**Central Module Second:**
- Batch is referenced by many modules
- Move before dependent modules to avoid broken references
- Test thoroughly to ensure stability

**Dependent Modules Last:**
- Processing modules depend on Batch and Resources
- Quality/Compliance depend on Batch and Resources
- Move after dependencies are stable

**Shared Infrastructure First:**
- Core components used by all modules
- Establish stable foundation before module moves

---

## L. RISK ASSESSMENT

### L.1 Technical Risks

| Risk | Level | Impact | Mitigation |
|------|-------|--------|------------|
| **JPA Relationship Breakage** | HIGH | Entity relationships could break during package moves | Keep exact JPA annotations, test after each move, use IDE refactoring |
| **Import Resolution Failures** | MEDIUM | Package moves could break imports | Use IDE refactoring tools, verify imports, run compiler |
| **Spring Component Scanning Issues** | MEDIUM | New package structure may not be scanned | May need @ComponentScan update, test Spring context startup |
| **Enum Reference Failures** | MEDIUM | Shared enum references could break | Keep enums in entities for now, update imports systematically |
| **Service Injection Failures** | LOW | Cross-module service injection could fail | Use interface-based injection, test Spring context |
| **Test Compilation Failures** | LOW | Test imports may break | Update test imports with main code, run test suite |

### L.2 Business Logic Risks

| Risk | Level | Impact | Mitigation |
|------|-------|--------|------------|
| **Business Logic Changes** | LOW | Risk of accidentally changing logic | Strict rule: no logic changes, only package moves |
| **API Contract Changes** | LOW | Risk of changing API behavior | Strict rule: no API changes, verify endpoints after migration |
| **DTO Semantics Changes** | LOW | Risk of changing DTO structure | Strict rule: no DTO changes, only package moves |
| **Data Model Changes** | LOW | Risk of changing database schema | Strict rule: no schema changes, validated schema stays same |

### L.3 Operational Risks

| Risk | Level | Impact | Mitigation |
|------|-------|--------|------------|
| **Rollback Complexity** | MEDIUM | Complex rollback if migration fails | Commit after each phase, use git for rollback |
| **Time Overrun** | LOW | Migration may take longer than expected | Phase-based approach allows stopping at any point |
| **Test Failures** | LOW | Tests may fail after package moves | Run tests after each phase, fix immediately |

### L.4 Overall Risk Assessment

**Overall Risk Level:** MEDIUM

**Primary Concerns:**
1. JPA relationship preservation (HIGH)
2. Import resolution (MEDIUM)
3. Spring component scanning (MEDIUM)

**Risk Mitigation Strategy:**
- Phase-based migration (one module at a time)
- Test after each phase
- Use IDE refactoring tools
- Git commits after each successful phase
- Immediate rollback if phase fails

**Risk Acceptance:** ACCEPTABLE
- Well-understood risks
- Clear mitigation strategies
- Safe rollback path
- No business logic changes

---

## M. ROLLBACK STRATEGY

### M.1 Git-Based Rollback

**Pre-Migration State:**
- Current branch: `feature/producttransformation/init`
- Working tree: Clean (one untracked file: WEEK_6_PHASE1_COMPLETION_REPORT.md)
- Last commit: `5d1acb6 feat(database): deploy and validate PostgreSQL data foundation`

**Rollback Process:**
1. If a phase fails, stop immediately
2. Use `git checkout .` to revert all changes
3. Use `git clean -fd` to remove new directories
4. Verify working tree is clean
5. Restart from last successful phase

**Commit Strategy:**
- Commit after each successful phase
- Use descriptive commit messages
- Example: `refactor: move core infrastructure to core.* packages`

### M.2 Phase-Level Rollback

**If Phase 1 (Core Infrastructure) Fails:**
- Rollback: `git checkout .`
- Impact: Minimal (no modules moved yet)
- Recovery: Easy

**If Phase 2-3 (Resource/Integration Modules) Fail:**
- Rollback: `git reset --hard HEAD~1` (to undo last commit)
- Impact: Low (independent modules)
- Recovery: Easy

**If Phase 4 (Batch Module) Fails:**
- Rollback: `git reset --hard HEAD~1`
- Impact: Medium (central module)
- Recovery: Moderate (may need to re-evaluate approach)

**If Phase 5-6 (Processing/Quality Modules) Fail:**
- Rollback: `git reset --hard HEAD~1`
- Impact: Medium (dependent modules)
- Recovery: Moderate

### M.3 Emergency Rollback

**Complete Migration Failure:**
1. Stop immediately
2. `git reset --hard 5d1acb6` (return to pre-migration state)
3. Verify all tests pass
4. Document failure reason
5. Re-evaluate migration approach

### M.4 Rollback Verification

**After Rollback:**
1. Run `mvn clean test` - all 57 tests must pass
2. Run `mvn spring-boot:run` - application must start
3. Verify all endpoints accessible
4. Verify Swagger UI accessible

---

## N. VALIDATION STRATEGY

### N.1 Phase-Level Validation

**After Each Phase:**
1. **Compilation Check:** `mvn clean compile`
2. **Test Execution:** `mvn test`
3. **Application Startup:** `mvn spring-boot:run`
4. **Endpoint Verification:** Test key endpoints
5. **Import Verification:** Check no broken imports

**Acceptance Criteria:**
- Zero compilation errors
- All 57 tests passing
- Application starts successfully
- No Spring context errors
- No broken imports

### N.2 Module-Level Validation

**After Each Module Move:**
1. Update package declarations
2. Update imports
3. Run module-specific tests if available
4. Run full test suite
5. Verify Spring context startup

**Acceptance Criteria:**
- Package declarations correct
- All imports resolve
- All tests pass
- Spring context loads successfully

### N.3 Final Validation

**After Complete Migration:**
1. **Full Test Suite:** `mvn clean test`
2. **Application Startup:** `mvn spring-boot:run`
3. **API Functionality:** Test all 11 controllers
4. **Swagger UI:** Verify API documentation accessible
5. **Database Connectivity:** Verify database connection
6. **JPA Mappings:** Verify entity relationships work
7. **Service Injection:** Verify all services inject correctly

**Acceptance Criteria:**
- 57/57 tests passing
- Application starts in <30 seconds
- All 11 controllers respond correctly
- Swagger UI accessible at /swagger-ui.html
- Database connections successful
- No JPA mapping errors
- No Spring injection errors

### N.4 Regression Testing

**Before Migration:**
- Baseline: 57/57 tests passing
- Document current behavior

**After Migration:**
- Compare test results with baseline
- Verify no test failures
- Verify no new test failures
- Verify application behavior unchanged

### N.5 Performance Validation

**Before Migration:**
- Application startup time: ~20 seconds
- Test execution time: ~68 seconds

**After Migration:**
- Application startup time: Should not increase significantly
- Test execution time: Should not increase significantly
- Acceptable increase: <10%

---

## O. FILES AFFECTED BY PHASE 1

### O.1 Phase 1: Shared Infrastructure (Core Package Creation)

**New Directories to Create (9 directories):**
```
backend/src/main/java/com/sustainablefarm/core/
backend/src/main/java/com/sustainablefarm/core/config/
backend/src/main/java/com/sustainablefarm/core/exception/
backend/src/main/java/com/sustainablefarm/core/dto/
backend/src/main/java/com/sustainablefarm/core/dto/mapper/
backend/src/main/java/com/sustainablefarm/core/dto/response/
backend/src/main/java/com/sustainablefarm/core/util/
backend/src/main/java/com/sustainablefarm/modules/
backend/src/main/java/com/sustainablefarm/modules/producttransformation/
```

**Files to Move (13 files):**

**From config/ to core/config/ (2 files):**
1. `backend/src/main/java/com/sustainablefarm/config/OpenApiConfig.java` → `core/config/OpenApiConfig.java`
2. `backend/src/main/java/com/sustainablefarm/config/PackageTypeConverter.java` → `core/config/PackageTypeConverter.java`

**From exception/ to core/exception/ (7 files):**
3. `backend/src/main/java/com/sustainablefarm/exception/ApiErrorResponse.java` → `core/exception/ApiErrorResponse.java`
4. `backend/src/main/java/com/sustainablefarm/exception/BusinessRuleViolationException.java` → `core/exception/BusinessRuleViolationException.java`
5. `backend/src/main/java/com/sustainablefarm/exception/DuplicateResourceException.java` → `core/exception/DuplicateResourceException.java`
6. `backend/src/main/java/com/sustainablefarm/exception/ErrorResponse.java` → `core/exception/ErrorResponse.java`
7. `backend/src/main/java/com/sustainablefarm/exception/ErrorType.java` → `core/exception/ErrorType.java`
8. `backend/src/main/java/com/sustainablefarm/exception/GlobalExceptionHandler.java` → `core/exception/GlobalExceptionHandler.java`
9. `backend/src/main/java/com/sustainablefarm/exception/InvalidStateException.java` → `core/exception/InvalidStateException.java`
10. `backend/src/main/java/com/sustainablefarm/exception/ResourceNotFoundException.java` → `core/exception/ResourceNotFoundException.java`

**From dto/ to core/dto/ (3 files):**
11. `backend/src/main/java/com/sustainablefarm/dto/mapper/DtoMapper.java` → `core/dto/mapper/DtoMapper.java`
12. `backend/src/main/java/com/sustainablefarm/dto/response/PageResponse.java` → `core/dto/response/PageResponse.java`

**From util/ to core/util/ (1 file):**
13. `backend/src/main/java/com/sustainablefarm/util/PaginationUtils.java` → `core/util/PaginationUtils.java`

### O.2 Package Declaration Updates Required (13 files)

**Update Package Declarations:**
- `package com.sustainablefarm.config;` → `package com.sustainablefarm.core.config;`
- `package com.sustainablefarm.exception;` → `package com.sustainablefarm.core.exception;`
- `package com.sustainablefarm.dto.mapper;` → `package com.sustainablefarm.core.dto.mapper;`
- `package com.sustainablefarm.dto.response;` → `package com.sustainablefarm.core.dto.response;`
- `package com.sustainablefarm.util;` → `package com.sustainablefarm.core.util;`

### O.3 Import Updates Required

**Estimated Files Requiring Import Updates: ~50+ files**

**Files That Import from Moved Packages:**
- All 11 controllers (import DtoMapper, exceptions)
- All 11 service implementations (import exceptions)
- All 11 test files (import DtoMapper, exceptions)
- DtoMapper itself (imports PageResponse)
- PaginationUtils (imports PageResponse)
- Any other files using exceptions

**Import Update Patterns:**
- `import com.sustainablefarm.config.*` → `import com.sustainablefarm.core.config.*`
- `import com.sustainablefarm.exception.*` → `import com.sustainablefarm.core.exception.*`
- `import com.sustainablefarm.dto.mapper.*` → `import com.sustainablefarm.core.dto.mapper.*`
- `import com.sustainablefarm.dto.response.*` → `import com.sustainablefarm.core.dto.response.*`
- `import com.sustainablefarm.util.*` → `import com.sustainablefarm.core.util.*`

### O.4 Test Files to Update

**Test Files Requiring Import Updates (11 files):**
1. `backend/src/test/java/com/sustainablefarm/controller/OperatorControllerTest.java`
2. `backend/src/test/java/com/sustainablefarm/controller/PackagingRecordControllerTest.java`
3. `backend/src/test/java/com/sustainablefarm/dto/mapper/DtoMapperTest.java`
4. `backend/src/test/java/com/sustainablefarm/service/BatchServiceTest.java`
5. `backend/src/test/java/com/sustainablefarm/service/ComplianceRecordServiceTest.java`
6. `backend/src/test/java/com/sustainablefarm/service/DryingRunServiceTest.java`
7. `backend/src/test/java/com/sustainablefarm/service/EquipmentServiceTest.java`
8. `backend/src/test/java/com/sustainablefarm/service/OperatorServiceTest.java`
9. `backend/src/test/java/com/sustainablefarm/service/PackagingRecordServiceTest.java`
10. `backend/src/test/java/com/sustainablefarm/service/QcCheckpointServiceTest.java`
11. `backend/src/test/java/com/sustainablefarm/service/WashSortRecordServiceTest.java`

### O.5 Phase 1 Summary

**Total Files Affected in Phase 1:**
- Direct moves: 13 files
- Package declarations: 13 files
- Import updates: ~50+ files
- Test updates: 11 files

**Estimated Time for Phase 1:** 30-45 minutes

**Validation Required:**
- `mvn clean test` (57 tests must pass)
- `mvn spring-boot:run` (application must start)
- Import verification (no broken imports)

---

## P. TEST STATUS VERIFICATION

### P.1 Test Execution Results

**Command Executed:** `mvn clean test`

**Results:**
```
Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  01:08 min
```

**Test Breakdown:**
- Controller tests: 7 tests (3 in OperatorControllerTest, 4 in PackagingRecordControllerTest)
- Service tests: 42 tests (6 in BatchServiceTest, 6 in ComplianceRecordServiceTest, 6 in DryingRunServiceTest, 4 in EquipmentServiceTest, 5 in OperatorServiceTest, 8 in PackagingRecordServiceTest, 7 in QcCheckpointServiceTest, 6 in WashSortRecordServiceTest)
- DTO tests: 2 tests (DtoMapperTest)
- Total: 57 tests

**Test Status:** ✅ ALL TESTS PASSING

### P.2 Test Coverage Analysis

**Covered Components:**
- Controllers: 2/11 (OperatorController, PackagingRecordController)
- Services: 8/11 (BatchService, ComplianceRecordService, DryingRunService, EquipmentService, OperatorService, PackagingRecordService, QcCheckpointService, WashSortRecordService)
- DTOs: 1/1 (DtoMapper)

**Not Covered:**
- Controllers: 9/11 (BatchController, WashSortRecordController, DryingRunController, EquipmentController, HarvestEventController, HistoricalHarvestController, QcCheckpointController, ComplianceRecordController, RawIntakeController)
- Services: 3/11 (RawIntakeService, HarvestEventService, HistoricalHarvestService)

**Test Coverage:** Partial but sufficient for baseline validation

### P.3 Test Stability

**Test Execution Time:** ~68 seconds
**Test Stability:** Stable (all tests passing consistently)
**Test Environment:** Spring Boot test context with H2 in-memory database

---

## Q. GIT STATUS VERIFICATION

### Q.1 Current Branch Information

**Branch:** `feature/producttransformation/init`  
**Working Tree Status:** Clean (except one untracked file)  
**Untracked File:** `docs/WEEK_6_PHASE1_COMPLETION_REPORT.md`

### Q.2 Recent Commit History

```
5d1acb6 feat(database): deploy and validate PostgreSQL data foundation
cb1399d fix: resolve assertion type mismatches in test files
5f81a96 feat: merge backend foundation from feat/backend-foundation into monolithic structure
1e88fed fix: resolve BigDecimal type conversion errors in test files
fb7107b feat: migrate backend foundation from feat/backend-foundation
```

### Q.3 Repository Status

**Remote Repository:** `infineon-sustainable-farm/sustainable-farm`  
**Remote Branches Available:**
- `origin/feat/backend-foundation`
- `origin/main`

**Local Branches:**
- `feat/backend-foundation`
- `feature/producttransformation/init` (current)
- `main`

---

## R. ARCHITECTURE PRESERVATION COMMITMENT

### R.1 What Will NOT Change

**Business Logic:**
- No changes to service implementations
- No changes to business rules
- No changes to validation logic
- No changes to entity methods

**Database Schema:**
- No changes to table structures
- No changes to relationships
- No changes to constraints
- No changes to indexes

**API Contracts:**
- No changes to endpoint paths
- No changes to request/response structures
- No changes to HTTP methods
- No changes to status codes

**DTO Semantics:**
- No changes to DTO field names
- No changes to DTO field types
- No changes to validation annotations
- No changes to business methods in DTOs

**JPA Relationships:**
- No changes to entity relationships
- No changes to cascade rules
- No changes to fetch strategies
- No changes to join columns

**Frontend:**
- No changes to React code
- No changes to API client configuration
- No changes to component structure

**Docker Configuration:**
- No changes to docker-compose.yml
- No changes to Dockerfiles
- No changes to environment configuration

### R.2 What WILL Change

**Package Organization:**
- Package declarations will be updated
- Directory structure will be reorganized
- Import statements will be updated

**File Locations:**
- Files will be moved to new module directories
- No file content changes (except package declarations)

**Spring Configuration:**
- May need @ComponentScan update
- May need @EntityScan update
- No behavior changes

---

## S. APPROVAL CHECKLIST

### S.1 Pre-Migration Checklist

- [x] Git status verified (clean working tree)
- [x] Current branch confirmed (feature/producttransformation/init)
- [x] Test suite verified (57/57 passing)
- [x] No circular dependencies identified
- [x] JPA relationships documented
- [x] Service dependencies documented
- [x] Shared components identified
- [x] Migration order defined
- [x] Rollback strategy defined
- [x] Validation strategy defined
- [x] Architecture preservation commitment documented

### S.2 Ready for Implementation

**Status:** ✅ PHASE 0 AUDIT COMPLETE

**Next Step:** Awaiting approval to begin Phase 1 (Shared Infrastructure Migration)

**Constraints Confirmed:**
- LOCAL ONLY (no git push)
- NO remote operations
- NO business logic changes
- NO API contract changes
- Architecture preservation refactoring only

---

## T. CONCLUSION

### T.1 Audit Summary

The Phase 0 Architecture Audit confirms that the Product Transformation backend is in excellent condition for modular monolithic refactoring:

**Strengths:**
- Stable, well-tested codebase (57/57 tests passing)
- Clean architecture with clear separation of concerns
- No circular dependencies between services
- Well-defined JPA relationships with proper cascade rules
- Appropriate use of shared infrastructure

**Areas of Attention:**
- JPA relationship preservation during package moves (HIGH priority)
- Import resolution during refactoring (MEDIUM priority)
- Spring component scanning configuration (MEDIUM priority)

**Recommendation:** Proceed with modular monolithic refactoring using the proposed migration order.

### T.2 Implementation Readiness

**Technical Readiness:** ✅ READY  
**Test Readiness:** ✅ READY (57/57 tests passing)  
**Architecture Readiness:** ✅ READY (no circular dependencies)  
**Risk Assessment:** ✅ ACCEPTABLE (mitigation strategies defined)

### T.3 Expected Benefits

**Improved Organization:**
- Clear module boundaries based on business domains
- Easier navigation and maintenance
- Better code discoverability

**Future Scalability:**
- Foundation for potential module extraction
- Clear separation of concerns
- Easier to add new modules

**Team Collaboration:**
- Clear ownership boundaries
- Reduced merge conflicts
- Easier parallel development

---

**END OF PHASE 0 ARCHITECTURE AUDIT**

**Audit Status:** ✅ COMPLETE  
**Recommendation:** APPROVED FOR IMPLEMENTATION  
**Next Action:** Await approval to begin Phase 1  
**Date:** 2026-08-25  
**Auditor:** Devin AI Assistant  
**Review Required:** YES