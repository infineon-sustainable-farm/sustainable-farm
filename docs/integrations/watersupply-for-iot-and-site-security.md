# Water Supply for IoT and Site Security

Water Supply can now **send** a command to a valve, not only receive telemetry. The decision logic,
the safety checks and the audit journal are done. Two things are still missing, and they are yours
to decide: the **device gateway** (how a command reaches the valve) and the **permissions** (who is
allowed to open a valve).

## 1. Command a valve

`POST /api/iot/devices/{deviceId}/commands`

```json
{ "command": "OPEN_VALVE", "durationSeconds": 20, "reason": "visitor_demo", "requestedBy": "<user uuid>" }
```

The checks run in this order, before anything is sent:

| Check | Refusal |
|---|---|
| `command` is `OPEN_VALVE` or `CLOSE_VALVE` | `400` |
| The device is known (it registers itself with its first telemetry) | `404` |
| The device was seen less than 15 minutes ago (`WATER_IOT_OFFLINE_THRESHOLD_MINUTES`) | `409` |
| `OPEN_VALVE`: 1 to 30 seconds (`WATER_IOT_VALVE_MAX_DURATION_SECONDS`). A missing duration means 30 s | `400` |
| `OPEN_VALVE`: no planned water outage in progress. `CLOSE_VALVE` is always allowed | `409` |

Every accepted request is written to `iot_commands`, then handed to the gateway:

- the gateway accepts it: `202` with `status: "dispatched"`;
- no gateway, or the gateway is down: **`503 VALVE_GATEWAY_UNAVAILABLE`**. The journal still keeps the
  attempt, with `status: "rejected"`. This is today's behaviour.

`GET /api/iot/devices/{deviceId}/commands` returns the journal, most recent first:

```json
[ { "commandId": "…", "deviceId": "valve-demo-01", "command": "OPEN_VALVE", "status": "rejected",
    "payload": "{\"durationSeconds\":20,\"reason\":\"visitor_demo\"}", "requestedBy": "…",
    "requestedAt": "…", "acknowledgedAt": null } ]
```

## 2. Plug in the gateway (IoT)

The outbound channel is one interface, `service/ValveCommandPort`:

```java
void send(IotCommand command); // throw ValveGatewayUnavailableException if the command cannot leave
```

The current implementation, `NotConfiguredValveCommandPort`, refuses everything. A real adapter
(MQTT, an HTTP gateway, LoRa) is a new `@Component` marked `@Primary` that implements this
interface. Nothing else needs to change. The `acknowledgedAt` column is ready for when the device
confirms. Water Supply will add a confirmation endpoint once the protocol is known.

To show as online, a valve has to send telemetry to `POST /api/iot/telemetry` (with its
`device_id`) at least every 15 minutes.

## 3. Permissions (Site Security / platform)

`WaterAuthorizationService.checkPhysicalCommandAllowed("iot.command.OPEN_VALVE")` is called before
any command. Today it allows everything, because the platform has no authentication yet. It is the
single place to plug the platform's role check in.

## 4. Planned outages

Planned water unavailability (`/api/water/outages`, described in the
[Visitor Management page](watersupply-for-visitor-management.md#3-planned-water-outages-and-maintenance))
already blocks valve openings and automatic irrigation. Site Security can read it to know when
maintenance work is planned on the water network.

## What we need from you

1. **IoT: protocol.** Is it MQTT, an HTTP gateway or something proprietary? How does the valve
   confirm that it opened and that it closed?
2. **IoT: device type.** Should valve devices declare a `type` (for example `valve`) in their
   telemetry, so that commands are refused on sensors?
3. **Site Security: roles.** Which role may open a valve? Do we need a second confirmation, or a
   simulation-only mode for the visitor demo?
4. **Site Security: briefing.** Who owns the water safety text shown to visitors
   (`/api/water/visitor/safety-briefing`)? Your critical zone already includes the main water valves.
5. **Site Security: the demo valve.** Is the demo valve inside or outside your critical zone?
