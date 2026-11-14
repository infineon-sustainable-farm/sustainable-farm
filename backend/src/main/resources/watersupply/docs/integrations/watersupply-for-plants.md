# Water Supply for Plants

Plants works with **blocks** (`blockCode`, for example `A` or `B2`). Water Supply works with
**zones** (`zoneId`, a UUID), and a zone is the area served by one irrigation line. The table
`zone_block_mapping` links the two. Every block feature below goes through it.

Status: ready and tested, updated with your answers of 2026-10-29. The growth phase factors stay at
1.0 until they are validated (see "Still open").

## 1. Map zones to blocks

Each Water Supply zone is mapped to the Plants blocks it waters. The water team manages these links
in its **Plants blocks** screen (`/watersupply/plants-blocks`), which also shows what you read per
block (sections 3 and 4).

| Method | Path | Body / parameters |
|---|---|---|
| `GET` | `/api/water/zone-block-mappings` | optional `zoneId`, `blockCode`, `farmId` |
| `POST` | `/api/water/zone-block-mappings` | `{"zoneId","blockCode","farmId"}` → 201 |
| `GET` / `PUT` / `DELETE` | `/api/water/zone-block-mappings/{mappingId}` | `PUT` accepts any subset of the three fields |

```json
{ "id": "5b1e…", "zoneId": "2f6c…", "blockCode": "A", "farmId": "9d0a…", "createdAt": "2026-10-22T08:00:00Z" }
```

The rules follow yours:

- **Block codes** are 1 to 10 letters or digits, trimmed and stored in upper case: `b2` becomes
  `B2`, and `Block B` is refused (`400 INVALID_ARGUMENT`), exactly as in Plants.
- **A zone may cover several blocks, but a block belongs to only one zone** of its farm. Linking a
  block that is already on another zone returns `409 CONFLICT`, and the message says so. Migration
  V13 kept only the oldest link for any block that had been linked to two zones.
- The `farmId` must be the farm that owns the zone, otherwise the API answers `400 INVALID_ARGUMENT`.
- A link is deleted together with its zone.

## 2. Irrigation and consumption per block

The existing lists accept a `blockCode` filter, which you can combine with `farmId`, `zoneId`,
`page` and `size`. The filter runs in the database, so `totalElements` counts the matching rows.

- `GET /api/irrigations?blockCode=A&page=0&size=20`
- `GET /api/water/consumption?blockCode=A&page=0&size=20`

Each schedule and each consumption carries two extra fields:

```json
{ "zoneId": "2f6c…", "blockCode": "A", "multipleBlockCodes": false }
```

`blockCode` is set when the zone covers exactly one block. When a zone covers several blocks,
`blockCode` is `null` and `multipleBlockCodes` is `true`: the volume of that zone cannot be split
between its blocks, and Water Supply does not guess.

## 3. Rainfall per block (to fill `localRainfallMm`)

`GET /api/water/plants/rainfall?blockCode=A&from=2026-10-01&to=2026-10-31`

`from` and `to` are days, both included, in UTC.

```json
{
  "blockCode": "A",
  "from": "2026-10-01",
  "to": "2026-10-31",
  "rainfallMm": 15.0,
  "source": "rain_gauge",
  "spatialResolution": "farm",
  "aggregation": "max_cumulative_per_gauge",
  "farmIds": ["9d0a…"],
  "gaugeCount": 2,
  "readings": [ { "sourceId": "…", "captureDate": "2026-10-05T06:00:00Z", "rainfallMm": 12.0 } ]
}
```

- The value is the rainfall of **the farm that owns the block**, which you confirmed is enough for a
  2-hectare farm.
- Two tanks on the same farm record the same rain, so the readings are not summed across gauges.
  `rainfallMm` is the highest cumulative total among the farm's gauges.
- An unmapped block returns `404`.

## 4. Irrigation need per block

`POST /api/water/plants/irrigation-need`

```json
{ "blockCode": "A", "treeCount": 120, "growthPhase": "gradual production", "ageYears": 4, "date": "2026-10-13" }
```

Only `blockCode` is required. If you leave out `growthPhase`, Water Supply reads it from your growth
calendar. `date` defaults to today (UTC).

```json
{
  "blockCode": "A",
  "date": "2026-10-13",
  "et0Mm": 5.0,
  "baseNeedLiters": 93750,
  "growthPhase": "gradual production",
  "growthPhaseSource": "request",
  "phaseFactor": 1.0,
  "needLiters": 93750,
  "treeCount": 120,
  "ageYears": 4,
  "method": "fao56_zone_need_x_growth_phase_factor",
  "matchedZones": ["2f6c…"],
  "warnings": ["phase_factors_not_validated"]
}
```

- Base need = FAO-56 need of the zone: area × ET0 × Kc / irrigation efficiency.
- `needLiters` = base need × the factor of the growth phase. The phases are yours (establishment,
  gradual production, full production) and are read from the growth calendar; the factors are
  Water Supply parameters, kept at 1.0 until validated.
- `growthPhaseSource` is `request`, `plants_calendar` or `unknown`. It is `unknown` when the calendar
  has no entry, or entries in **different** phases for the same block code, and then no factor is
  applied.
- `treeCount` is the right driver for a per-tree need, as you confirmed. It will be read from
  `GET /api/plants/varieties` (per block and variety) once the litres per tree and per day are
  validated. Until then `treeCount` and `ageYears` are returned unchanged and do not change the result.
- The "20 m³ per day per tree" of the Zalka 2025 file is **not** used: as you pointed out, it looks
  like the need of the whole orchard.

The **automatic irrigation** applies the same factor. When a zone covers exactly one block, the
planned volume is multiplied by the factor of that block's phase, and the decision report shows
`growth_phase` and `phase_factor`.

## 5. What Water Supply reads from Plants

`PlantsGrowthCalendarAdapter` calls `GrowthCalendarService.getAllGrowthCalendarEntries(null, blockCode)`
in the same JVM, without `farmId` as you suggested (single farm). It reads three fields of
`GrowthCalendarResponse`: `blockCode`, `growthPhase` and `ageYears`. Please tell us before you rename
these fields, rename the phase values, or change that method's signature. If the call fails,
irrigation carries on without the factor and the error is logged.

## Decisions recorded (2026-10-29)

| Topic | Decision |
|---|---|
| Farm identifier | Plants is called without `farmId`. With several farms, one identifier shared by the whole team, not a mapping table |
| Block codes | 1 to 10 letters or digits, upper case, no prefix, same rule on both sides |
| Cardinality | A zone may cover several blocks; a block belongs to only one zone |
| Phase factors | Water Supply parameters, 1.0 until validated |
| Per-tree need | `treeCount` from `GET /api/plants/varieties`; litres per tree and per day to be validated |
| Rainfall | Farm-level value is enough |

## Still open

1. **Validation of the factors and of the litres per tree and per day.** There is no agronomy team:
   we will ask the mentors who validates them. The values are set through
   `WATER_PLANTS_PHASE_FACTOR_ESTABLISHMENT`, `WATER_PLANTS_PHASE_FACTOR_GRADUAL_PRODUCTION` and
   `WATER_PLANTS_PHASE_FACTOR_FULL_PRODUCTION`.
2. **Shared farm identifier**, if a second farm comes: to be raised with Jean-Louis.
