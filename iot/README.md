# IoT - Water Supply (real system integration)

> This folder holds everything about the **mandatory** integration of the IoT system:
> sensor feature analysis, ingestion contract, ESP32 architecture and firmware.
> **The ingestion endpoint is implemented and validated**: POST /api/iot/telemetry (watersupply backend).

## 1. Retained architecture

```
[ Field sensors ]  --wired-->  [ ESP32 zone gateway ]  --Wi-Fi/HTTP JSON-->  [ Spring backend ]  -->  [ PostgreSQL ]  -->  [ React frontend ]
 level, flow,                                            POST /api/iot/telemetry        routing to            history,
 rain, soil, quality                                    (single object or batch)      business tables     charts, alerts
```

- **One ESP32 per zone/site**: all the zone's sensors are wired to its GPIOs.
- The ESP32 reads the sensors, timestamps them (or lets the backend timestamp), and sends JSON.
- An unreadable measurement never fails the batch: the backend answers 202 with the status of
  each measurement (`processed` / `rejected` / `not_routed_yet`).
- Device authentication will be provided by the future **global software** (out of the module's scope).

## 2. Analysis: which features depend on the sensors?

### A. Sensor measurements (automatic ingestion - the UI does NOT type them)

| # | Measurement | Sensor | Ingestion endpoint | UI impact | Scenarios |
|---|---|---|---|---|---|
| A1 | Tank level | HC-SR04 ultrasonic | `level` -> `water_sources.current_level_liters` | /sources (% level), /rainwater (gauge), / | SC-03, SC-06 |
| A2 | Flow / consumed volume | YF-S201 pulse | `flow` -> `water_consumption` | /consumption (chart, READ-ONLY), / KPIs | SC-01, SC-10, SC-12 |
| A3 | Rainfall | Tipping bucket | `rain` -> `rainwater_harvests` (volume computed by the backend) | /rainwater | SC-06, SC-12 |
| A4 | Water quality | pH probe + turbidity + DS18B20 | `quality` -> `water_quality_tests` (+ automatic notification when out of range) | /quality (alerts) | SC-05 |
| A5 | Soil moisture | Capacitive probe | `soil` -> `soil_moisture_readings`: feeds the automatic irrigation triggering rule | /irrigation (auto control) | SC-01, SC-02 |
| A6 | Abnormal drip flow | Pressure/flow sensor | `clogging` -> `drip_maintenance_logs` (clogging) | /maintenance | SC-07 |
| A7 | Gateway availability / power | heartbeat + battery | `gateway` -> **not supported**: unknown types are rejected (heartbeat to add on the backend side) | Offline / Power outage badges (to wire) | SC-11, SC-13 |

### B. Human data (legitimate manual entry - not IoT)

| Data | Screen |
|---|---|
| Farms / Fields / Zones (site config) | /farms |
| Sources (type, capacity) | /sources |
| Irrigation schedules + start/stop | /irrigation |
| Maintenance interventions (visits) | /maintenance |

### C. Applied decision
- Consumption is **READ-ONLY** in the UI (the volumes come from the A2 sensors): manual CRUD was removed.

## 3. Ingestion contract (IMPLEMENTED AND VALIDATED)

```
POST /api/iot/telemetry
Content-Type: application/json
```

A single object **or** an array (batch). Fields:

| Field | Type | Required | Description |
|---|---|---|---|
| device_id | string | yes | id of the ESP32 |
| type | string | yes | level - flow - quality - rain - clogging - soil (any other type is rejected) |
| source_id | uuid | depends on type | concerned source (level, flow, quality, rain) |
| zone_id | uuid | depends on type | concerned zone (clogging, soil) |
| values | object | yes | measured values (see examples) |
| timestamp | ISO-8601 | no | the backend timestamps now when absent |

Real examples (tested and validated against this backend):

```json
{"device_id":"esp32-a-01","type":"level","source_id":"<uuid>","values":{"level_percent":15}}
{"device_id":"esp32-a-01","type":"flow","source_id":"<uuid>","zone_id":"<uuid>","values":{"flow_liters":250}}
{"device_id":"sonde-ph-01","type":"quality","source_id":"<uuid>","values":{"ph":5.2,"turbidity_ntu":8.4,"temperature_celsius":20}}
{"device_id":"pluviometre-01","type":"rain","source_id":"<uuid>","values":{"rainfall_mm":15,"catchment_area_m2":180}}
{"device_id":"capteur-debit-01","type":"clogging","zone_id":"<uuid>","values":{"severity":"high"}}
```

Response: `202 Accepted` with the status of each measurement, e.g.:

```json
[
  {"type":"level","source_id":"...","status":"processed","message":"Tank level = 1500.0 L"},
  {"type":"quality","source_id":"...","status":"processed","message":"OUT-OF-RANGE measurement saved (alert generated)"},
  {"type":"soil","zone_id":"...","status":"processed","message":"Soil moisture = 32 % (zone ...)"}
]
```

Planned frequencies: level 1 min - flow at every pulse (flush 1 min) - rain 1 min - quality 15 min - soil 10 min - heartbeat 30 s.

## 4. Firmware

See `iot/firmware/`:
- `firmware/esp32_gateway/esp32_gateway.ino`: **main gateway** - all sensors wired onto a single ESP32, periodic send per measurement type.
- Folders `water_level`, `flow_meter`, `water_quality`, `rain_gauge`, `soil_moisture`: single-sensor sketches (simple alternative nodes).
- Wiring table and detailed calibration in `firmware/README.md`.

## 5. Checks performed (2026-09-16)

A real telemetry batch sent to the running backend: level, flow, quality, rain, clogging -> all `processed`;
soil -> `not_routed_yet` (accepted). Effects verified directly in SQL:
tank level updated, consumption recorded, out-of-range quality test (notification generated),
rainwater harvest computed by the backend (180 m2 x 15 mm x 0.8 = 2160 L), clogging intervention created.
