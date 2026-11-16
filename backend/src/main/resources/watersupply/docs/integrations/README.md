# Water Supply integrations

Water Supply now exposes data and actions that other modules asked for in the
[integration audit](../watersupply-integration-audit.md). This folder has one page per consuming
team. Each page covers what is ready, how to call it, the decisions recorded with that team and
what is still open. The pages were updated on 2026-10-29 with the answers of Plants, Visitor
Management and Product Transformation.

| Team | Page | What you get |
|---|---|---|
| Plants | [watersupply-for-plants.md](watersupply-for-plants.md) | Irrigation and consumption per `blockCode`, rain gauge rainfall per block, irrigation need per block |
| Visitor Management | [watersupply-for-visitor-management.md](watersupply-for-visitor-management.md) | Tank level, savings, irrigation status, visit counter, planned outages, visit notifications, safety briefing, all by stop name |
| Product Transformation | [watersupply-for-product-transformation.md](watersupply-for-product-transformation.md) | Water per lot and activity, washing water quality per lot, availability forecast and buffer alert |
| IoT / Site Security | [watersupply-for-iot-and-site-security.md](watersupply-for-iot-and-site-security.md) | Valve command journal and safety rules; the gateway and permissions are yours to decide |

## How to call Water Supply

The application is a modular monolith: every module runs in the same JVM. You have two options:

- **HTTP**, on the same backend (`http://<backend>/api/...`). This is the stable contract described in
  these pages and in Swagger (`/swagger-ui`, tag "Water Supply - Integrations").
- **In-process**, by injecting the Water Supply Spring service named on each page. This is faster, but
  your module then compiles against our classes. Tell us if you do this, so we warn you before
  changing a signature.

Water Supply only reads from other modules in one place: `PlantsGrowthCalendarAdapter`, which
reads the Plants growth calendar (see the Plants page).

## Conventions shared by every endpoint

- Identifiers are UUIDs (`farmId`, `zoneId`, `sourceId`). Instants are ISO-8601 UTC
  (`2026-10-13T10:00:00Z`) and days are `yyyy-MM-dd`.
- Errors use one envelope: `{"timestamp","status","error","code","message"}`. The codes are:

  | HTTP | `code` | When |
  |---|---|---|
  | 400 | `VALIDATION_ERROR` | A required body field is missing or out of range |
  | 400 | `MALFORMED_REQUEST` | Unreadable JSON, a wrong UUID or date format, or a missing query parameter |
  | 400 | `INVALID_ARGUMENT` | A business rule rejected the value (unknown activity, overlapping period, and so on) |
  | 404 | `NOT_FOUND` | Unknown farm, zone, source, block mapping or device |
  | 409 | `CONFLICT` / `DATA_CONSTRAINT_VIOLATION` | Device offline, outage in progress, duplicate mapping |
  | 503 | `VALVE_GATEWAY_UNAVAILABLE` | No device gateway is configured yet |

- Authentication and roles are not handled by any module yet. They will come from the global
  platform. Until then every endpoint is open.
- Several values are working defaults, not validated references: quality thresholds, Product
  Transformation coefficients, growth phase factors and the buffer. All of them can be changed
  through environment variables. Each page lists the ones that concern you, and we need your answer
  to replace them.

## Database migrations

`V11__zone_block_mapping.sql`, `V12__watersupply_integration.sql` and
`V13__watersupply_integration_feedback.sql` belong to Water Supply. The next free number is **V14**.
Please announce your number before you open a pull request, because the migration folder is shared.

## Known platform issue (all module owners)

On a fresh PostgreSQL database, migrations V1 to V13 apply cleanly, but the application then stops
at startup with `Schema validation: missing table [...]`. Production runs
`spring.jpa.hibernate.ddl-auto=validate`, and several entities have no Flyway migration. The tests
do not catch this because they let Hibernate create the schema.

The tables without a migration on 2026-10-29 were (the list grows with every module merged):

| Module | Tables |
|---|---|
| Visitor Management | `agri_activity`, `booking`, `briefing`, `event`, `feedback`, `registration`, `staff`, `survey_send`, `time_slot`, `tour_stop`, `visitor`, `workshop` |
| Site Security | `site_security_gates`, `site_security_logs`, `site_security_users`, `site_security_zones`, `zone_entity_requirements` |
| Plants | `calendrier_croissance`, `currency_rate`, `fertilizer_movement`, `fertilizer_product`, `growth_phase_yield_share`, `harvest_record`, `health_finding`, `health_inspection`, `health_issue_reference`, `health_treatment`, `nursery_batch`, `nursery_event`, `population_event`, `variety_reference` |
| Machinery | `fuel_log`, `maintenance_schedule`, `operator_assignment`, `repair_log`, `spare_part`, `usage_log` |
| Sales & Marketing | `alerts`, `campaign_metrics`, `campaigns`, `certifications`, `competitor_prices`, `customer_certifications`, `customer_contacts`, `customers`, `demand_forecasts`, `demand_reports`, `forecast_model_runs`, `order_items`, `orders`, `pricing_history`, `pricing_recommendations`, `product_batches`, `products`, `report_files`, `sales_channel_targets`, `sales_channels`, `shipment_events`, `shipments` |

Each owner needs to add a migration for their tables. Once those migrations exist, the Water Supply
schema passes `validate` on PostgreSQL (checked on 2026-10-29 by booting with `validate` after the
missing tables were created). Until then, for local development only, the backend starts with
`SPRING_JPA_HIBERNATE_DDL_AUTO=update`, which creates the missing tables after the migrations.
