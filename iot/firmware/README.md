# Firmware ESP32 - Water Supply

Les capteurs sont **cables sur les GPIO de l ESP32**. Deux options :

## Option recommandee : passerelle unique
`esp32_gateway/esp32_gateway.ino` - une seule ESP32 par zone lit tous les capteurs
(niveau, debit, pluie, sol, pH/turbidite/temperature) et envoie la telemetrie
vers POST /api/iot/telemetry. La table de cablage est en tete du croquis :

| Capteur | Modele | GPIO ESP32 |
|---|---|---|
| Niveau reservoir (ultrason) | HC-SR04 | TRIG=5, ECHO=18 |
| Debit (impulsions) | YF-S201 | 27 |
| Pluviometre (auget) | RG-11 type | 26 |
| Humidite du sol (capacitive) | v1.2 | 32 (ADC1) |
| pH | sonde analogique | 34 (ADC1) |
| Turbidite | TS-300B | 35 (ADC1) |
| Temperature | DS18B20 (OneWire) | 4 |
| Relais electrovanne (futur) | module relais | 25 |

## Alternative : un ESP32 par capteur
Les dossiers water_level, flow_meter, water_quality, rain_gauge, soil_moisture
contiennent des croquis mono-capteur simples (noeuds independants).

## Configuration (dans chaque croquis)
- WIFI_SSID / WIFI_PASS : reseau du site
- API_URL : http://<host-backend>:8080/api/iot/telemetry
- DEVICE_ID : identifiant unique de l appareil
- SOURCE_ID / ZONE_ID : ids visibles dans les ecrans Sources / Fermes & Champs

## Calibration (a faire sur site)
- water_level : TANK_HEIGHT_CM / TANK_CAPACITY_L + offset ultrason
- flow_meter : LITERS_PER_PULSE selon la fiche (7.5 impulsions/s = 1 L/min pour YF-S201)
- water_quality : pH avec solutions tampon 4/7/10 ; NTU avec echantillons connus
- rain_gauge : MM_PER_TIP selon le modele d auget
- soil_moisture : SOIL_DRY / SOIL_WET (air / eau)

## Notes SC-11 / SC-13
En cas d echec HTTP (offline / coupure) : stocker les mesures en local
(SPIFFS/Preferences) et les renvoyer au retour du reseau - a implementer
dans postTelemetry (marque dans le croquis passerelle).
