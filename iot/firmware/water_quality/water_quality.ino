// water_quality - capteur IoT Water Supply

// Sonde pH + turbidite + temperature -> SC-05
#define PH_PIN 34
#define TURBIDITY_PIN 35
#include <OneWire.h>
#include <DallasTemperature.h>
OneWire oneWire(4);
DallasTemperature tempSensor(&oneWire);

// Communs : WiFi + envoi HTTP JSON vers le contrat d ingestion
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* WIFI_SSID = "MON_RESEAU";
const char* WIFI_PASS = "MOT_DE_PASSE";
const char* API_URL   = "http://192.168.1.50:8080/api/iot/telemetry";  // endpoint d ingestion (a creer cote backend)
const char* DEVICE_ID = "esp32-water_quality-01";
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
  if (millis() - lastSend < 900000) return;  // 15 min
  lastSend = millis();
  float ph = (analogRead(PH_PIN) * 3.3 / 4095.0) * 3.5;        // calibrer avec solutions pH 4/7/10
  float ntu = map(analogRead(TURBIDITY_PIN), 0, 4095, 300, 0); // calibrer
  tempSensor.requestTemperatures();
  float celsius = tempSensor.getTempCByIndex(0);
  JsonDocument values;
  values["ph"] = ph;
  values["turbidity_ntu"] = ntu;
  values["temperature_celsius"] = celsius;
  sendTelemetry("quality", "UUID_SOURCE", values);
}
