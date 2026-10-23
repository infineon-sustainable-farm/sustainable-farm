# Module water supply (SWMS)

Intelligent water management for the farm: irrigation driven by the crops' actual
needs, tracking of sources and consumption, water quality, drought alerts.
It is the reference module of the program: the other modules (machinery, plants)
share the same structure and the same conventions.

## Where the code lives

| Part | Location |
|---|---|
| REST API, services, entities | `backend/src/main/java/com/infineonbit/sustainablefarm/modules/watersupply/` |
| Database schema (migrations V1 -> V10) | `backend/src/main/resources/db/migration/` |
| Interface (views, hooks, API client) | `frontend/src/features/watersupply/` |
| Sensors and ESP32 firmware | `iot/` |

The module is mounted under `/watersupply` in the application (`routes.jsx`), like
`/machinery` and `/plants`: each module carries its own menu and its own screens.

## API contract (summary)

- **Farms / fields / zones**: `GET|POST /api/farms`, `GET|PUT|DELETE /api/farms/{id}`,
  `GET /api/farms/{id}/fields`, `GET|POST /api/fields`, `GET|PUT|DELETE /api/fields/{id}`,
  `GET /api/fields/{id}/zones`, `GET|POST /api/zones`, `GET|PUT|DELETE /api/zones/{id}`.
  A zone carries its irrigation method, its crop coefficient (Kc) and, for
  drip irrigation, the number of emitters and their nominal flow rate.
- **Water sources**: `GET|POST /api/water/sources`, `GET|PUT|DELETE /api/water/sources/{id}`,
  `GET /api/water/sources/{id}/level` (capacity, level, percentage, status, rainwater tank).
- **Consumption**: `GET|POST /api/water/consumption`, `GET|PUT|DELETE /api/water/consumption/{id}`
  (array response, or a Page as soon as `page`/`size` is provided).
- **Quality**: `GET|POST /api/water/quality`, `GET|PUT|DELETE /api/water/quality/{id}`.
- **Irrigation**: `GET|POST /api/irrigations`, `GET|PUT|DELETE /api/irrigations/{id}`,
  `POST /api/irrigations/{id}/start|stop`, `POST /api/irrigations/{id}/postpone`,
  `POST /api/irrigations/auto-trigger`, `GET /api/irrigation/suggestions`,
  `GET|POST /api/irrigation-logs`, `GET|PUT|DELETE /api/irrigation-logs/{id}`.
- **Drip clogging**: `GET /api/water/zones/{zoneId}/flow-check` (flow diagnostic of the zone).
- **Rainwater**: `GET|POST /api/rainwater-harvests`, `GET|PUT|DELETE /api/rainwater-harvests/{id}`,
  `GET /api/rainwater-harvests/coverage`.
- **Drip maintenance**: `GET|POST /api/drip-maintenance-logs`,
  `GET|PUT|DELETE /api/drip-maintenance-logs/{id}`, `GET /api/drip-maintenance-logs/schedule`
  (preventive calendar computed per zone).
- **Quotas**: `GET|POST /api/water/quotas`, `GET /api/water/quotas/usage`, `GET|PUT|DELETE .../{id}`.
- **Dashboard**: `GET /api/dashboard/kpis|activities|alerts|water-savings|savings-series|water-balance|leaks`.
- **AI**: `GET /api/ai/recommendations`, `GET /api/ai/drought-prediction`, `POST /api/ai/analyze`.
- **Weather**: `GET /api/weather/current`, `GET /api/weather/forecast` (optional coordinates).
- **Notifications**: `GET|POST /api/notifications`, `GET /api/notifications/{id}`,
  `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`, `DELETE /api/notifications/{id}`.
- **Reports**: `GET /api/reports/consumption|irrigation|quality` and `.../csv`.
- **Health**: `GET /api/health` (really checks the database, 503 when it is down).

## Business rules (values are spelled out in the code)

| Rule | Reference |
|---|---|
| Water need of a zone | `need = area x ET0 x Kc / efficiency` (FAO-56) — `WaterNeedService` |
| System efficiency | drip 0.90 · sprinkler 0.75 · gravity 0.60 |
| Automatic control by soil moisture | seasonal thresholds **40 / 45 / 50 / 55%** (rain, transition, fresh dry, hot dry) + guard "last watering > 24 h" — `IrrigationAutomationService` |
| Irrigation postponement because of rain | probability >= 60% and rain >= 5 mm — `IrrigationService` |
| Monthly quota | alert at 80% then 100% — `WaterQuotaService` (evaluation throttled to 1x/60 s) |
| Drip clogging / leak | measured volume < 90% (clogging) or > 110% (leak) of the expected volume — `DripFlowCheckService` |
| Rainwater tank | >= 95% likely overflow, >= 50% use the rain first — `RainwaterTankMonitor` |
| Recurring alert | same title unread and less than 12 h apart: no duplicate — `AlertService` |

## Configuration (environment variables)

| Property | Default | Role |
|---|---|---|
| `app.weather.latitude` / `longitude` | 10.63 / -4.77 (Banfora) | Only source of the agro-weather coordinates (ET0, rain) |
| `app.irrigation.auto-trigger-enabled` | `true` | Enables the automatic control job |
| `app.irrigation.auto-trigger-interval-ms` | 900000 (15 min) | Period of the job |
| `app.quota.check-throttle-seconds` | 60 | Minimum window between two quota evaluations (0 = disabled) |
| `app.rainwater.weekly-nonirrigation-need-liters` | 0 | Declared non-irrigation need: without it the "rain first" recommendation stays silent |
| `app.cors.allowed-origins` | localhost:5173 | Allowed origins (PATCH included) |

## IoT ingestion contract

`POST /api/iot/telemetry` (single object or batch, 202, status per measurement) — types
`level`, `flow`, `quality`, `rain`, `clogging`, `soil`. Two things to know:

- **`flow` can carry `zone_id`**: this link is what allows comparing the measured volume to the
  expected volume of the network (without it, `flow-check` answers `no_measurement`).
- **`level`** updates the source level and triggers the rainwater tank rules, on top of the
  real-time level read by `/api/water/sources/{id}/level`.

## Tests

```bash
cd backend && ./mvnw test     # 88 tests (including one HTTP integration test) - PostgreSQL required
cd frontend && npm test       # 38 tests (views, hooks, wiring, menu)
cd frontend && npm run lint && npm run build
```

The module's tests declare their own DOM environment (`// @vitest-environment jsdom`):
the project has no global Vitest configuration, and the view tests provide a brand-new
React Query client (`components/views/testRender.jsx`).

## Integrating this module into another one (or the reverse)

1. Shareable data is exposed read-only through dedicated endpoints
   (`/api/water/sources/{id}/level`, `/api/water/zones/{id}/flow-check`, `/api/dashboard/water-balance`)
   rather than through direct table access.
2. API responses are DTOs (`dto/`), never JPA entities.
3. The schema only evolves through versioned migrations: **announce the number** before writing
   the next one (folder shared between modules).
4. The module never modifies another module's files nor `frontend/src/shared/`.

## Open decisions (to settle with the team)

- **Authentication**: out of the module's scope (no `/api/auth` endpoint), it will come from the
  global platform. Automatic alerts are attached to a technical account
  (`SystemUsers.IOT_SYSTEM_USER_ID`, with no usable password).
- **Migration numbers**: `V10` (automatic control + flow per zone) is to be announced.
- **`shared/hooks/useAuth.js`**: dead code outside the module importing `getToken`/`setToken` from
  `shared/api/client.js` — those functions do not exist; to fix in coordination or remove.

