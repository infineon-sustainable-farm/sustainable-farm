# Water Supply for Visitor Management

This page covers what a visitor screen or a visit can read from Water Supply, and what you send to
us. It follows your answers of 2026-10-29: **nothing changes on your side**. You use your
registration id, your stop names and your time slots; Water Supply adapts to them. Everything is
ready and tested, except the demo valve (section 7).

## 1. Your stops, mapped on our side

Your tour stops have a name and a position, no water id. The water team maps, in its **Visitors**
screen (`/watersupply/visitors`), the stops that pass by water facilities (for example "Smart drip
irrigation") to the zone and/or the tank they show.

- Send stop names as you display them; case and surrounding spaces do not matter.
- A stop the water team did not map is not a water stop: it is ignored in visit announcements, and a
  summary asked for it answers `404` (show nothing for it).
- The mapping itself is readable at `GET /api/water/visitor/stops` if you want to know which stops
  have water data.

## 2. One call for a visitor screen

`GET /api/water/visitor/summary?stop=Smart%20drip%20irrigation&sessionFrom=…&sessionTo=…`

`stop` can be replaced by `sourceId` and/or `zoneId`. `sessionFrom` and `sessionTo` are the start
and end of the visit's time slot, as ISO instants (the site's local time is UTC+0).

```json
{
  "stop": "Smart drip irrigation",
  "sourceLevel": { "sourceId": "…", "name": "Rain tank", "capacityLiters": 5000, "currentLevelLiters": 2500,
                   "levelPercentage": 50.0, "status": "…", "rainwaterTank": true, "readAt": "…" },
  "waterSavingsPercentage": 18,
  "irrigationStatus": { "active": true, "zoneId": "…", "scheduleId": "…", "status": "running",
                        "lastActivationAt": "2026-10-13T10:02:00Z", "plannedLiters": 300.0 },
  "liveConsumption": { "stop": "Smart drip irrigation", "windowMinutes": 15, "liters": 30.0,
                       "flowRateLitersPerMinute": 2.0, "lastMeasurementAt": "…",
                       "session": { "from": "…", "to": "…", "inProgress": true, "liters": 120.0, "measurementCount": 2 } },
  "activeUnavailability": []
}
```

If the weather service is down, `waterSavingsPercentage` is `null` and the rest is still returned.

## 3. The live counter

`GET /api/water/visitor/live-consumption?stop=…&sessionFrom=…&sessionTo=…`

As you asked:

- **`session.liters` is the total to show**: the water measured from the start of the time slot to
  now while the visit is in progress, or to the end of the slot once it is over. A session lasts at
  most one day; `sessionTo` before `sessionFrom` returns `400`.
- **The 15-minute window only gives the instantaneous flow**: `flowRateLitersPerMinute`, computed on
  the last `windowMinutes` (15 by default). `liters` is the volume of that window.
- There is no streaming: poll the endpoint, every 30 to 60 seconds is enough for the sensors'
  sending rate.

## 4. The same data, endpoint by endpoint

| Need | Endpoint |
|---|---|
| Tank level now | `GET /api/water/sources/{sourceId}/level` |
| Tank level history (for a chart) | `GET /api/water/sources/{sourceId}/level-history?from=…&to=…` |
| Share of water saved, and how it is computed | `GET /api/dashboard/water-savings?period=day|week|month|year` (need = area × ET0 × Kc / efficiency, compared with the metered consumption) |
| Is the zone being irrigated, and when did it last start | `GET /api/water/visitor/irrigation-status?zoneId=…` (or the summary with `stop`) |

`active` is `true` while a schedule is `running`; otherwise the latest schedule is shown.
`lastActivationAt` is its actual start, or its planned start if it never ran. A zone with no schedule
returns `"status": "none"`.

## 5. Planned water outages and maintenance

`GET /api/water/outages?from=…&to=…&zoneId=…&sourceId=…&activeOn=…` (every filter is optional)

