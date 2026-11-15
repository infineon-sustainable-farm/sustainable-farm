# Product Transformation — Schema Ownership

## Decision

Two modules declared an `@Entity` on the same physical table `equipment`:

| Module | Entity | Table before | Table after |
|---|---|---|---|
| Machinery | `modules/machinery/entity/Equipment` | `equipment` | `equipment` (unchanged) |
| Product Transformation | `modules/producttransformation/equipment/model/Equipment` | `equipment` | **`pt_equipment`** |

**Machinery keeps `equipment`. Product Transformation owns `pt_equipment`.**

## Why

Hibernate cannot map two unrelated entities to the same table. With
`ddl-auto=create-drop` (tests) the last mapping wins and the other module's
inserts fail; in production the two modules would silently share a table whose
columns mean different things:

- Machinery: `id`, `name`, `category`, `stage`, `status`
- Product Transformation: `equipment_id`, `equipment_name`, `equipment_type`,
  `capacity_kg_per_hour`, `energy_consumption_kwh_per_kg`, ...

Observed failure before the fix:

```
insert into equipment (category,name,stage,status,id) values (?,?,?,?,default)
NULL not allowed for column "CAPACITY_KG_PER_HOUR"
```

Machinery's `EquipmentDeletionIntegrationTest` and `MachineryLogsIntegrationTest`
were red for this reason.

## What changed

- `modules/producttransformation/equipment/model/Equipment.java`:
  `@Table(name = "pt_equipment")`.
- `backend/src/main/resources/seed/producttransformation-demo-seed.sql`: inserts into `pt_equipment`.
- No Machinery entity, table, migration or business logic was modified.
- Foreign keys are unaffected: `drying_run.equipment_id` and
  `wash_sort_record.equipment_id` are plain columns referencing
  `pt_equipment.equipment_id`; no SQL query in the PT module used the table name
  (repositories reference the entity name `ProdTransEquipment`).

## Convention going forward

A new module must not reuse an existing module's table name. When a module needs
a similar concept, it owns a prefixed table (`pt_equipment`, `pt_operator`, ...).
The rule applies to entities, not to columns: `equipment_id` stays the FK column
name in PT child tables.

## Migration status

The PT schema is now declared by a Flyway migration owned by this module:

```
backend/src/main/resources/db/migration/V14__producttransformation_schema.sql
```

It creates the 11 PT tables with `IF NOT EXISTS`, adds
`harvest_event.plants_harvest_id`, and adds every primary key, uniqueness rule
and foreign key behind an existence guard. It was verified against two paths:

| Environment | Result |
|---|---|
| Empty database, V1 → V14 | 11 tables created, seed loads, 0 orphan FK |
| Database already built by Hibernate (tables present) | migration runs, nothing recreated, no error |

### Environments created before V14

Those databases were built by Hibernate and can contain the **old** PT table
under the name `equipment` (recognisable by an `equipment_id` column). Two
cases:

1. `equipment` has an `equipment_id` column → it is the old PT table:

   ```sql
   ALTER TABLE equipment RENAME TO pt_equipment;   -- foreign keys follow
   ```

   Run this **before** applying V14. `equipment` is Machinery's table name, so
   this rename must be decided by the team that owns Machinery: after it,
   Machinery needs its own table recreated (definition in
   `V9__machinery_and_plants_tables.sql`).

2. `equipment` has an `id` column → it is the Machinery table. Nothing to do;
   V14 creates `pt_equipment` alongside it.

V14 deliberately does **not** rename `equipment` automatically: renaming
another module's table is a cross-module decision, not a Product Transformation
migration step.
