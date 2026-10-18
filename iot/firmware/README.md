# ESP32 Firmware - Water Supply

The sensors are **wired onto the ESP32 GPIOs**. Two options:

## Recommended option: single gateway
`esp32_gateway/esp32_gateway.ino` - a single ESP32 per zone reads all the sensors
(level, flow, rain, soil, pH/turbidity/temperature) and sends the telemetry
to POST /api/iot/telemetry. The wiring table is at the top of the sketch:

| Sensor | Model | ESP32 GPIO |
|---|---|---|
| Tank level (ultrasonic) | HC-SR04 | TRIG=5, ECHO=18 |
| Flow (pulses) | YF-S201 | 27 |
| Rain gauge (tipping) | RG-11 type | 26 |
| Soil moisture (capacitive) | v1.2 | 32 (ADC1) |
| pH | analog probe | 34 (ADC1) |
| Turbidity | TS-300B | 35 (ADC1) |
| Temperature | DS18B20 (OneWire) | 4 |
| Solenoid valve relay (future) | relay module | 25 |

## Alternative: one ESP32 per sensor
The folders water_level, flow_meter, water_quality, rain_gauge, soil_moisture
contain simple single-sensor sketches (independent nodes).

## Configuration (in each sketch)
- WIFI_SSID / WIFI_PASS: site network
- API_URL: http://<backend-host>:8080/api/iot/telemetry
- DEVICE_ID: unique id of the device
- SOURCE_ID / ZONE_ID: ids visible in the Sources / Farms & Fields screens

## Calibration (to do on site)
- water_level: TANK_HEIGHT_CM / TANK_CAPACITY_L + ultrasonic offset
- flow_meter: LITERS_PER_PULSE according to the datasheet (7.5 pulses/s = 1 L/min for YF-S201)
- water_quality: pH with 4/7/10 buffer solutions; NTU with known samples
- rain_gauge: MM_PER_TIP according to the tipping bucket model
- soil_moisture: SOIL_DRY / SOIL_WET (air / water)

## Notes SC-11 / SC-13
On HTTP failure (offline / outage): store the measurements locally
(SPIFFS/Preferences) and resend them when the network is back - to implement
in postTelemetry (flagged in the gateway sketch).