```json
[ { "id": "…", "scope": "zone", "scopeId": "…", "startAt": "2026-10-20T08:00:00Z",
    "endAt": "2026-10-20T12:00:00Z", "reason": "filter cleaning", "status": "planned", "createdAt": "…" } ]
```

- **Polling every 2 to 5 minutes is enough**, as agreed: no webhook.
- `scope` is `farm`, `source` or `zone`. Filtering on a `zoneId` (or a `sourceId`) also returns the
  farm-wide periods of its farm. `activeOn=<instant>` returns only the periods in force at that
  instant; cancelled periods are left out.
- The water team plans the periods in its **Planned outages** screen.

## 6. Tell us a group is coming

`POST /api/water/visitor-events` → `201`

```json
{ "visitId": 1842, "groupName": "School group A", "expectedAt": "2026-10-20T09:00:00Z",
  "stops": ["Smart drip irrigation", "Solar plant & tracking"] }
```

```json
{ "eventId": "…", "visitId": "1842", "created": true, "expectedAt": "…", "zones": ["…"],
  "stops": ["Smart drip irrigation"], "ignoredStops": ["Solar plant & tracking"],
  "conflict": false, "conflictingZones": [], "conflictingStops": [],
  "notificationTitle": "Visitor group near water facilities - School group A" }
```

- `visitId` is your **registration id**, sent as a number or as text. It is the idempotency key:
  sending the same visit again (reschedule, retry) updates it, and the response has `"created": false`.
- `stops` are your stop names; the unmapped ones come back in `ignoredStops`.
- The water team gets a notification. If a mapped stop has its zone or tank under a planned outage
  at `expectedAt`, the response has `conflict: true` with `conflictingStops`, and the notification
  is raised as a warning.

## 7. Water safety briefing

`GET /api/water/visitor/safety-briefing`

```json
{ "version": "2026-10-22", "allowedZones": ["reservoir surroundings", "demonstration valve"],
  "forbiddenZones": ["electric pump room"], "risks": ["slippery ground near the basin", "electrical hazard at the pump"],
  "text": "…", "maxCapacity": 10, "guideRequired": true, "validated": true, "publishedAt": "…" }
```

The roles are the ones you proposed:

- **Water Supply** writes and validates the water content (`text`, `risks`), and publishes it from its
  Visitors screen. `PATCH` publishes a new version; the earlier versions are kept.
- **Site Security** defines the access rules: `allowedZones` and `forbiddenZones` must match them.
- **Visitor Management** adds this text to its existing safety briefing and collects the signature.

Until a briefing is published, a default text is returned with `"validated": false`: do not show it
to visitors as an official one.

## 8. Demo valve

`POST /api/iot/devices/{deviceId}/commands` with `{"command":"OPEN_VALVE","durationSeconds":20,"reason":"visitor_demo"}`.
The opening is capped at 30 seconds, refused while an outage is in progress, and recorded in the
command journal. Today the answer is **`503 VALVE_GATEWAY_UNAVAILABLE`**, because no device gateway
exists yet. The details are in [watersupply-for-iot-and-site-security.md](watersupply-for-iot-and-site-security.md).
Please do not put a button in front of visitors until that page says the gateway is live.

## How to call us

HTTP is the simplest option. In-process works too, in the same JVM: inject `VisitorIntegrationService`
(`visitorEvent`, `summary`, `liveConsumption`, `safetyBriefing`) and `WaterUnavailabilityService`
(`search`). The service objects return the same maps as the HTTP responses.

## Decisions recorded (2026-10-29)

| Topic | Decision |
|---|---|
| Visit identifier | Registration id (`Long`) |
| Stops | Identified by name; mapped to zones and tanks by Water Supply; unmapped stops are not water stops |
| Live counter | Total over the visit's time slot; 15-minute window for the instantaneous flow only |
| Safety briefing | Water Supply writes the water content, Site Security sets the access rules, Visitor Management integrates it and collects the signature |
| Outages | Polling every 2 to 5 minutes, no webhook |

Nothing is pending on your side.
