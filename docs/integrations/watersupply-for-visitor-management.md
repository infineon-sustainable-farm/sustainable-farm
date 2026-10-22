# Water Supply for Visitor Management

This page covers what a visitor screen or a visit booking can read from Water Supply, and what you
can send to us. Everything is ready and tested, except the demo valve: its endpoint and safety
checks exist, but no device gateway is connected yet (section 6).

Zones and sources are UUIDs on our side. Your tour stops will need to store the water `zoneId` or
`sourceId` they pass by.

## 1. One call for a visitor screen

`GET /api/water/visitor/summary?sourceId=…&zoneId=…` (both optional)

```json
{
  "sourceLevel": { "sourceId": "…", "name": "Rain tank", "capacityLiters": 5000, "currentLevelLiters": 2500,
                   "levelPercentage": 50.0, "status": "…", "rainwaterTank": true, "readAt": "…" },
  "waterSavingsPercentage": 18,
  "irrigationStatus": { "active": true, "zoneId": "…", "scheduleId": "…", "status": "running",
                        "lastActivationAt": "2026-10-13T10:02:00Z", "plannedLiters": 300.0 },
  "liveConsumption": { "sourceId": "…", "zoneId": "…", "windowMinutes": 15, "liters": 12.5,
                       "measurementCount": 1, "lastMeasurementAt": "…" },
  "activeUnavailability": []
}
```

If the weather service is down, `waterSavingsPercentage` is `null` and the rest of the response is
still returned. An unknown `sourceId` or `zoneId` returns `404`.

## 2. The same data, endpoint by endpoint

| Need | Endpoint |
|---|---|
| Tank level now | `GET /api/water/sources/{sourceId}/level` |
| Tank level history (for a chart) | `GET /api/water/sources/{sourceId}/level-history?from=…&to=…` |
| Share of water saved, and how it is computed | `GET /api/dashboard/water-savings?period=day|week|month|year` (need = area × ET0 × Kc / efficiency, compared with the metered consumption) |
| Is the zone being irrigated, and when did it last start | `GET /api/water/visitor/irrigation-status?zoneId=…` |
| Live counter | `GET /api/water/visitor/live-consumption?sourceId=…&zoneId=…&windowMinutes=15` |

- On the irrigation status, `active` is `true` while a schedule is `running`. Otherwise the latest
  schedule is shown. `lastActivationAt` is the actual start of that schedule, or its planned start
  if it never ran. A zone with no schedule at all returns `"status": "none"`.
- The live counter is the volume metered over the last `windowMinutes` minutes. The default is 15,
  and the value is kept between 1 and 1440. Both filters are optional, and leaving both out gives the
  whole site. There is no streaming: poll this endpoint, every 30 to 60 seconds is enough for the
  sensors' sending rate.

## 3. Planned water outages and maintenance

`GET /api/water/outages?from=…&to=…&zoneId=…&sourceId=…&activeOn=…` (every filter is optional)

```json
[ { "id": "…", "scope": "zone", "scopeId": "…", "startAt": "2026-10-20T08:00:00Z",
    "endAt": "2026-10-20T12:00:00Z", "reason": "filter cleaning", "status": "planned", "createdAt": "…" } ]
```

- `scope` is `farm`, `source` or `zone`. Filtering on a `zoneId` (or a `sourceId`) also returns the
  farm-wide periods of its farm.
- `activeOn=<instant>` returns only the periods in force at that instant. Cancelled periods are left
  out.
- `GET /api/water/outages/{id}` returns a single period. Creating, updating and deleting periods
  (`POST`, `PUT`, `DELETE /api/water/outages`) is the water team's job, from its **Planned outages**
  screen.

Use it to warn the guide, or to refuse a booking on a zone with no water.

## 4. Tell us a group is coming

`POST /api/water/visitor-events` → `201`

```json
{ "visitId": "BOOKING-1842", "groupName": "School group A", "expectedAt": "2026-10-20T09:00:00Z",
  "zones": ["<water zoneId>", "…"] }
```

```json
{ "eventId": "…", "visitId": "BOOKING-1842", "created": true, "expectedAt": "…", "zones": ["…"],
  "conflict": false, "conflictingZones": [], "notificationTitle": "Visitor group near water facilities - School group A" }
```

- `visitId` is the idempotency key (at most 128 characters). Sending the same visit again, for
  example after a reschedule or a retry, updates it, and the response then has `"created": false`.
  Send your booking id, such as `"BOOKING-" + id`.
- The water team gets a notification. If one of the zones has a planned outage at `expectedAt`, the
  response has `conflict: true` and lists the affected zones, and the notification is raised as a
  warning.

## 5. Water safety briefing

`GET /api/water/visitor/safety-briefing`

```json
{ "version": "2026-10-22", "allowedZones": ["reservoir surroundings", "demonstration valve"],
  "forbiddenZones": ["electric pump room"], "risks": ["slippery ground near the basin", "electrical hazard at the pump"],
  "text": "…", "maxCapacity": 10, "guideRequired": true, "validated": true, "publishedAt": "…" }
```

- Until someone publishes a briefing, a default text is returned with `"validated": false`. Do not
  show that text to visitors as an official one.
- `PATCH /api/water/visitor/safety-briefing` publishes a new version, and the earlier versions are
  kept. Fields you leave out keep their current value.
- The water team publishes it from its **Visitors** screen (`/watersupply/visitors`), which also
  previews exactly what `/api/water/visitor/summary` sends you.
- This fits next to your own per-registration briefing (`/api/v1/registrations/{id}/briefing`), where
  you can copy the water part.

## 6. Demo valve

`POST /api/iot/devices/{deviceId}/commands` with `{"command":"OPEN_VALVE","durationSeconds":20,"reason":"visitor_demo"}`.
The opening is capped at 30 seconds, refused while an outage is in progress, and recorded in the
command journal. Today the answer is **`503 VALVE_GATEWAY_UNAVAILABLE`**, because no device gateway
exists yet. The details are in [watersupply-for-iot-and-site-security.md](watersupply-for-iot-and-site-security.md).
Please do not put a button in front of visitors until that page says the gateway is live.

## How to call us

HTTP is the simplest option. In-process works too, in the same JVM: inject `VisitorIntegrationService`
(`visitorEvent`, `summary`, `liveConsumption`, `safetyBriefing`) and `WaterUnavailabilityService`
(`search`). The service objects return the same maps as the HTTP responses.

## What we need from you

1. **Visit identifier.** Is the booking id (`Long`) the right `visitId`, or would the registration id
   be better?
2. **Zones visited.** Which tour stops go near water facilities, and can you store their water
   `zoneId` or `sourceId`?
3. **"Live".** Is a 15-minute window right for the counter, or do you want the current day or the
   visit session?
4. **Briefing owner.** Who writes and validates the safety text: Water Supply, Site Security or
   Visitor Management? Site Security already lists the main water valves in its critical zone.
5. **Push or poll.** Do you need us to notify you when an outage is created, or is polling
   `/api/water/outages` enough?
