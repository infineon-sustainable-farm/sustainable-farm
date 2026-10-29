# Water Supply for Product Transformation

Your module has no code yet, so this page doubles as the contract to build against. Everything
below is ready and tested, and updated with your answers of 2026-10-29. The coefficients and the
quality rules stay provisional and configurable until both teams confirm them.

Flow meters know nothing about lots. The model is therefore: the meter records consumptions as
usual, then **you tell us which time window belongs to which lot**, and we attribute the
consumptions of that window to the lot.

## 1. Lot identifier

`lotId` is any stable text of at most 128 characters: Water Supply does not interpret it. Your
`LOT-2026-0142` format works, and so would your `batchId` as is. The choice is yours (see "Still
open"); the only requirement is that one lot always keeps the same identifier.

Note that Plants now gives a lot number to each harvest, in the form `LOT-<year>-<block>-<sequence>`
(for example `LOT-2026-A-002`). If your lots are not the harvest lots, pick a format that cannot be
mistaken for theirs.

## 2. Activities

Send exactly these codes, in English (any case): `WASHING`, `CUTTING`, `DRYER_CLEANING`, `OTHER`.
Translated values such as `LAVAGE` are refused (`400 INVALID_ARGUMENT`).

- Your washing operation is `WASHING`; cutting is `CUTTING`.
- Your sorting and drying operations have no code of their own: send `OTHER` until we agree on new
  codes. Cleaning the dryer, once you model it, is `DRYER_CLEANING`.
- `IRRIGATION` also exists, but it stays with Water Supply: you never send it.

## 3. Attach a lot to a consumption window

`POST /api/water/lot-usage`

```json
{ "lotId": "LOT-2026-0142", "activity": "WASHING",
  "startAt": "2026-10-13T08:00:00Z", "endAt": "2026-10-13T10:00:00Z",
  "sourceId": "<washing tank id>", "zoneId": null }
```

```json
{ "lotId": "LOT-2026-0142", "activity": "WASHING", "startAt": "…", "endAt": "…",
  "attachedCount": 2, "attachedLiters": 150.0, "skippedOtherLotCount": 0, "warnings": [] }
```

- This is the whole payload to implement. The window is `[startAt, endAt[`, as ISO instants (the
  site's local time is UTC+0). `lotId`, `startAt` and `endAt` are required.
- `sourceId` is the id of the tank used, as listed by `GET /api/water/sources`. Please always send
  it: without it every meter of the site is in the window (warning `no_source_or_zone_filter`).
- A consumption that already belongs to **another** lot is never taken over: it is counted in
  `skippedOtherLotCount`, with the warning `window_overlaps_other_lot`. The other warning is
  `no_consumption_found`.
- Sending the same window again is harmless.

If you already know the volume, you can create the consumption directly:
`POST /api/water/consumption` with `"lotId"` and `"activity"`. Both fields are also accepted by
`PUT /api/water/consumption/{id}`, and you can filter with `GET /api/water/consumption?lotId=…`.

## 4. Water consumed by a lot

`GET /api/water/consumption/by-lot?lotId=LOT-2026-0142`

```json
{ "lotId": "LOT-2026-0142", "totalLiters": 270.0, "byActivity": { "WASHING": 250.0, "OTHER": 20.0 },
  "firstConsumptionAt": "…", "lastConsumptionAt": "…",
  "items": [ { "consumptionId": "…", "activity": "WASHING", "sourceId": "…", "zoneId": null,
               "liters": 250.0, "consumptionDate": "…" } ] }
```

An unknown lot answers `200` with `totalLiters: 0`.

## 5. Washing water quality for a lot

`GET /api/water/quality/compliance?lotId=LOT-2026-0142[&from=…&to=…]`

```json
{ "lotId": "LOT-2026-0142", "from": "…", "to": "…", "validityDays": 7, "sourceIds": ["…"],
  "status": "compliant", "compliant": true,
  "rule": { "phMin": 6.0, "phMax": 7.5, "turbidityMaxNtu": 5.0 },
  "sources": [ { "sourceId": "…", "status": "compliant", "testCount": 1, "lastTestDate": "…" } ],
  "tests": [ { "qualityTestId": "…", "sourceId": "…", "ph": 7.0, "turbidityNtu": 1.0, "status": "compliant", "testDate": "…" } ] }
```

Answers to your two questions:

- **Validity period: 7 days** (provisional, `WATER_QUALITY_TEST_VALIDITY_DAYS`). For each source, the
  decisive tests are the last one made in the 7 days before the lot, plus every test made during the
  lot. An older failed test that was followed by a passing one no longer counts.
- **A change of source needs a new test, whatever the time elapsed.** Each source that supplied the
  lot is judged on its own tests (`sources`). One source without a valid test makes the whole lot
  `no_test`.

`status` is `compliant`, `non_compliant` (a decisive test is out of range) or **`no_test`**, and with
`no_test`, `compliant` is `false`: having no test proves nothing. If you leave out `from` and `to`,
the window is the lot's first-to-last consumption. Without `lotId`, `from` and `to` are required and
every source that has a test is checked.

## 6. Availability forecast and buffer alert

`POST /api/water/product-transformation/availability-forecast`

```json
{ "sourceId": "…", "horizonDays": 7, "includeIrrigation": true,
  "batches": [ { "lotId": "LOT-2026-0150", "activity": "WASHING", "plannedAt": "2026-10-15T08:00:00Z", "sizeKg": 1100 } ] }
```

```json
{ "sourceId": "…", "horizonDays": 7, "currentLevelLiters": 2500.0,
  "batchDemandLiters": 2200, "irrigationDemandLiters": 0, "forecastDemandLiters": 2200,
  "availableAfterForecastLiters": 300, "bufferMinLiters": 500.0, "bufferStatus": "tight",
  "batches": [ { "lotId": "LOT-2026-0150", "activity": "WASHING", "plannedAt": "…", "demandLiters": 2200, "inHorizon": true } ],
  "warnings": [], "parametersStatus": "unvalidated_defaults" }
```

- `horizonDays` defaults to 7 and accepts 1 to 90. Batches planned outside the horizon count for 0.
  A batch without `plannedAt` is counted.
- When `includeIrrigation` is `true` (the default), the scheduled irrigations **of the source's farm**
  over the horizon are added.
- `bufferStatus` is `ok`, `tight` (below the minimum buffer) or `insufficient` (below zero). When it
  is not `ok`, the water team receives a warning notification.

## Provisional values

As agreed, these stay configurable defaults until both teams confirm them:

| Value | Default | Environment variable |
|---|---|---|
| Washing | 2.0 L per kg | `WATER_PT_WASHING_LITERS_PER_KG` |
| Cutting | 0.5 L per kg (already provided) | `WATER_PT_CUTTING_LITERS_PER_KG` |
| Dryer cleaning | 1.0 m³ per lot | `WATER_PT_DRYER_CLEANING_M3_PER_LOT` |
| Minimum buffer | 500 L | `WATER_PT_BUFFER_MIN_LITERS` |
| Compliant pH | 6.0 to 7.5 | `WATER_QUALITY_PH_MIN`, `WATER_QUALITY_PH_MAX` |
| Maximum turbidity | 5.0 NTU | `WATER_QUALITY_TURBIDITY_MAX_NTU` |
| Validity of a quality test | 7 days | `WATER_QUALITY_TEST_VALIDITY_DAYS` |

No other coefficient is needed for the initial integration. `OTHER` batches (sorting, drying) count
for 0 L and add a `no_coefficient_for_activity_other` warning.

## Running the backend and the tests locally

- **Database:** from the repository root, `docker compose up -d postgres` starts PostgreSQL 17 with
  the credentials of `.env` (copy `.env.example`). Then start the backend with
  `DB_URL=jdbc:postgresql://localhost:5432/sustainable_farm`, `DB_USERNAME=farm_admin` and
  `DB_PASSWORD=changeme` (the values of `.env`).
- **Tests do not need it:** `cd backend && ./mvnw test` runs on an in-memory H2 database by default.
  The CI runs the same tests on PostgreSQL.
- **Known blocker on a fresh database:** the backend then stops with
  `Schema validation: missing table [...]`, because several modules have no Flyway migration yet (list
  in the [index](README.md#known-platform-issue-all-module-owners)). Until those migrations exist,
  add `SPRING_JPA_HIBERNATE_DDL_AUTO=update` on your machine only, never in production.

## What the water team sees

The **Processing lots** screen (`/watersupply/lots`) shows the same lookups, the attribution form
and the forecast. The water team can fix an attribution while your module is being built.

## How to call us

Use HTTP, or inject `ProductTransformationIntegrationService` in the same JVM (`attachLotUsage`,
`byLot`, `qualityCompliance`, `forecast`).

## Decisions recorded (2026-10-29)

| Topic | Decision |
|---|---|
| Activities | The list above is the initial base; irrigation stays with Water Supply |
| Payload | `POST /api/water/lot-usage` as in section 3, with the tank's `sourceId` |
| Coefficients and quality thresholds | Provisional, configurable defaults |
| Quality test validity | 7 days, one valid test per source used (provisional) |

## Still open

1. **Lot identifier:** `LOT-2026-0142` as a new identifier, or your `batchId` directly? Both work on
   our side.
2. **Sorting and drying:** keep them as `OTHER`, or add their own codes?
3. **Confirmation of the provisional values** above, including the 7-day validity, once your
   process is modelled.
