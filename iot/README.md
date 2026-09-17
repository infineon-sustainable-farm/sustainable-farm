# IoT - Water Supply (integration systeme reel)

> Ce dossier contient tout ce qui concerne l integration **obligatoire** du systeme IoT :
> analyse des fonctionnalites capteurs, contrat d ingestion, architecture ESP32 et firmware.
> **L endpoint d ingestion est implemente et valide** : POST /api/iot/telemetry (backend watersupply).

## 1. Architecture retenue

```
[ Capteurs terrain ]  --fils-->  [ ESP32 passerelle de zone ]  --Wi-Fi/HTTP JSON-->  [ Backend Spring ]  -->  [ PostgreSQL ]  -->  [ Frontend React ]
 niveau, debit,                                            POST /api/iot/telemetry        routage vers            historiques,
 pluie, sol, qualite                                       (objet unique ou lot)          les tables metiers     graphiques, alertes
```

- **Un ESP32 par zone/site** : tous les capteurs de la zone sont cables sur ses GPIO.
- L ESP32 lit les capteurs, horodate (ou laisse le backend horodater), envoie en JSON.
- Une mesure illisible ne fait jamais echouer le lot : le backend repond 202 avec le statut de chaque mesure (`processed` / `rejected` / `not_routed_yet`).
- L authentification des appareils sera assuree par le futur **logiciel global** (hors perimetre du module).

## 2. Analyse : quelles fonctionnalites dependent des capteurs ?

### A. Mesures capteur (ingestion automatique - l UI ne saisit PAS)

| # | Mesure | Capteur | Endpoint d ingestion | Impact UI | Scenarios |
|---|---|---|---|---|---|
| A1 | Niveau de reservoir | HC-SR04 ultrason | `level` -> `water_sources.current_level_liters` | /sources (% niveau), /pluvial (jauge), / | SC-03, SC-06 |
| A2 | Debit / volume consomme | YF-S201 a impulsions | `flow` -> `water_consumption` | /consommation (graphique, LECTURE SEULE), / KPIs | SC-01, SC-10, SC-12 |
| A3 | Pluviometrie | Auget basculant | `rain` -> `rainwater_harvests` (volume calcule par le backend) | /pluvial | SC-06, SC-12 |
| A4 | Qualite de l eau | Sonde pH + turbidite + DS18B20 | `quality` -> `water_quality_tests` (+ notification auto si hors seuil) | /qualite (alertes) | SC-05 |
| A5 | Humidite du sol | Sonde capacitive | `soil` -> accepte, routage a venir (decision d architecture) | futur scheduler irrigation | SC-01, SC-02 |
| A6 | Debit goutte-a-goutte anormal | Capteur pression/debit | `clogging` -> `drip_maintenance_logs` (colmatage) | /goutte-a-goutte | SC-07 |
| A7 | Disponibilite passerelle / alimentation | heartbeat + batterie | `gateway` -> accepte, routage a venir | badges Offline / Power outage | SC-11, SC-13 |

### B. Donnees humaines (saisie manuelle legitime - pas d IoT)

| Donnee | Ecran |
|---|---|
| Fermes / Champs / Zones (config site) | /fermes |
| Sources (type, capacite) | /sources |
| Plannings d irrigation + start/stop | /irrigation |
| Interventions de maintenance (visites) | /goutte-a-goutte |

### C. Decision appliquee
- La consommation est en **LECTURE SEULE** dans l UI (les volumes viennent des capteurs A2) : le CRUD manuel a ete retire.

## 3. Contrat d ingestion (IMPLEMENTE ET VALIDE)

```
POST /api/iot/telemetry
Content-Type: application/json
```

Objet unique **ou** tableau (lot). Champs :

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| device_id | string | oui | identifiant de l ESP32 |
| type | string | oui | level - flow - quality - rain - clogging - soil - gateway |
| source_id | uuid | selon type | source concernee (level, flow, quality, rain) |
| zone_id | uuid | selon type | zone concernee (clogging, soil) |
| values | object | oui | valeurs mesurees (voir exemples) |
| timestamp | ISO-8601 | non | le backend horodate maintenant si absent |

Exemples reels (testes et valides sur cette base) :

```json
{"device_id":"esp32-a-01","type":"level","source_id":"<uuid>","values":{"level_percent":15}}
{"device_id":"esp32-a-01","type":"flow","source_id":"<uuid>","values":{"flow_liters":250}}
{"device_id":"sonde-ph-01","type":"quality","source_id":"<uuid>","values":{"ph":5.2,"turbidity_ntu":8.4,"temperature_celsius":20}}
{"device_id":"pluviometre-01","type":"rain","source_id":"<uuid>","values":{"rainfall_mm":15,"catchment_area_m2":180}}
{"device_id":"capteur-debit-01","type":"clogging","zone_id":"<uuid>","values":{"severity":"high"}}
```

Reponse : `202 Accepted` avec le statut de chaque mesure, ex :

```json
[
  {"type":"level","source_id":"...","status":"processed","message":"Niveau reservoir = 1500.0 L"},
  {"type":"quality","source_id":"...","status":"processed","message":"Mesure HORS SEUIL enregistree (alerte generee)"},
  {"type":"soil","status":"not_routed_yet","message":"Mesure acceptee - routage a venir"}
]
```

Frequences prevues : niveau 1 min - debit a chaque impulsion (vidage 1 min) - pluie 1 min - qualite 15 min - sol 10 min - heartbeat 30 s.

## 4. Firmware

Voir `iot/firmware/` :
- `firmware/esp32_gateway/esp32_gateway.ino` : **passerelle principale** - tous les capteurs cabled sur une seule ESP32, envoi periodique par type de mesure.
- Dossiers `water_level`, `flow_meter`, `water_quality`, `rain_gauge`, `soil_moisture` : croquis mono-capteur (nODEs simples alternatifs).
- Table de cablage et calibration detaillees dans `firmware/README.md`.

## 5. Verifications effectuees (2026-09-16)

Lot de telemetries reel envoye au backend en marche : level, flow, quality, rain, clogging -> tous `processed` ;
soil -> `not_routed_yet` (accepte). Effets verifies directement en SQL :
niveau reservoir mis a jour, consommation enregistree, test qualite hors seuil (notification generee),
recolte pluviale calculee par le backend (180 m2 x 15 mm x 0.8 = 2160 L), intervention de colmatage creee.
