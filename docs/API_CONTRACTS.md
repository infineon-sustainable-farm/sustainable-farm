# API Contracts

**Project:** BIT × Infineon Excellence Program  
**Module:** Product Transformation  
**Week:** 10  
**Purpose:** Document confirmed API contracts for cross-module integration

---

## Contract Status Legend

| Status | Meaning |
|--------|---------|
| ✅ **CONFIRMED** | API contract reviewed and agreed upon by both teams |
| 🟡 **DRAFT** | Proposed contract awaiting confirmation |
| ❌ **REJECTED** | Contract rejected, revision needed |
| ⏳ **PENDING** | Contract not yet proposed or received |

---

## Contract 1: Plants Module

| Attribute | Value |
|-----------|-------|
| **Contract ID** | API-001 |
| **Source Module** | Plants |
| **Consumer Module** | Product Transformation |
| **Status** | ✅ CONFIRMED & IMPLEMENTED |
| **Version** | 1.0 |
| **Last Updated** | 2026-09-21 |
| **Contact** | Plants Team |
| **Implementation** | Backend + Frontend complete, 67/67 tests passing |

---

### Endpoint 1.1: Get Varieties

```http
GET /api/plants/varieties
```

#### Description

Retrieves all mango varieties with farm and parcel information.

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| None | - | - | No parameters required |

#### Response

**Status Code:** `200 OK`

**Response Body:**

```json
{
  "data": [
    {
      "id": "string",
      "id_ferme": "string",
      "nom": "string",
      "nombre_arbres": "integer",
      "espacement_inter_rang_m": "number",
      "espacement_intra_rang_m": "number",
      "densite_arbres_ha": "number",
      "rendement_attendu_kg": "number",
      "rendement_reel_kg": "number",
      "vigueur": "string",
      "bloc_parcelle": "string",
      "origine_plant": "string",
      "source": "string",
      "date_maj": "string (ISO 8601)"
    }
  ]
}
```

#### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique variety identifier |
| `id_ferme` | string | Farm identifier (maps to `farm_id`) |
| `nom` | string | Variety name |
| `nombre_arbres` | integer | Number of trees |
| `espacement_inter_rang_m` | number | Inter-row spacing in meters |
| `espacement_intra_rang_m` | number | Intra-row spacing in meters |
| `densite_arbres_ha` | number | Tree density per hectare |
| `rendement_attendu_kg` | number | Expected yield in kg |
| `rendement_reel_kg` | number | Actual yield in kg |
| `vigueur` | string | Plant vigor |
| `bloc_parcelle` | string | Parcel/block identifier (maps to `parcel_id`) |
| `origine_plant` | string | Plant origin |
| `source` | string | Data source |
| `date_maj` | string | Last update date (ISO 8601) |

#### Integration Notes

- `id_ferme` maps to Product Transformation's `farm_id`
- `bloc_parcelle` maps to Product Transformation's `block_id`/`parcel_id`
- Parcel identifier format: single character (A, B, C, ...)
- Store exact character: `"A"`, NOT `"Bloc A"`, `"block A"`, `" A"`, `"A "`
- Field naming: preserve `snake_case`, do not auto-convert to `camelCase`
- Null handling: `null` = information unavailable, `0` = actual zero value

---

### Endpoint 1.2: Get Growth Calendar

```http
GET /api/plants/growth-calendar
```

#### Description

Retrieves growth calendar information for crops across farms and parcels.

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `bloc_parcelle` | string | No | Filter by parcel/block identifier |
| `id_ferme` | string | No | Filter by farm identifier |

#### Response

**Status Code:** `200 OK`

**Response Body:**

```json
{
  "data": [
    {
      "id": "string",
      "id_ferme": "string",
      "bloc_parcelle": "string",
      "varietes": "string",
      "date_plantation": "string (ISO 8601)",
      "precision_date": "string",
      "age_annees": "integer",
      "age_mois": "integer",
      "phase_croissance": "string",
      "phase_tranche_annees": "string",
      "stade_actuel": "string",
      "phase_annees": "string",
      "pluviometrie_locale_mm": "number",
      "source": "string",
      "date_maj": "string (ISO 8601)"
    }
  ]
}
```

#### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique calendar entry identifier |
| `id_ferme` | string | Farm identifier |
| `bloc_parcelle` | string | Parcel/block identifier |
| `varietes` | string | Varieties planted |
| `date_plantation` | string | Plantation date (ISO 8601) |
| `precision_date` | string | Date precision |
| `age_annees` | integer | Age in years |
| `age_mois` | integer | Age in months |
| `phase_croissance` | string | Growth phase |
| `phase_tranche_annees` | string | Growth phase time range |
| `stade_actuel` | string | Current growth stage |
| `phase_annees` | string | Phase duration in years |
| `pluviometrie_locale_mm` | number | Local rainfall in mm |
| `source` | string | Data source |
| `date_maj` | string | Last update date (ISO 8601) |

#### Integration Notes

- Both filters are optional
- Can be used together for precise filtering
- Useful for correlating harvest timing with growth stages

---

## Contract 2: Harvest Module

| Attribute | Value |
|-----------|-------|
| **Contract ID** | API-002 |
| **Source Module** | Harvest (Plants task) |
| **Consumer Module** | Product Transformation |
| **Status** | 🟡 FUTURE (Not yet built) |
| **Version** | - |
| **Last Updated** | 2026-09-21 |
| **Contact** | Plants Team |
| **Note** | Harvest is a task within Plants module, not yet constructed. Will provide actual yield and harvested quantities when available. |

---

### Required Endpoints (To Be Defined)

#### Endpoint 2.1: Get Harvest Events

```http
GET /api/harvest/events
```

**Proposed Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `farm_id` | string | No | Filter by farm |
| `parcel_id` | string | No | Filter by parcel |
| `variety_id` | string | No | Filter by variety |
| `date_from` | string (ISO 8601) | No | Filter by harvest date range (start) |
| `date_to` | string (ISO 8601) | No | Filter by harvest date range (end) |

**Proposed Response Fields:**

```json
{
  "data": [
    {
      "harvest_id": "string",
      "farm_id": "string",
      "parcel_id": "string",
      "variety_id": "string",
      "harvest_date": "string (ISO 8601)",
      "quantity_harvested_kg": "number",
      "quality": "string",
      "grade": "string",
      "operator_id": "string",
      "source": "string",
      "created_at": "string (ISO 8601)",
      "updated_at": "string (ISO 8601)"
    }
  ]
}
```

#### Endpoint 2.2: Get Harvest Event by ID

```http
GET /api/harvest/events/{harvest_id}
```

**Proposed Response:** Same as individual event object above

---

### Open Questions

- [x] Who owns the Harvest module/team? - CONFIRMED: Plants Team
- [x] Is Harvest a submodule of Plants or separate? - CONFIRMED: Task within Plants module
- [ ] What is the actual API contract? - PENDING: Task not yet built
- [ ] When will the Harvest API be available? - PENDING: To be communicated by Plants Team

---

## Contract 3: Machinery Module

| Attribute | Value |
|-----------|-------|
| **Contract ID** | API-003 |
| **Source Module** | Machinery |
| **Consumer Module** | Product Transformation |
| **Status** | ⏳ PENDING |
| **Version** | - |
| **Last Updated** | - |
| **Contact** | Machinery Team |

---

### Required Endpoints (To Be Defined)

#### Endpoint 3.1: Get Machinery

```http
GET /api/machinery
```

**Proposed Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `plant_id` | string | No | Filter by plant/facility |
| `type` | string | No | Filter by machinery type |
| `processing_stage` | string | No | Filter by processing stage |
| `status` | string | No | Filter by status |

**Proposed Response Fields:**

```json
{
  "data": [
    {
      "machinery_id": "string",
      "name": "string",
      "type": "string",
      "processing_stage": "string",
      "capacity": "number",
      "status": "string",
      "plant_id": "string",
      "availability": "boolean",
      "created_at": "string (ISO 8601)",
      "updated_at": "string (ISO 8601)"
    }
  ]
}
```

#### Endpoint 3.2: Get Machinery by ID

```http
GET /api/machinery/{machinery_id}
```

**Proposed Response:** Same as individual machinery object above

#### Endpoint 3.3: Update Machinery Status

```http
PUT /api/machinery/{machinery_id}/status
```

**Proposed Request Body:**

```json
{
  "status": "string",
  "batch_id": "string",
  "operator_id": "string",
  "timestamp": "string (ISO 8601)"
}
```

---

### Open Questions

- [ ] What is the Machinery API contract?
- [ ] Does Product Transformation store only `machinery_id` or maintain local mapping?
- [ ] How is machinery availability tracked?
- [ ] Who updates machinery status during processing?

---

## Contract 4: Energy Supply Systems Module

| Attribute | Value |
|-----------|-------|
| **Contract ID** | API-004 |
| **Source Module** | Energy Supply Systems |
| **Consumer Module** | Product Transformation |
| **Status** | ⏳ PENDING |
| **Version** | - |
| **Last Updated** | - |
| **Contact** | Energy Team |

