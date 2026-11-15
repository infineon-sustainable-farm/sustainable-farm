# Energy Supply Systems

Module 05 API reference for the React feature at `/energy-supply` and the
Spring Boot endpoints under `/api/energy`.

## Energy components

| Method | Endpoint | Success |
| --- | --- | --- |
| `GET` | `/api/energy/components` | `200` with all components |
| `GET` | `/api/energy/components/{componentId}` | `200` with one component |
| `POST` | `/api/energy/components` | `201`, response body and `Location` header |
| `PUT` | `/api/energy/components/{componentId}` | `200` with the updated component |
| `DELETE` | `/api/energy/components/{componentId}` | `204` |

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

`componentId` must match `ENG-` followed by three digits, `name` is required
and limited to 255 characters, `category` and `status` are required enum
values, and `capacityValue` must be zero or greater when supplied. For updates,
the ID in the path must match the request body.

Errors use `{ "timestamp": "...", "status": 400, "message": "..." }`.
Validation or a path/body ID mismatch returns `400`; a missing component
returns `404`; duplicate IDs and deletes blocked by dependent Energy Supply
records return `409`.

## Other endpoints

| Area | Methods and endpoints |
| --- | --- |
| Dashboard | `GET /api/energy/dashboard/summary` |
| Consumption | `GET /api/energy/consumption`, `/today`, `/breakdown/{category}`, `/{logId}`; `POST`, `PUT /{logId}`, `DELETE /{logId}` |
| Solar generation | `GET /api/energy/solar`, `/{logId}`, `/last-7-days`; `POST`, `PUT /{logId}`, `DELETE /{logId}` |
| Generator events | `GET /api/energy/generator-events`, `/{eventId}`; `POST`, `PUT /{eventId}`, `DELETE /{eventId}` |
| Alerts | `GET /api/energy/alerts`, `/open`; `PATCH /{alertId}/status` |
| Carbon metrics | `GET /api/energy/carbon` |
| What-if analysis | `POST /api/energy/whatif/simulate` |

## Development data

The demo data runner is enabled only with the Spring `dev` profile. Energy
component IDs are assigned by the application and are inserted as new entities;
the runner seeds missing component IDs and is transactional. Use a disposable
development database when starting the application with this profile.
