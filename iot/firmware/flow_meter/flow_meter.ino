// flow_meter - capteur IoT Water Supply

// Debitmetre a impulsions YF-S201 -> consommation (SC-01 / SC-12)
#define FLOW_PIN 27
volatile unsigned long pulses = 0;
void IRAM_ATTR pulseISR() { pulses++; }

// Communs : WiFi + envoi HTTP JSON vers le contrat d ingestion
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* WIFI_SSID = "MON_RESEAU";
const char* WIFI_PASS = "MOT_DE_PASSE";
const char* API_URL   = "http://192.168.1.50:8080/api/iot/telemetry";  // endpoint d ingestion (a creer cote backend)
const char* DEVICE_ID = "esp32-flow_meter-01";
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
  setup_extra();
}

void setup_extra() { pinMode(FLOW_PIN, INPUT_PULLUP); attachInterrupt(digitalPinToInterrupt(FLOW_PIN), pulseISR, FALLING); }

void loop() {
  if (millis() - lastSend < SEND_INTERVAL_MS) return;
  lastSend = millis();
  noInterrupts(); unsigned long p = pulses; pulses = 0; interrupts();
  float liters = p / 7.5;  // YF-S201 : ajuster selon la fiche technique
  JsonDocument values;
  values["flow_liters"] = liters;
  sendTelemetry("flow", "UUID_SOURCE", values);
}
