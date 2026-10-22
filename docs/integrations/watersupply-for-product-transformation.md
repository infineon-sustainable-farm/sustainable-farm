# Water Supply for Product Transformation

Your module has no code yet, so this page doubles as the contract to build against. Everything
below is ready and tested. The coefficients are working defaults that we need you to confirm.

Flow meters know nothing about lots. The model is therefore: the meter records consumptions as
usual, then **you tell us which time window belongs to which lot**, and we attribute the
consumptions of that window to the lot.

## 1. Activities

`WASHING`, `CUTTING`, `DRYER_CLEANING`, `IRRIGATION`, `OTHER`. Any case is accepted, and any other
value returns `400 INVALID_ARGUMENT`.

## 2. Attach a lot to a consumption window

`POST /api/water/lot-usage`

```json
{ "lotId": "LOT-2026-0142", "activity": "washing",
  "startAt": "2026-10-13T08:00:00Z", "endAt": "2026-10-13T10:00:00Z",
  "sourceId": "<washing tank>", "zoneId": null }
```

```json
{ "lotId": "LOT-2026-0142", "activity": "WASHING", "startAt": "…", "endAt": "…",
  "attachedCount": 2, "attachedLiters": 150.0, "skippedOtherLotCount": 0, "warnings": [] }
```

- The window is `[startAt, endAt[`. `lotId` (at most 128 characters), `startAt` and `endAt` are
  required.
- A consumption that already belongs to **another** lot is never taken over. It is counted in
  `skippedOtherLotCount` and flagged with the warning `window_overlaps_other_lot`.
- Other warnings are `no_consumption_found` and `no_source_or_zone_filter`. The second one means
  every meter of the site was in the window, so please always send the `sourceId`.
- Sending the same window again is harmless.

If you already know the volume, you can create the consumption directly:
`POST /api/water/consumption` with `"lotId"` and `"activity"`. Both fields are also accepted by
`PUT /api/water/consumption/{id}`, and you can filter with `GET /api/water/consumption?lotId=…`.

## 3. Water consumed by a lot

`GET /api/water/consumption/by-lot?lotId=LOT-2026-0142`

```json
{ "lotId": "LOT-2026-0142", "totalLiters": 270.0, "byActivity": { "WASHING": 250.0, "OTHER": 20.0 },
  "firstConsumptionAt": "…", "lastConsumptionAt": "…",
  "items": [ { "consumptionId": "…", "activity": "WASHING", "sourceId": "…", "zoneId": null,
               "liters": 250.0, "consumptionDate": "…" } ] }
```

An unknown lot answers `200` with `totalLiters: 0`.

## 4. Washing water quality for a lot

`GET /api/water/quality/compliance?lotId=LOT-2026-0142[&from=…&to=…]`

```json
{ "lotId": "LOT-2026-0142", "from": "…", "to": "…", "sourceIds": ["…"],
  "status": "compliant", "compliant": true,
  "rule": { "phMin": 6.0, "phMax": 7.5, "turbidityMaxNtu": 5.0 },
  "tests": [ { "qualityTestId": "…", "sourceId": "…", "ph": 7.0, "turbidityNtu": 1.0, "status": "compliant", "testDate": "…" } ] }
```

- Only the tests of the **sources that supplied the lot** are used. If you leave out `from` and `to`,
  the window is the lot's first-to-last consumption, both included.
- `status` is `compliant`, `non_compliant` (at least one test is out of range) or **`no_test`**.
  With `no_test`, `compliant` is `false`: having no test proves nothing.
- Without `lotId`, `from` and `to` are required and every source is checked.

## 5. Availability forecast and buffer alert

`POST /api/water/product-transformation/availability-forecast`

```json
{ "sourceId": "…", "horizonDays": 7, "includeIrrigation": true,
  "batches": [ { "lotId": "LOT-2026-0150", "activity": "washing", "plannedAt": "2026-10-15T08:00:00Z", "sizeKg": 1100 } ] }
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

## Working defaults to confirm

| Value | Default | Environment variable |
|---|---|---|
| Washing | 2.0 L per kg | `WATER_PT_WASHING_LITERS_PER_KG` |
| Cutting | 0.5 L per kg | `WATER_PT_CUTTING_LITERS_PER_KG` |
| Dryer cleaning | 1.0 m³ per lot | `WATER_PT_DRYER_CLEANING_M3_PER_LOT` |
| Minimum buffer | 500 L | `WATER_PT_BUFFER_MIN_LITERS` |
| Compliant pH | 6.0 to 7.5 | `WATER_QUALITY_PH_MIN`, `WATER_QUALITY_PH_MAX` |
| Maximum turbidity | 5.0 NTU | `WATER_QUALITY_TURBIDITY_MAX_NTU` |

`IRRIGATION` and `OTHER` batches have no coefficient. They count for 0 and add a
`no_coefficient_for_activity_*` warning.

## What the water team sees

The **Processing lots** screen (`/watersupply/lots`) shows the same lookups, the attribution form
and the forecast. The water team can fix an attribution while your module is being built.

## How to call us

Use HTTP, or inject `ProductTransformationIntegrationService` in the same JVM (`attachLotUsage`,
`byLot`, `qualityCompliance`, `forecast`).

## What we need from you

1. **Lot identifier.** What format will your stable `lotId` have?
2. **Activities.** Is the list in section 1 right? Is anything missing?
3. **Windows.** Can you send the start and end of each activity for each lot, together with the
   source (tank) it uses?
4. **Coefficients.** What are the real litres per kg or per lot for each activity, and what minimum
   buffer do you need?
5. **Quality rule.** Which standard applies to washing water, and how long does a quality test stay
   valid before a lot?
