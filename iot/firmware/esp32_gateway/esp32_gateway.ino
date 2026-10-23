// ============================================================
// ESP32 SENSOR GATEWAY - Water Supply (Sustainable Farm)
// -----------------------------------------------------------
// All the sensors are wired onto THIS ESP32 (zone gateway).
// The ESP32 reads each sensor and sends the telemetry in JSON to:
//   POST /api/iot/telemetry   (contract: iot/README.md section 3)
//
// Sensor wiring (ESP32 DevKit V1 - 3.3V):
//   Tank level sensor     HC-SR04 ultrasonic      TRIG=GPIO5  ECHO=GPIO18
//   Flow sensor (YF-S201) pulses                  GPIO27 (signal)
//   Tipping rain gauge    dry contact             GPIO26 (signal)
//   Soil moisture probe    capacitive v1.2        GPIO32 (ADC1)
//   pH probe               analog                 GPIO34 (ADC1)
//   Turbidity probe        analog                 GPIO35 (ADC1)
//   Temperature probe      DS18B20 (OneWire)      GPIO4
//   Solenoid valve relay   relay module           GPIO25 (optional, future actuation)
// ============================================================

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <NewPing.h>
#include <OneWire.h>
#include <DallasTemperature.h>

// ---------- Network / API configuration ----------
const char* WIFI_SSID = "MON_RESEAU";
const char* WIFI_PASS = "MOT_DE_PASSE";
const char* API_URL   = "http://192.168.1.50:8080/api/iot/telemetry";
const char* DEVICE_ID = "esp32-gateway-zone-a-01";

// ---------- Configuration ids (taken from the screens / API) ----------
const char* SOURCE_ID = "UUID_DE_LA_SOURCE";   // monitored source (e.g. cistern)
const char* ZONE_ID   = "UUID_DE_LA_ZONE";     // drip zone

// ---------- Cablage ----------
#define TRIG_PIN       5
#define ECHO_PIN       18
#define FLOW_PIN       27
#define RAIN_PIN       26
#define SOIL_PIN       32
#define PH_PIN         34
#define TURBIDITY_PIN  35
#define ONEWIRE_PIN    4
#define VALVE_PIN      25

// ---------- Calibration ----------
const float TANK_HEIGHT_CM  = 150.0;
const float TANK_CAPACITY_L = 50000.0;
const float LITERS_PER_PULSE = 1.0 / 7.5;   // YF-S201: 7.5 pulses = 1 L/min
const float MM_PER_TIP      = 0.279;        // tipping bucket
const int   SOIL_DRY        = 3200;         // probe in air
const int   SOIL_WET        = 1400;         // probe in water

// ---------- State ----------
NewPing sonar(TRIG_PIN, ECHO_PIN);
OneWire oneWire(ONEWIRE_PIN);
DallasTemperature tempSensor(&oneWire);
volatile unsigned long flowPulses = 0;
volatile unsigned long rainTips  = 0;
unsigned long lastLevelMs = 0, lastQualityMs = 0, lastRainMs = 0, lastSoilMs = 0;
const unsigned long LEVEL_MS   = 60000;    // level: 1 min
const unsigned long QUALITY_MS = 900000;   // quality: 15 min
const unsigned long RAIN_MS    = 60000;    // rain: 1 min
const unsigned long SOIL_MS    = 600000;   // soil: 10 min

void IRAM_ATTR onFlowPulse() { flowPulses++; }
void IRAM_ATTR onRainTip()   { rainTips++;  }

void connectWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    if (millis() - start > 30000) return;  // SC-11: offline -> local buffer (to implement)
  }
}

