# WEEK 10 SETUP

## Cross-Module Integration & System Readiness

**Project:** BIT × Infineon Excellence Program
**Module:** Product Transformation
**Week:** 10
**Objective:** Establish and validate the integration contracts between Product Transformation and the other project modules.

---

## 1. Week 10 Objective

Week 10 moves the project from **module-level development** to **system-level integration**.

The main objective is to ensure that every relevant interconnection has:

* a clearly identified data owner;
* a defined API/data contract;
* compatible identifiers;
* a documented direction of data flow;
* validated business relationships;
* at least one integration test or validation scenario.

### Target

> By the end of Week 10, no critical cross-module dependency should remain undefined.

---

# 2. Current Module Landscape

The project currently identifies the following modules:

| # | Module                     | Role                                            |
| - | -------------------------- | ----------------------------------------------- |
| 1 | **Plants**                 | Farm, parcel, variety and crop information      |
| 2 | **Product Transformation** | Raw material intake and transformation workflow |
| 3 | **Crop Storage**           | Storage and stock conditions                    |
| 4 | **Machinery**              | Processing equipment and machinery              |
| 5 | **Fencing & Security**     | Facility/security infrastructure                |
| 6 | **Visitor Management**     | Visitor/access management                       |
| 7 | **Energy Supply Systems**  | Energy supply and consumption                   |
| 8 | **Sales & Marketing**      | Commercial/product distribution layer           |

### Architecture note

Only **8 modules are currently identified**.

The Plants team has also mentioned a **Harvest/Récolte task**. Before finalizing the integration architecture, confirm whether Harvest is:

* a submodule of Plants, or
* a separate ninth module.

Do not create a separate dependency until this is confirmed.

---

# 3. Product Transformation Core Workflow

The Product Transformation workflow remains:

```text
Plants / Harvest
       ↓
Raw Intake
       ↓
Batch
       ↓
Washing & Sorting
       ↓
Drying
       ↓
QC / Cooling
       ↓
Packaging
       ↓
Storage
       ↓
Shipment
       ↓
Sales
```

Machinery and Energy provide operational context around processing.

Operators and QC provide accountability and validation.

---

# 4. Integration Matrix

| Module                     | Current information                                                                                         | Required information                                                                     | Priority    | Status               |
| -------------------------- | ----------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- | ----------- | -------------------- |
| **Plants**                 | `GET /api/plants/varieties`; `GET /api/plants/growth-calendar`; farm/parcel/variety data; parcel convention | Confirm stable IDs and final contract; clarify Harvest ownership                         | 🔴 High     | 🟡 Contract received |
| **Harvest / Récolte**      | Not yet available                                                                                           | Harvest ID, farm, parcel, variety, date, quantity harvested, quality if available        | 🔴 Critical | 🔴 Pending           |
| **Crop Storage**           | No confirmed contract                                                                                       | Storage ID, location, capacity, conditions, stock movement, entry/exit                   | 🔴 High     | 🔴 Pending           |
| **Machinery**              | Equipment domain exists internally                                                                          | Machinery ID, type, capacity, processing stage, status, plant/location, availability     | 🔴 Critical | 🔴 Pending           |
| **Fencing & Security**     | No confirmed dependency                                                                                     | Security zones/access constraints relevant to production                                 | 🟡 Medium   | ⚪ To assess          |
| **Visitor Management**     | No confirmed dependency                                                                                     | Visitor/access information if production-zone traceability requires it                   | 🟡 Medium   | ⚪ To assess          |
| **Energy Supply Systems**  | Energy requirements already defined for processing                                                          | Energy source, supply status, consumption, unit, timestamp, equipment/plant relationship | 🔴 High     | 🔴 Pending           |
| **Sales & Marketing**      | Shipment exists in Product Transformation                                                                   | Product/SKU, order/customer reference, quantity, destination, order/shipment status      | 🔴 High     | 🔴 Pending           |
| **Product Transformation** | Full processing workflow implemented                                                                        | Connect external identifiers and validate end-to-end integration                         | 🔴 Critical | 🟡 In progress       |

---

# 5. Plants Integration

## Available APIs

### Varieties

```http
GET /api/plants/varieties
```

Available fields include:

