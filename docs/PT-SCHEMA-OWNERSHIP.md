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
- `db/demo/pt_demo_seed.sql`: inserts into `pt_equipment`.
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

The PT tables have no Flyway migrations yet: they are created outside the
migration chain. On an environment where the old `equipment` table was created
with Product Transformation's columns, the table must be renamed:

```sql
ALTER TABLE equipment RENAME TO pt_equipment;
```

This is only required if the existing `equipment` table holds PT data. If it
holds Machinery data, create `pt_equipment` from the PT entity instead. A
dedicated PT migration (Flyway) covering all PT tables is tracked as a follow-up
and is out of scope for this delivery.
