# Integration Matrix

**Project:** BIT × Infineon Excellence Program  
**Module:** Product Transformation  
**Week:** 10  
**Purpose:** Document all cross-module dependencies and integration status

---

## Integration Status Legend

| Status | Meaning |
|--------|---------|
| 🟢 **CONNECTED** | Contract implemented and integration tested |
| 🟡 **CONTRACT CONFIRMED** | Contract known; implementation pending |
| 🟠 **INFORMATION REQUESTED** | Waiting for another team |
| 🔴 **BLOCKING** | Cannot safely implement without clarification |
| ⚪ **NOT REQUIRED** | No business dependency identified |

---

## Module Dependencies

### 1. Plants Module

| Attribute | Value |
|-----------|-------|
| **Integration Direction** | Plants → Product Transformation |
| **Priority** | 🔴 High |
| **Status** | 🟡 CONTRACT CONFIRMED |
| **Data Owner** | Plants Team |
| **Consumer** | Product Transformation |

#### Available Endpoints

```http
GET /api/plants/varieties
GET /api/plants/growth-calendar
```

#### Data Fields

**Varieties:**
- `id`
- `id_ferme`
- `nom`
- `nombre_arbres`
- `espacement_inter_rang_m`
- `espacement_intra_rang_m`
- `densite_arbres_ha`
- `rendement_attendu_kg`
- `rendement_reel_kg`
- `vigueur`
- `bloc_parcelle`
- `origine_plant`
- `source`
- `date_maj`

**Growth Calendar:**
- `id`
- `id_ferme`
- `bloc_parcelle`
- `varietes`
- `date_plantation`
- `precision_date`
- `age_annees`
- `age_mois`
- `phase_croissance`
- `phase_tranche_annees`
- `stade_actuel`
- `phase_annees`
- `pluviometrie_locale_mm`
- `source`
- `date_maj`

#### Integration Conventions

**Parcel Identifier:**
- Store: `A`, `B`, `C`
- Do NOT store: `Bloc A`, `block A`, ` A`, `A `
- "Block" may be added only for display

**Field Naming:**
- Keep: `snake_case`
- Do NOT automatically convert to: `camelCase`

**Null Handling:**
- `null` = information unavailable
- `0` = actual zero value

#### Open Questions

- [x] Confirm stable IDs are permanent
- [x] Clarify Harvest/Récolte ownership (submodule of Plants or separate?) - CONFIRMED: Harvest is a task within Plants module, not yet built

---

### 2. Harvest / Récolte Module

| Attribute | Value |
|-----------|-------|
| **Integration Direction** | Harvest (Plants task) → Product Transformation |
| **Priority** | 🔴 Critical |
| **Status** | � FUTURE (Not yet built) |
| **Data Owner** | Plants Team |
| **Consumer** | Product Transformation |
| **Note** | Harvest is a task within Plants module, not yet constructed. Will provide actual yield and harvested quantities when available. |

#### Required Information

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

#### Data Flow

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

#### Open Questions

- [x] Is Harvest a submodule of Plants or a separate ninth module? - CONFIRMED: Task within Plants module
- [x] Who owns the Harvest module/team? - CONFIRMED: Plants Team
- [ ] What is the Harvest API contract? - PENDING: Task not yet built
- [ ] When will Harvest API be available? - PENDING: To be communicated by Plants Team

---

### 3. Crop Storage Module

| Attribute | Value |
|-----------|-------|
| **Integration Direction** | Product Transformation → Crop Storage |
| **Priority** | 🔴 High |
| **Status** | 🟠 INFORMATION REQUESTED |
| **Data Owner** | Crop Storage Team |
| **Consumer** | Product Transformation |

#### Expected Information

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

#### Data Flow

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

#### Open Questions

- [ ] What is the Crop Storage API contract?
- [ ] How does Product Transformation trigger storage entry?
- [ ] Who initiates storage exit (Storage or Sales)?

---

### 4. Machinery Module

| Attribute | Value |
|-----------|-------|
| **Integration Direction** | Machinery → Product Transformation |
| **Priority** | 🔴 Critical |
| **Status** | 🟠 INFORMATION REQUESTED |
| **Data Owner** | Machinery Team |
| **Consumer** | Product Transformation |

#### Expected Information

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

#### Data Flow

```text
Machinery
   ↓
Processing Run
   ↓
Batch
```

#### Example

```text
Plant A
   ↓
Dryer-01
   ↓
Drying Run DR-001
   ↓
Batch B-001
```

#### Open Questions