```text
id
id_ferme
nom
nombre_arbres
espacement_inter_rang_m
espacement_intra_rang_m
densite_arbres_ha
rendement_attendu_kg
rendement_reel_kg
vigueur
bloc_parcelle
origine_plant
source
date_maj
```

### Growth Calendar

```http
GET /api/plants/growth-calendar
```

Available fields include:

```text
id
id_ferme
bloc_parcelle
varietes
date_plantation
precision_date
age_annees
age_mois
phase_croissance
phase_tranche_annees
stade_actuel
phase_annees
pluviometrie_locale_mm
source
date_maj
```

Optional filters:

```text
bloc_parcelle
id_ferme
```

## Integration conventions

### Parcel identifier

Store:

```text
A
B
C
```

Do not store:

```text
Bloc A
block A
 A
A 
```

“Block” may be added only for display.

### Field naming

Keep:

```text
snake_case
```

Do not automatically convert to:

```text
camelCase
```

### Null handling

```text
null = information unavailable
0    = actual zero value
```

Product Transformation must preserve this distinction.

---

# 6. Harvest Dependency

The most important unresolved upstream dependency is Harvest.

Product Transformation needs to establish:

```text
Farm
  ↓
Parcel
  ↓
Variety
  ↓
Harvest Event
  ↓
Quantity Harvested
  ↓
Raw Intake
  ↓
Batch
```

Minimum expected data:

```text
harvest_id
id_ferme
bloc_parcelle
variety_id / variety
harvest_date
quantity_harvested_kg
quality / grade
operator_id
source
```

The exact contract must be confirmed with the responsible team.

---

# 7. Machinery Integration

Product Transformation must not duplicate Machinery master data unnecessarily.

Target relationship:

```text
Machinery
   ↓
Processing Run
   ↓
Batch
```

Minimum expected information:

```text
machinery_id
name
type
processing_stage
capacity
status
plant_id
availability
```

Example:

```text
Plant A
   ↓
Dryer-01
   ↓
Drying Run DR-001
   ↓
Batch B-001
```

Critical question:

> Does Product Transformation store only `machinery_id`, or is a local mapping required?

This must be confirmed before implementation.

---

# 8. Energy Integration

Energy consumption should eventually be traceable to processing.

Target:

```text
Plant
  ↓
Machinery
  ↓
Processing Run
  ↓
Energy Consumption
  ↓
Batch
```

Required contract information:

```text
energy_source
consumption
unit
timestamp
plant_id
machinery_id
```

The exact ownership of energy measurements must be confirmed with Energy Supply Systems.

---

# 9. Crop Storage Integration

Target:

```text
Packaging
    ↓
Storage Entry
    ↓
Storage Location
    ↓
Storage Conditions
    ↓
Storage Exit
    ↓
Shipment
```

Expected information:

```text
storage_id
location
capacity
available_capacity
temperature
humidity
stock_quantity
entry_timestamp
exit_timestamp
```

The exact fields should follow the Crop Storage team's existing model rather than being invented by Product Transformation.

---

# 10. Sales & Marketing Integration

Product Transformation should provide the physical product/shipment information.

Sales & Marketing should remain the owner of commercial information.

Target:

```text
Product Transformation
        ↓
Shipment
        ↓
Sales & Marketing
        ↓
Order / Customer / Destination
```

Potential references:

```text
product_id
sku
order_id
customer_id
quantity
destination
shipment_status
```

Do not duplicate customer or commercial master data inside Product Transformation.

---

# 11. Security & Visitor Management

These modules may not require direct integration with Product Transformation.

First determine whether they expose information relevant to:

* production-zone access;
* restricted machinery areas;
* visitor access;
* security incidents;
* facility traceability.

If there is no direct business dependency, document:

```text
Integration: Not required
Reason: Facility-level module; no Product Transformation dependency identified.
```

Avoid creating unnecessary APIs between modules.

---

# 12. Identifier Strategy

Every cross-module relationship must use a stable identifier.

Example:

```text
farm_id
parcel_id / bloc_parcelle
variety_id
harvest_id
batch_id
machinery_id
plant_id
storage_id
energy_record_id
shipment_id
order_id
```

### Principle

Do not join modules using display names when a stable ID exists.

Avoid:

```text
"Dryer 1"
"Bloc A"
"Mango Keitt"
```

as primary integration keys.

Prefer:

```text
machinery_id
bloc_parcelle
variety_id
```

