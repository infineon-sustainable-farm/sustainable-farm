# Energy Supply Systems

API reference of the energysupply module, used by the screens under `/energy-supply`. Every
endpoint is under `/api/energy-supply`; errors use the common `ApiError` body of
`core.exception.CoreExceptionHandler` (see `docs/ARCHITECTURE.md`, section 2.8).

## Energy components

A component has a `Long` id in the database and a unique business code (`ENG-001`), which the API
calls `componentId` and uses in the paths.

| Method | Endpoint | Success |
| --- | --- | --- |
| `GET` | `/api/energy-supply/components` | `200` with all components |
| `GET` | `/api/energy-supply/components/{componentId}` | `200` with one component |
| `POST` | `/api/energy-supply/components` | `201`, response body and `Location` header |
| `PUT` | `/api/energy-supply/components/{componentId}` | `200` with the updated component |
| `DELETE` | `/api/energy-supply/components/{componentId}` | `204` |

Create and update requests use this shape:

```json
{
  "componentId": "ENG-006",
  "name": "Additional battery",
  "category": "BATTERY",
  "capacityValue": 10.0,
  "status": "OPERATIONAL"
}
```

`componentId` must match `ENG-` followed by three digits, `name` is required and limited to 255
characters, `category` and `status` are required enum values, and `capacityValue` must be zero or
greater when supplied. For updates, the code in the path must match the request body.

| Case | Status |
| --- | --- |
| Validation error, path and body codes that differ | `400` |
| Unknown component | `404` |
| Existing code on create (the existing component is never overwritten), delete of a component still referenced by an alert or a solar log | `409` |

## Other endpoints

| Area | Methods and endpoints |
| --- | --- |
| Dashboard | `GET /api/energy-supply/dashboard/summary` |
| Consumption | `GET /api/energy-supply/consumption`, `/today`, `/breakdown/{category}`, `/{logId}`; `POST`, `PUT /{logId}`, `DELETE /{logId}` |
| Solar generation | `GET /api/energy-supply/solar`, `/{logId}`, `/last-7-days`; `POST`, `PUT /{logId}`, `DELETE /{logId}` |
| Generator events | `GET /api/energy-supply/generator-events`, `/{eventId}`; `POST`, `PUT /{eventId}`, `DELETE /{eventId}` |
| Alerts | `GET /api/energy-supply/alerts`, `/open`; `PATCH /{alertId}/status` (alerts are stored in the shared `alert` table of the notification module) |
| Carbon metrics | `GET /api/energy-supply/carbon` |
| What-if analysis | `POST /api/energy-supply/whatif/simulate` |

## What-if analysis

`POST /api/energy-supply/whatif/simulate` takes `additionalPanels`, `additionalBatteryKwh` and
`productionScaleKgPerDay` and returns:

| Field | Meaning |
| --- | --- |
| `additionalCapexEur` | Cost of the added panels and battery capacity |
| `newPaybackYears` | Payback of that cost from the savings of the new panels; `null` when no panel is added |
| `newSystemCapacityKwp` | Current PV capacity plus the added panels |
| `additionalPvGenerationKwhPerYear` | Yearly output of the added panels |
| `additionalProductionDemandKwhPerYear` | Yearly dryer demand of the production scale-up, treated as additional daily output |
| `netAnnualEnergyBalanceKwh` | Added generation minus added production demand |
| `netAnnualCo2AvoidedKg` | CO2 avoided by the new panels minus the grid emissions of the added demand (negative: an increase) |

## Development data

With the Spring `dev` profile, the common demo data set (`seed/demo-data.sql`, loaded by
`core.config.DemoDataSeeder`) creates the components `ENG-001` to `ENG-005`, three weeks of solar logs, two weeks of
consumption, two generator events, carbon metrics and two alerts. The module has no seeder of its own.