---

### Required Endpoints (To Be Defined)

#### Endpoint 4.1: Get Energy Consumption

```http
GET /api/energy/consumption
```

**Proposed Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `plant_id` | string | No | Filter by plant/facility |
| `machinery_id` | string | No | Filter by machinery |
| `batch_id` | string | No | Filter by batch |
| `date_from` | string (ISO 8601) | No | Filter by date range (start) |
| `date_to` | string (ISO 8601) | No | Filter by date range (end) |
| `energy_source` | string | No | Filter by energy source |

**Proposed Response Fields:**

```json
{
  "data": [
    {
      "energy_record_id": "string",
      "energy_source": "string",
      "consumption": "number",
      "unit": "string",
      "timestamp": "string (ISO 8601)",
      "plant_id": "string",
      "machinery_id": "string",
      "batch_id": "string",
      "created_at": "string (ISO 8601)"
    }
  ]
}
```

#### Endpoint 4.2: Record Energy Consumption

```http
POST /api/energy/consumption
```

**Proposed Request Body:**

```json
{
  "energy_source": "string",
  "consumption": "number",
  "unit": "string",
  "timestamp": "string (ISO 8601)",
  "plant_id": "string",
  "machinery_id": "string",
  "batch_id": "string",
  "operator_id": "string"
}
```

---

### Open Questions

- [ ] What is the Energy API contract?
- [ ] Who owns energy measurements (Energy or Machinery)?
- [ ] How is energy consumption linked to processing runs?
- [ ] What energy sources are available (Grid, Solar, Generator)?

---

## Contract 5: Crop Storage Module

| Attribute | Value |
|-----------|-------|
| **Contract ID** | API-005 |
| **Source Module** | Crop Storage |
| **Consumer Module** | Product Transformation |
| **Status** | ⏳ PENDING |
| **Version** | - |
| **Last Updated** | - |
| **Contact** | Crop Storage Team |

---

### Required Endpoints (To Be Defined)

#### Endpoint 5.1: Get Storage Locations

```http
GET /api/storage/locations
```

**Proposed Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `location` | string | No | Filter by location name |

**Proposed Response Fields:**

```json
{
  "data": [
    {
      "storage_id": "string",
      "location": "string",
      "capacity": "number",
      "available_capacity": "number",
      "temperature": "number",
      "humidity": "number",
      "created_at": "string (ISO 8601)",
      "updated_at": "string (ISO 8601)"
    }
  ]
}
```

#### Endpoint 5.2: Record Storage Entry

```http
POST /api/storage/entries
```

**Proposed Request Body:**

```json
{
  "storage_id": "string",
  "batch_id": "string",
  "quantity": "number",
  "packaging_id": "string",
  "operator_id": "string",
  "entry_timestamp": "string (ISO 8601)"
}
```

#### Endpoint 5.3: Record Storage Exit

```http
POST /api/storage/exits
```

**Proposed Request Body:**

```json
{
  "storage_id": "string",
  "batch_id": "string",
  "quantity": "number",
  "shipment_id": "string",
  "operator_id": "string",
  "exit_timestamp": "string (ISO 8601)"
}
```

---

### Open Questions

- [ ] What is the Crop Storage API contract?
- [ ] How does Product Transformation trigger storage entry?
- [ ] Who initiates storage exit (Storage or Sales)?
- [ ] What fields does Crop Storage expect from Product Transformation?

---

## Contract 6: Sales & Marketing Module

| Attribute | Value |
|-----------|-------|
| **Contract ID** | API-006 |
| **Source Module** | Sales & Marketing |
| **Consumer Module** | Product Transformation |
| **Status** | ⏳ PENDING |
| **Version** | - |
| **Last Updated** | - |
| **Contact** | Sales & Marketing Team |

---

### Required Endpoints (To Be Defined)

#### Endpoint 6.1: Report Shipment

```http
POST /api/sales/shipments
```

**Proposed Request Body:**

```json
{
  "shipment_id": "string",
  "batch_id": "string",
  "product_id": "string",
  "sku": "string",
  "quantity": "number",
  "packaging_details": "object",
  "operator_id": "string",
  "shipment_timestamp": "string (ISO 8601)"
}
```

#### Endpoint 6.2: Get Order Details

```http
GET /api/sales/orders/{order_id}
```

**Proposed Response Fields:**

