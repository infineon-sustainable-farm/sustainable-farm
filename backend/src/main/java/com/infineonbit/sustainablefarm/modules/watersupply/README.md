# Module water supply (SWMS)

Intelligent water management for the farm: irrigation driven by the crops' actual
needs, tracking of sources and consumption, water quality, drought alerts.
It is the reference module of the program: the other modules (machinery, plants)
share the same structure and the same conventions.

## Where the code lives

| Part | Location |
|---|---|
| REST API, services, entities | `backend/src/main/java/com/infineonbit/sustainablefarm/modules/watersupply/` |
| Database schema (migrations V1 -> V12) | `backend/src/main/resources/db/migration/` |
| Interface (views, hooks, API client) | `frontend/src/features/watersupply/` |
| Sensors and ESP32 firmware | `iot/` |

The module is mounted under `/watersupply` in the application (`routes.jsx`), like
`/machinery` and `/plants`: each module carries its own menu and its own screens.

## Integration

Water Supply serves Plants, Visitor Management, Product Transformation and IoT. The contracts,
examples and open questions for each team are in [`docs/integrations/`](../../../../../../../../../docs/integrations/README.md).
The source of the needs is the [integration audit](../../../../../../../../../docs/watersupply-integration-audit.md).

### Progress

| ID | Task | Module | Status |
|---|---|---|---|
| 0.0 | Integration plan and progress tracking | Water Supply | done |
| 1.1 | Normalize Plants zone-to-block codes | Plants | done |
| 1.2 | Log swallowed alert failures | Water Supply | done |
| 1.3 | Externalize quality and rain defaults | Water Supply | done |
| 1.4 | Use direct zone consumption for quotas | Water Supply | done |
| 2.1 | Expose and filter irrigation/consumption by block | Plants | done |
| 2.2 | Expose rainfall by block | Plants | done |
| 2.3 | Calculate block irrigation need from Plants calendar | Plants | done (phase factors 1.0 until validated) |
| 2.4 | Use adjusted block need in auto-trigger | Plants | done |
| 3.1 | Expose visitor irrigation status | Visitor Management | done |
| 3.2 | Expose visitor live consumption | Visitor Management | done |
| 3.3 | Manage planned water outages | Visitor Management | done |
| 3.4 | Register idempotent visitor events | Visitor Management | done |
| 3.5 | Manage visitor safety briefing | Visitor Management | done (owner to confirm) |
| 3.6 | Expose visitor water summary | Visitor Management | done |
| 4.1 | Add lot and activity to consumption | Product Transformation | done |
| 4.2 | Attach a consumption window to a lot | Product Transformation | done |
| 4.3 | Expose consumption by lot | Product Transformation | done |
| 4.4 | Expose quality compliance for a lot/window | Product Transformation | done |
| 4.5 | Forecast Product Transformation availability | Product Transformation | done (coefficients to validate) |
| 5.1 | Factor recent rainfall into auto-trigger | Water Supply | done |
| 5.2 | Store and expose level history | Water Supply | done |
| 5.3 | Add forecast demand to water balance | Water Supply | done |
| 6.1 | Record and dispatch valve commands | IoT | done, gateway not configured (503) |
| 7.1 | Run Java 21 suite and finalize documentation | Water Supply | done |

### Endpoints added

| Area | Endpoints |
|---|---|
| Zone to block (Plants) | `GET|POST /api/water/zone-block-mappings`, `GET|PUT|DELETE /api/water/zone-block-mappings/{id}`; `blockCode` filter on `GET /api/irrigations` and `GET /api/water/consumption` |
| Plants | `GET /api/water/plants/rainfall`, `POST /api/water/plants/irrigation-need` |
| Visitor Management | `GET /api/water/visitor/irrigation-status`, `/visitor/live-consumption`, `/visitor/summary`, `GET|PATCH /api/water/visitor/safety-briefing`, `POST /api/water/visitor-events` |
| Planned unavailability | `GET|POST /api/water/outages`, `GET|PUT|DELETE /api/water/outages/{id}` |
| Product Transformation | `lotId`/`activity` on consumption, `POST /api/water/lot-usage`, `GET /api/water/consumption/by-lot`, `GET /api/water/quality/compliance`, `POST /api/water/product-transformation/availability-forecast` |
| Internal | `GET /api/water/sources/{id}/level-history`; `demand_forecast_liters` / `available_after_forecast_liters` in `GET /api/dashboard/water-balance` |
| IoT | `POST|GET /api/iot/devices/{deviceId}/commands` |

### Screens (frontend `features/watersupply`)

| Screen | Path | What the water team does there |
|---|---|---|
| Planned outages | `/watersupply/outages` | Plans, edits, cancels a maintenance or a cut (zone, source or farm), optionally postponing the irrigations of the period |
| Plants blocks | `/watersupply/plants-blocks` | Links zones to Plants blocks; sees per block the month's rainfall and today's irrigation need |
| Processing lots | `/watersupply/lots` | Looks up a lot (water per activity, quality verdict), attributes a metering window to a lot, forecasts a tank for planned batches |
| Visitors | `/watersupply/visitors` | Publishes the safety briefing, previews the visitor screen data, drives the demo valve and reads its journal |