- [ ] What is the Machinery API contract?
- [ ] Does Product Transformation store only `machinery_id`, or is a local mapping required?
- [ ] How is machinery availability tracked?
- [ ] Who updates machinery status during processing?

---

### 5. Energy Supply Systems Module

| Attribute | Value |
|-----------|-------|
| **Integration Direction** | Energy → Product Transformation |
| **Priority** | 🔴 High |
| **Status** | 🟠 INFORMATION REQUESTED |
| **Data Owner** | Energy Supply Systems Team |
| **Consumer** | Product Transformation |

#### Required Contract Information

```text
energy_source
consumption
unit
timestamp
plant_id
machinery_id
```

#### Data Flow

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

#### Open Questions

- [ ] What is the Energy API contract?
- [ ] Who owns energy measurements (Energy or Machinery)?
- [ ] How is energy consumption linked to processing runs?
- [ ] What energy sources are available (Grid, Solar, Generator)?

---

### 6. Sales & Marketing Module

| Attribute | Value |
|-----------|-------|
| **Integration Direction** | Product Transformation → Sales & Marketing |
| **Priority** | 🔴 High |
| **Status** | 🟠 INFORMATION REQUESTED |
| **Data Owner** | Sales & Marketing Team |
| **Consumer** | Product Transformation |

#### Potential References

```text
product_id
sku
order_id
customer_id
quantity
destination
shipment_status
```

#### Data Flow

```text
Product Transformation
        ↓
Shipment
        ↓
Sales & Marketing
        ↓
Order / Customer / Destination
```

#### Integration Principle

Product Transformation provides physical product/shipment information.  
Sales & Marketing remains the owner of commercial information.

#### Open Questions

- [ ] What is the Sales & Marketing API contract?
- [ ] How does Product Transformation report shipment completion?
- [ ] What fields does Sales & Marketing expect from Product Transformation?
- [ ] Do not duplicate customer or commercial master data in Product Transformation

---

### 7. Fencing & Security Module

| Attribute | Value |
|-----------|-------|
| **Integration Direction** | TBD |
| **Priority** | 🟡 Medium |
| **Status** | ⚪ NOT REQUIRED (Assessment Pending) |
| **Data Owner** | Security Team |
| **Consumer** | TBD |

#### Assessment Criteria

Determine whether Security exposes information relevant to:
- Production-zone access
- Restricted machinery areas
- Visitor access
- Security incidents
- Facility traceability

#### Open Questions

- [ ] Does Product Transformation need security zone information?
- [ ] Are there security events that affect production traceability?
- [ ] Is operator access to machinery tracked by Security?

---

### 8. Visitor Management Module

| Attribute | Value |
|-----------|-------|
| **Integration Direction** | TBD |
| **Priority** | 🟡 Medium |
| **Status** | ⚪ NOT REQUIRED (Assessment Pending) |
| **Data Owner** | Visitor Management Team |
| **Consumer** | TBD |

#### Assessment Criteria

Determine whether Visitor Management exposes information relevant to:
- Production-zone access
- Visitor access
- Facility traceability

#### Open Questions

- [ ] Does Product Transformation need visitor access information?
- [ ] Are visitors involved in production operations?
- [ ] Is visitor presence relevant to batch traceability?

---

## Priority Integration Order

Based on the Product Transformation workflow:

```text
1. Plants / Harvest (CRITICAL - upstream dependency)
2. Machinery (CRITICAL - operational dependency)
3. Energy (HIGH - operational dependency)
4. Crop Storage (HIGH - downstream dependency)
5. Sales & Marketing (HIGH - downstream dependency)
6. Security (MEDIUM - assessment needed)
7. Visitor Management (MEDIUM - assessment needed)
```

---

## Summary Statistics

| Status | Count | Modules |
|--------|-------|---------|
| 🟢 CONNECTED | 1 | Plants |
| 🟡 FUTURE | 1 | Harvest (Plants task, not yet built) |
| 🟠 INFORMATION REQUESTED | 4 | Crop Storage, Machinery, Energy, Sales & Marketing |
| ⚪ NOT REQUIRED | 2 | Security, Visitor Management (assessment pending) |

---

## Next Steps

1. ✅ **COMPLETED:** Clarify Harvest ownership with Plants team - CONFIRMED: Plants task
2. **High Priority:** Request contracts from Machinery, Energy, Crop Storage, Sales & Marketing
3. **Assessment:** Determine if Security and Visitor Management integration is needed
4. ✅ **COMPLETED:** Plants integration (contract confirmed and implemented)

---

**Last Updated:** 2026-09-21  
**Status:** Phase 1 - Contract Collection in progress
