// water_level - capteur IoT Water Supply

// Capteur de niveau de reservoir (ultrason HC-SR04) -> SC-03 / SC-06
#include <NewPing.h>
#define TRIG_PIN 5
#define ECHO_PIN 18
#define TANK_HEIGHT_CM 150.0
#define TANK_CAPACITY_L 50000.0
NewPing sonar(TRIG_PIN, ECHO_PIN);

// Communs : WiFi + envoi HTTP JSON vers le contrat d ingestion
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* WIFI_SSID = "MON_RESEAU";
const char* WIFI_PASS = "MOT_DE_PASSE";
const char* API_URL   = "http://192.168.1.50:8080/api/iot/telemetry";  // endpoint d ingestion (a creer cote backend)
const char* DEVICE_ID = "esp32-water_level-01";
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
  if (millis() - lastSend < SEND_INTERVAL_MS) return;
  lastSend = millis();
  float distanceCm = sonar.ping_median(5) / US_ROUNDTRIP_CM;
  float levelCm = TANK_HEIGHT_CM - distanceCm;
  float levelLiters = (levelCm / TANK_HEIGHT_CM) * TANK_CAPACITY_L;
  JsonDocument values;
  values["level_liters"] = levelLiters;
  values["level_percent"] = (levelLiters / TANK_CAPACITY_L) * 100;
  sendTelemetry("level", "UUID_SOURCE", values);
}
