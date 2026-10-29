// soil_moisture - capteur IoT Water Supply

// Sonde capacitive d humidite du sol -> decision d irrigation (SC-01 / SC-02)
#define SOIL_PIN 32
const int DRY_VALUE = 3200;   // calibration a l air
const int WET_VALUE = 1400;   // calibration dans l eau

// Communs : WiFi + envoi HTTP JSON vers le contrat d ingestion
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* WIFI_SSID = "MON_RESEAU";
const char* WIFI_PASS = "MOT_DE_PASSE";
const char* API_URL   = "http://192.168.1.50:8080/api/iot/telemetry";  // endpoint d ingestion (a creer cote backend)
const char* DEVICE_ID = "esp32-soil_moisture-01";
const unsigned long SEND_INTERVAL_MS = 60000;  // 1 min
unsigned long lastSend = 0;

void connectWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED) { delay(500); }
}

void sendTelemetry(const char* type, const char* sourceId, JsonObject values) {
  if (WiFi.status() != WL_CONNECTED) return;  // SC-11 : buffer local + sync diffee a implementer
  HTTPClient http;
  http.begin(API_URL);
  http.addHeader("Content-Type", "application/json");
  String body;
  JsonDocument doc;
  doc["device_id"] = DEVICE_ID;
  doc["type"] = type;
  doc["source_id"] = sourceId;
  JsonObject v = doc["values"].to<JsonObject>();
  for (JsonPair kv : values) { v[kv.key()] = kv.value(); }
  doc["timestamp"] = millis();
  serializeJson(doc, body);
  http.POST(body);
  http.end();
}

void setup() {
  Serial.begin(115200);
  connectWiFi();
}

void loop() {
  if (millis() - lastSend < 600000) return;  // 10 min
  lastSend = millis();
  int raw = analogRead(SOIL_PIN);
  float percent = constrain(map(raw, DRY_VALUE, WET_VALUE, 0, 100), 0, 100);
  JsonDocument values;
  values["soil_moisture_percent"] = percent;
  sendTelemetry("soil", "UUID_ZONE", values);
}