---

# 13. Integration Status Model

Every dependency must receive one status:

| Status                       | Meaning                                       |
| ---------------------------- | --------------------------------------------- |
| 🟢 **CONNECTED**             | Contract implemented and integration tested   |
| 🟡 **CONTRACT CONFIRMED**    | Contract known; implementation pending        |
| 🟠 **INFORMATION REQUESTED** | Waiting for another team                      |
| 🔴 **BLOCKING**              | Cannot safely implement without clarification |
| ⚪ **NOT REQUIRED**           | No business dependency identified             |

---

# 14. Week 10 Execution Plan

## Phase 1 — Contract Collection

* [ ] Confirm Plants API contract
* [ ] Clarify Harvest ownership
* [ ] Request Machinery contract
* [ ] Request Crop Storage contract
* [ ] Request Energy contract
* [ ] Request Sales & Marketing contract
* [ ] Assess Security integration
* [ ] Assess Visitor Management integration

## Phase 2 — Identifier Mapping

Create:

```text
docs/INTEGRATION_MATRIX.md
```

Document:

* source module
* destination module
* endpoint
* identifier
* fields
* direction
* transformation/mapping
* status

## Phase 3 — Integration Implementation

Implement only confirmed contracts.

Priority:

```text
Plants / Harvest
      ↓
Product Transformation
      ↓
Machinery
      ↓
Energy
      ↓
Storage
      ↓
Sales
```

## Phase 4 — End-to-End Validation

Run a real scenario:

```text
Plant
 ↓
Parcel
 ↓
Variety
 ↓
Harvest
 ↓
Raw Intake
 ↓
Batch
 ↓
Washing
 ↓
Drying + Machinery + Energy
 ↓
QC
 ↓
Packaging
 ↓
Storage
 ↓
Shipment
 ↓
Sales
```

---

# 15. Week 10 Acceptance Criteria

Week 10 is complete when:

* [ ] All identified modules have an integration status
* [ ] Plants integration is confirmed
* [ ] Harvest dependency is clarified
* [ ] Machinery contract is confirmed
* [ ] Crop Storage contract is confirmed
* [ ] Energy contract is confirmed
* [ ] Sales & Marketing contract is confirmed
* [ ] Security dependency is assessed
* [ ] Visitor dependency is assessed
* [ ] Stable identifiers are documented
* [ ] No undocumented cross-module assumptions remain
* [ ] Critical integrations are implemented
* [ ] At least one complete end-to-end workflow is tested
* [ ] Integration failures are documented
* [ ] Remaining issues are converted into Week 11 tasks

---

# 16. Week 10 Deliverables

```text
docs/
├── WEEK_10_SETUP.md
├── INTEGRATION_MATRIX.md
├── API_CONTRACTS.md
├── IDENTIFIER_MAPPING.md
└── WEEK_10_VALIDATION.md
```

The final Week 10 outcome should be:

> **All module boundaries are understood, integration contracts are documented, critical dependencies are connected, and the complete production lifecycle can be validated end-to-end.**

Week 11 should then focus primarily on **closing integration gaps, fixing defects, UX refinement, and production deployment readiness**, rather than discovering new architectural dependencies.

---

## 17. Week 10 Verification Results (2026-09-23)

### Verification Commands

```bash
# Backend tests
cd backend && ./mvnw test
# Result: 361 passing, 0 failures, BUILD SUCCESS

# Frontend build
cd frontend && npm run build
# Result: 207KB gzipped, Vite build successful
```

### Environment

| Component | Version |
|-----------|---------|
| Java | 21.0.12.1 (OpenJDK) |
| Maven | 3.9.16 (via wrapper) |
| Node.js | 24.12.0 |
| Vite | 8.2.1 |
| React | 19.2.8 |
| Spring Boot | 4.1.0 |

### Key Fix Applied

- **EntityScan import**: Updated from `org.springframework.boot.autoconfigure.domain.EntityScan` (Spring Boot 3.x) to `org.springframework.boot.persistence.autoconfigure.EntityScan` (Spring Boot 4.1.0)

### PR Status

| Field | Value |
|-------|-------|
| Branch | `feature/producttransformation/init-merge` |
| Base | `develop` |
| Files | 272 changed, 29,438 insertions |
| Status | 🟢 Able to merge |