Existing screens also show the new data: Plants block on irrigation schedules, block and lot on
consumption measurements, level history of a source (*History* button), water balance with the
7-day irrigation forecast on the dashboard, and the location of farms and fields (below).

### Farm and field location (to be reviewed)

When a farm or a field is **created**, its position is requested from the device (browser
Geolocation API, `hooks/useGeolocation.js`) and filled in; it can be corrected by hand or taken
again with *Use my position*. Editing never relocates. Farms store `latitude`/`longitude`, fields
store `coordinates` as `"lat, lng"` text. Edits now always send the farm position: the API
replaces it on every update, so an edit used to erase it.

This is a first version, **to be reviewed soon**:

- the position is the device's: accurate on a phone outdoors, a few kilometres off on a desktop
  (Wi-Fi/IP). The accuracy is shown, but nothing forces the person to be on site;
- browsers only allow it over HTTPS (or `localhost`) and after the user's permission;
- a field is a surface: a map picker or a boundary (polygon) would describe it better than a point,
  and `coordinates` is free text today;
- the weather (ET0, rain) still uses the site coordinates `app.weather.*`, not each farm's position.

### Design notes

- **Planned unavailability** (`WaterUnavailabilityService`) is shared: Visitor Management reads it,
  a valve opening and an automatic irrigation are refused during it, and a farm-wide period applies
  to every zone of the farm.
- **Automatic irrigation** additionally skips a zone under planned unavailability, skips a zone
  whose farm gauges measured the recent-rain threshold, and multiplies the volume by the Plants growth
  phase factor of the zone's block.
- **Rainfall** of a farm is the highest cumulative value among its gauges, never their sum (two
  tanks record the same rain).
- **Plants** is read in a single class, `PlantsGrowthCalendarAdapter`, behind `PlantsGrowthPort`.
- **Valve commands** go through `ValveCommandPort`. Its only implementation refuses every command
  until an MQTT/HTTP gateway is chosen. The attempt is journaled anyway (`noRollbackFor`).
- Optional filters use Criteria (`ConsumptionCriteria`, unavailability search) instead of JPQL
  `(:p is null or ...)`, which PostgreSQL rejects (it cannot type the parameter).

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
| `app.water.quality.ph-min` / `ph-max` / `turbidity-max-ntu` | 6.0 / 7.5 / 5.0 | Quality alert and lot compliance thresholds |
| `app.water.rain.default-catchment-area-m2` / `default-runoff-coefficient` | 180 / 0.8 | Used when `rain` telemetry omits them |
| `app.water.irrigation.recent-rain-skip-threshold-mm` / `recent-rain-window-hours` | 5.0 / 24 | Automatic irrigation skips a zone whose farm gauges measured this much rain |
| `app.water.visitor.live-window-minutes` / `safety-default-max-capacity` | 15 / 10 | Visitor live counter window, default briefing capacity |
| `app.water.pt.*` | 2.0 L/kg washing, 0.5 L/kg cutting, 1 m3/lot dryer, 500 L buffer | Product Transformation forecast (to validate) |
| `app.water.plants.phase-factor.*` | 1.0 | Need multiplier per Plants growth phase (to validate) |
| `app.water.iot.valve-max-duration-seconds` / `offline-threshold-minutes` | 30 / 15 | Valve opening cap, device offline delay |

Every `app.water.*` value has an environment variable (see `application.properties`); the
integration values are working defaults listed as open questions in `docs/integrations/`.

## IoT ingestion contract

`POST /api/iot/telemetry` (single object or batch, 202, status per measurement) — types
`level`, `flow`, `quality`, `rain`, `clogging`, `soil`. Two things to know:

- **`flow` can carry `zone_id`**: this link is what allows comparing the measured volume to the
  expected volume of the network (without it, `flow-check` answers `no_measurement`).
- **`level`** updates the source level and triggers the rainwater tank rules, on top of the
  real-time level read by `/api/water/sources/{id}/level`.

## Tests

```bash
cd backend && ./mvnw test     # whole backend: 664 tests, of which 168 for watersupply
cd frontend && npm test       # 79 tests, of which 72 for watersupply (views, hooks, wiring, menu)
cd frontend && npm run lint && npm run build
```

Watersupply tests: unit tests per service (Mockito) and three HTTP integration classes on the full
context — `WatersupplyApiIntegrationTest`, `WatersupplyIntegrationsApiTest` (every integration
endpoint, weather and Plants calendar mocked) and `IotTelemetryIntegrationTest`. The defaults run on
H2; the CI overrides the datasource to PostgreSQL, and the suite passes on both.

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
- **Migration numbers**: `V11` (zone/block mapping) and `V12` (integrations) belong to watersupply;
  the next free number is `V13`.
- **Fresh PostgreSQL startup**: V1-V12 apply, but `ddl-auto=validate` then fails on tables of other
  modules that have no migration (list in `docs/integrations/README.md`).
- **Farm and field location**: first version from the device geolocation, to be reviewed (see
  "Farm and field location" above).
- **Integration answers** expected from Plants, Visitor Management, Product Transformation, IoT and
  Site Security: see the "What we need from you" section of each page in `docs/integrations/`.
- **`shared/hooks/useAuth.js`**: dead code outside the module importing `getToken`/`setToken` from
  `shared/api/client.js` — those functions do not exist; to fix in coordination or remove.