```json
{
  "data": {
    "order_id": "string",
    "customer_id": "string",
    "destination": "string",
    "quantity_requested": "number",
    "status": "string",
    "created_at": "string (ISO 8601)",
    "updated_at": "string (ISO 8601)"
  }
}
```

---

### Open Questions

- [ ] What is the Sales & Marketing API contract?
- [ ] How does Product Transformation report shipment completion?
- [ ] What fields does Sales & Marketing expect from Product Transformation?
- [ ] Do not duplicate customer or commercial master data in Product Transformation

---

## Contract 7: Security Module

| Attribute | Value |
|-----------|-------|
| **Contract ID** | API-007 |
| **Source Module** | Fencing & Security |
| **Consumer Module** | Product Transformation |
| **Status** | ⏳ PENDING (Assessment Required) |
| **Version** | - |
| **Last Updated** | - |
| **Contact** | Security Team |

---

### Assessment Required

Before defining API contracts, determine if integration is needed:

- [ ] Does Product Transformation need security zone information?
- [ ] Are there security events that affect production traceability?
- [ ] Is operator access to machinery tracked by Security?

**If integration is NOT required:**
- Document: "Integration: Not required - Reason: Facility-level module; no Product Transformation dependency identified"

---

## Contract 8: Visitor Management Module

| Attribute | Value |
|-----------|-------|
| **Contract ID** | API-008 |
| **Source Module** | Visitor Management |
| **Consumer Module** | Product Transformation |
| **Status** | ⏳ PENDING (Assessment Required) |
| **Version** | - |
| **Last Updated** | - |
| **Contact** | Visitor Management Team |

---

### Assessment Required

Before defining API contracts, determine if integration is needed:

- [ ] Does Product Transformation need visitor access information?
- [ ] Are visitors involved in production operations?
- [ ] Is visitor presence relevant to batch traceability?

**If integration is NOT required:**
- Document: "Integration: Not required - Reason: Facility-level module; no Product Transformation dependency identified"

---

## Contract Summary

| Contract ID | Module | Status | Endpoints Confirmed |
|-------------|--------|--------|---------------------|
| API-001 | Plants | ✅ CONFIRMED & IMPLEMENTED | 2 |
| API-002 | Harvest | 🟡 FUTURE (Not yet built) | 0 |
| API-003 | Machinery | ⏳ PENDING | 0 |
| API-004 | Energy | ⏳ PENDING | 0 |
| API-005 | Crop Storage | ⏳ PENDING | 0 |
| API-006 | Sales & Marketing | ⏳ PENDING | 0 |
| API-007 | Security | ⏳ PENDING (Assessment) | 0 |
| API-008 | Visitor Management | ⏳ PENDING (Assessment) | 0 |

---

## Integration Priority

Based on Product Transformation workflow:

```text
1. API-001: Plants (✅ IMPLEMENTED - Backend + Frontend complete)
2. API-002: Harvest (🟡 FUTURE - Plants task, not yet built)
3. API-003: Machinery (CRITICAL - Pending contract)
4. API-004: Energy (HIGH - Pending contract)
5. API-005: Crop Storage (HIGH - Pending contract)
6. API-006: Sales & Marketing (HIGH - Pending contract)
7. API-007: Security (MEDIUM - Assessment needed)
8. API-008: Visitor Management (MEDIUM - Assessment needed)
```

---

## Contract Governance

### Version Control

- Each contract has a version number
- Changes require agreement from both teams
- Deprecated contracts must be supported for transition period

### Change Process

1. Propose change with rationale
2. Review by both teams
3. Update version number
4. Communicate breaking changes
5. Maintain backward compatibility when possible

### Testing

- All contracts must have integration tests
- Contract tests validate API compliance
- End-to-end tests validate business workflows

---

## Next Steps

### Immediate Actions

1. **Plants Integration:** Begin implementation of API-001 (already confirmed)
2. **Harvest Clarification:** Resolve Harvest ownership with Plants team
3. **Contract Requests:** Request contracts from Machinery, Energy, Crop Storage, Sales & Marketing
4. **Security Assessment:** Determine if Security integration is needed
5. **Visitor Assessment:** Determine if Visitor Management integration is needed

### Implementation Sequence

1. Implement Plants integration (API-001)
2. Wait for Harvest ownership clarification
3. Implement Machinery integration (API-003) when contract received
4. Implement Energy integration (API-004) when contract received
5. Implement Crop Storage integration (API-005) when contract received
6. Implement Sales & Marketing integration (API-006) when contract received

---

**Last Updated:** 2026-09-21  
**Status:** Phase 2 - API contracts documentation started, 1 confirmed, 7 pending
