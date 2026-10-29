# Water Supply for Plants

Plants works with **blocks** (`blockCode`, for example `A` or `B2`). Water Supply works with
**zones** (`zoneId`, a UUID), and a zone is the area served by one irrigation line. The table
`zone_block_mapping` links the two. Every block feature below goes through it.

Status: ready and tested. The growth phase factors stay at 1.0 until you validate them (see
"What we need from you").

## 1. Map zones to blocks

Each Water Supply zone has to be mapped to the Plants block it waters, once. Block codes are stored
in upper case (`a` becomes `A`).

| Method | Path | Body / parameters |
|---|---|---|
| `GET` | `/api/water/zone-block-mappings` | optional `zoneId`, `blockCode`, `farmId` |
| `POST` | `/api/water/zone-block-mappings` | `{"zoneId","blockCode","farmId"}` → 201 |
| `GET` / `PUT` / `DELETE` | `/api/water/zone-block-mappings/{mappingId}` | `PUT` accepts any subset of the three fields |

```json
{ "id": "5b1e…", "zoneId": "2f6c…", "blockCode": "A", "farmId": "9d0a…", "createdAt": "2026-10-22T08:00:00Z" }
```

On the Water Supply side, the water team manages these links in the **Plants blocks** screen
(`/watersupply/plants-blocks`), which also shows what you read per block (sections 3 and 4).

The `farmId` must be the farm that owns the zone, otherwise the API answers `400 INVALID_ARGUMENT`.
The same `(zoneId, blockCode)` pair cannot be mapped twice (`409`). A zone may carry several blocks.
A mapping is deleted together with its zone.

## 2. Irrigation and consumption per block

The existing lists accept a `blockCode` filter, which you can combine with `farmId`, `zoneId`,
`page` and `size`. The filter runs in the database, so `totalElements` counts the matching rows.

- `GET /api/irrigations?blockCode=A&farmId=…&page=0&size=20`
- `GET /api/water/consumption?blockCode=A&farmId=…&page=0&size=20`

Each schedule and each consumption now carries two extra fields:

```json
{ "zoneId": "2f6c…", "blockCode": "A", "multipleBlockCodes": false }
```

`blockCode` is set only when the zone maps to exactly one block. When a zone covers several blocks,
`blockCode` is `null` and `multipleBlockCodes` is `true`. In that case the volume cannot be split
between the blocks, and Water Supply does not guess.

## 3. Rainfall per block (to fill `localRainfallMm`)

`GET /api/water/plants/rainfall?blockCode=A&from=2026-10-01&to=2026-10-31[&farmId=…]`

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

- The rain gauges are attached to water sources, which belong to a farm. The value is therefore the
  rainfall of **the farm that owns the block**, not a measurement inside the block.
- Two tanks on the same farm record the same rain, so the readings are not summed across gauges.
  `rainfallMm` is the highest cumulative total among the farm's gauges.
- An unmapped block returns `404`.

## 4. Irrigation need per block

`POST /api/water/plants/irrigation-need`

```json
{ "blockCode": "A", "farmId": null, "treeCount": 120, "growthPhase": "gradual production", "ageYears": 4, "date": "2026-10-13" }
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

- Base need = FAO-56 need of the mapped zones: area × ET0 × Kc / irrigation efficiency.
- `needLiters` = base need × the factor of the growth phase. Accepted phases are `establishment`,
  `gradual production` and `full production`. Underscores and dashes are accepted too, and any
  other value returns `400`.
- `growthPhaseSource` is `request`, `plants_calendar` or `unknown`. It is `unknown` when the
  calendar has no entry, or has entries in **different** phases for the same block code, and in
  that case no factor is applied.
- `treeCount` and `ageYears` are returned unchanged. They do not affect the result until a per-tree
  coefficient is validated.

The **automatic irrigation** applies the same factor. When a zone maps to exactly one block, the
planned volume is multiplied by the factor of that block's phase, and the decision report shows
`growth_phase` and `phase_factor`.

## 5. What Water Supply reads from Plants

`PlantsGrowthCalendarAdapter` calls `GrowthCalendarService.getAllGrowthCalendarEntries(null, blockCode)`
in the same JVM. It reads three fields of `GrowthCalendarResponse`: `blockCode`, `growthPhase` and
`ageYears`. Please tell us before you rename these fields, rename the phase values, or change that
method's signature. If the call fails, irrigation carries on without the factor and the error is
logged.

## What we need from you

1. **Farm identifiers.** Plants uses an `Integer` `farmId`, while Water Supply uses UUIDs. The adapter
   therefore looks up a block by code only, across every farm. Can we agree on a shared farm
   identifier, or on a mapping table?
2. **Block codes.** Can we always store them in upper case, or do you need block names to stay
   case-sensitive?
3. **Cardinality.** Is a zone one block, or can one zone water several blocks (and one block span
   several zones)?
4. **Phase factors.** What multiplier should apply for each phase? Defaults are 1.0, set through
   `WATER_PLANTS_PHASE_FACTOR_ESTABLISHMENT`, `WATER_PLANTS_PHASE_FACTOR_GRADUAL_PRODUCTION` and
   `WATER_PLANTS_PHASE_FACTOR_FULL_PRODUCTION`. Who in agronomy validates them?
5. **Per-tree need.** Should `treeCount` drive the need (litres per tree and per day by phase)? If
   so, please send validated values.
6. **Rainfall.** Is a farm-level value enough to fill `localRainfallMm`, or do you need a gauge per
   block?