void postTelemetry(const char* type, const char* sourceId, const char* zoneId, JsonObject values) {
  if (WiFi.status() != WL_CONNECTED) {
    // SC-11 / SC-13: store the measurement locally (SPIFFS) and resend when the network is back.
    return;
  }
  HTTPClient http;
  http.begin(API_URL);
  http.addHeader("Content-Type", "application/json");
  JsonDocument doc;
  doc["device_id"] = DEVICE_ID;
  doc["type"]      = type;
  if (sourceId) doc["source_id"] = sourceId;
  if (zoneId)   doc["zone_id"]   = zoneId;
  JsonObject v = doc["values"].to<JsonObject>();
  for (JsonPair kv : values) { v[kv.key()] = kv.value(); }
  doc["timestamp"] = "" ;  // the backend timestamps when absent
  String body;
  serializeJson(doc, body);
  int code = http.POST(body);
  http.end();
  Serial.printf("[%s] HTTP %d\n", type, code);
}

void readLevel() {
  float distanceCm = sonar.ping_median(5) / US_ROUNDTRIP_CM;
  float levelCm = TANK_HEIGHT_CM - distanceCm;
  float liters = (levelCm / TANK_HEIGHT_CM) * TANK_CAPACITY_L;
  JsonDocument v;
  v["level_liters"] = liters;
  v["level_percent"] = (liters / TANK_CAPACITY_L) * 100.0;
  postTelemetry("level", SOURCE_ID, NULL, v);
}

void readFlow() {
  noInterrupts(); unsigned long p = flowPulses; flowPulses = 0; interrupts();
  float liters = p * LITERS_PER_PULSE;
  if (liters <= 0) return;
  JsonDocument v;
  v["flow_liters"] = liters;
  postTelemetry("flow", SOURCE_ID, NULL, v);
}

void readRain() {
  noInterrupts(); unsigned long t = rainTips; rainTips = 0; interrupts();
  float mm = t * MM_PER_TIP;
  if (mm <= 0) return;
  JsonDocument v;
  v["rainfall_mm"] = mm;
  v["catchment_area_m2"] = 180.0;
  postTelemetry("rain", SOURCE_ID, NULL, v);
}

void readSoilAndQuality() {
  tempSensor.requestTemperatures();
  float celsius = tempSensor.getTempCByIndex(0);

  int soilRaw = analogRead(SOIL_PIN);
  float soilPct = constrain(map(soilRaw, SOIL_DRY, SOIL_WET, 0, 100), 0, 100);
  JsonDocument vs;
  vs["soil_moisture_percent"] = soilPct;
  postTelemetry("soil", NULL, ZONE_ID, vs);

  float ph  = (analogRead(PH_PIN) * 3.3 / 4095.0) * 3.5;          // calibrate (4/7/10 solutions)
  float ntu = map(analogRead(TURBIDITY_PIN), 0, 4095, 300, 0);    // calibrate
  JsonDocument vq;
  vq["ph"] = ph;
  vq["turbidity_ntu"] = ntu;
  vq["temperature_celsius"] = celsius;
  postTelemetry("quality", SOURCE_ID, NULL, vq);
}

void setup() {
  Serial.begin(115200);
  pinMode(FLOW_PIN, INPUT_PULLUP);
  pinMode(RAIN_PIN, INPUT_PULLUP);
  pinMode(VALVE_PIN, OUTPUT);
  digitalWrite(VALVE_PIN, LOW);  // valve closed by default (SC-13: passive fail-safe)
  attachInterrupt(digitalPinToInterrupt(FLOW_PIN), onFlowPulse, FALLING);
  attachInterrupt(digitalPinToInterrupt(RAIN_PIN), onRainTip, FALLING);
  tempSensor.begin();
  connectWiFi();
}

void loop() {
  unsigned long now = millis();
  if (now - lastLevelMs  >= LEVEL_MS)   { lastLevelMs  = now; readLevel(); }
  if (now - lastRainMs   >= RAIN_MS)    { lastRainMs   = now; readRain(); }
  if (now - lastSoilMs   >= SOIL_MS)    { lastSoilMs   = now; readSoilAndQuality(); }
  if (now - lastQualityMs >= QUALITY_MS) { lastQualityMs = now; readSoilAndQuality(); }
  readFlow();  // flow is drained on every loop turn
  delay(1000);
}
