# Audit du module Water Supply pour integration inter-modules

Date d'audit: 2026-10-13  
Portee: `com.infineonbit.sustainablefarm.modules.watersupply`  
Regle appliquee: seules les preuves dans le code sont citees. Quand aucune preuve n'a ete trouvee, le document indique `non trouve`.

> **Mise a jour 2026-10-22** : ce document est l'audit de depart (etat au 2026-10-13). Les besoins V1-V8, T1-T4, P1-P3 et L1-L3 sont desormais implementes et testes ; l'avancement est dans le README du module (section *Integration*) et les contrats par equipe dans [`docs/integrations/`](integrations/README.md). Les numeros de ligne cites plus bas ne correspondent plus au code actuel.

## 0. Synthese

- Le module expose deja des API CRUD et dashboard pour sources, niveaux, consommations, qualite, irrigation, maintenance goutte-a-goutte, recolte de pluie, quotas, alertes, meteo et ingestion IoT.
- Les identifiants metier sont majoritairement des UUID plats (`farmId`, `fieldId`, `zoneId`, `sourceId`) et non des relations JPA directes; les contraintes SQL creent cependant plusieurs FK (ex. `water_consumption.source_id -> water_sources.id`, `zones.field_id -> fields.id`) dans `backend/src/main/resources/db/migration/V2__watersupply_constraints_and_indexes.sql:47-93`.
- Contradiction avec le contexte initial: un lien `zoneId <-> blockCode` existe maintenant dans le workspace via `ZoneBlockMapping`, endpoint `/api/water/zone-block-mappings` et migration `V11__zone_block_mapping.sql`. Preuves: `ZoneBlockMappingController.java:22-60`, `ZoneBlockMapping.java:11-26`, `V11__zone_block_mapping.sql:1-13`.
- Les alertes automatiques sont persistees comme `notifications` et lisibles via API, mais certaines exceptions d'alerte sont avalees pour ne pas bloquer la mesure. Preuves: `AlertService.java:44-77`, `NotificationController.java:33-67`, `WaterService.java:187-199`, `IotTelemetryService.java:225-230`.
- Pas de roles/authentification dans le module: le code indique que l'authentification est deleguee a la plateforme globale. Preuves: `IrrigationService.java:95-97`, `SystemUsers.java` signale cette delegation via la recherche globale, et aucun `@PreAuthorize`/`@Secured` n'a ete trouve dans le package watersupply.

## 1. Cartographie des endpoints existants

Roles/authentification pour tous les endpoints ci-dessous: `absent` dans le module, aucune annotation `@PreAuthorize`, `@Secured`, `hasRole` ou filtre local trouvee; delegation globale mentionnee par `IrrigationService.java:95-97`.  
Versionnement API `/v1`: `absent` pour Water Supply; les routes sont sous `/api`, `/api/water`, `/api/dashboard`, etc. Preuves endpoints ci-dessous.

### 1.1 WaterController - `/api/water`

| Methode | Chemin | Parametres | Entree | Sortie | Donnees de lecture exposees | Preuve |
|---|---|---|---|---|---|---|
| GET | `/api/water/sources` | `page`, `size`, `farmId` optionnels | aucune | `List<WaterSourceResponse>` ou `PageResponse<WaterSourceResponse>` | `id`, `farmId`, `name`, `type`, `capacityLiters`, `currentLevelLiters`, `latitude`, `longitude`, `createdAt` | `WaterController.java:48-55`, `WaterSourceResponse.java:7-13` |
| POST | `/api/water/sources` | aucun | `WaterSourceCreateRequest` | `WaterSourceResponse` | n/a | `WaterController.java:57-60` |
| GET | `/api/water/sources/{sourceId}` | `sourceId` | aucune | `WaterSourceResponse` | memes champs que ci-dessus | `WaterController.java:63-65`, `WaterSourceResponse.java:7-13` |
| GET | `/api/water/sources/{sourceId}/level` | `sourceId` | aucune | `WaterLevelResponse` | `sourceId`, `name`, `type`, `capacityLiters`, `currentLevelLiters`, `levelPercentage`, `status`, `rainwaterTank`, `readAt` | `WaterController.java:72-75`, `WaterLevelResponse.java:13-22`, `WaterService.java:97-109` |
| GET | `/api/water/zones/{zoneId}/flow-check` | `zoneId` | aucune | `Map<String,Object>` | cles exactes non trouvees dans controleur; service a inspecter pour contrat final | `WaterController.java:82-85` |
| PUT | `/api/water/sources/{sourceId}` | `sourceId` | `WaterSourceUpdateRequest` | `WaterSourceResponse` | n/a | `WaterController.java:88-90` |
| DELETE | `/api/water/sources/{sourceId}` | `sourceId` | aucune | 204 | n/a | `WaterController.java:93-96` |
| GET | `/api/water/consumption` | `page`, `size`, `farmId`, `sourceId` optionnels | aucune | `List<WaterConsumptionResponse>` ou `PageResponse<WaterConsumptionResponse>` | `id`, `farmId`, `sourceId`, `consumptionLiters`, `consumptionDate`, `irrigationId`, `zoneId`, `createdAt` | `WaterController.java:99-107`, `WaterConsumptionResponse.java:7-13` |
| POST | `/api/water/consumption` | aucun | `WaterConsumptionCreateRequest` | `WaterConsumptionResponse` | n/a | `WaterController.java:109-112` |
| GET | `/api/water/consumption/{consumptionId}` | `consumptionId` | aucune | `WaterConsumptionResponse` | memes champs que ci-dessus | `WaterController.java:115-117`, `WaterConsumptionResponse.java:7-13` |
| PUT | `/api/water/consumption/{consumptionId}` | `consumptionId` | `WaterConsumptionUpdateRequest` | `WaterConsumptionResponse` | n/a | `WaterController.java:120-122` |
| DELETE | `/api/water/consumption/{consumptionId}` | `consumptionId` | aucune | 204 | n/a | `WaterController.java:125-128` |
| GET | `/api/water/quality` | `page`, `size`, `sourceId` optionnels | aucune | `List<WaterQualityResponse>` ou `PageResponse<WaterQualityResponse>` | `id`, `sourceId`, `ph`, `temperatureCelsius`, `turbidityNtu`, `conductivityUsCm`, `salinityPpt`, `testDate`, `createdAt` | `WaterController.java:131-138`, `WaterQualityResponse.java:7-13` |
| POST | `/api/water/quality` | aucun | `WaterQualityCreateRequest` | `WaterQualityResponse` | n/a | `WaterController.java:145-148` |
| GET | `/api/water/quality/{qualityTestId}` | `qualityTestId` | aucune | `WaterQualityResponse` | memes champs que ci-dessus | `WaterController.java:151-153`, `WaterQualityResponse.java:7-13` |
| PUT | `/api/water/quality/{qualityTestId}` | `qualityTestId` | `WaterQualityUpdateRequest` | `WaterQualityResponse` | n/a | `WaterController.java:156-158` |
| DELETE | `/api/water/quality/{qualityTestId}` | `qualityTestId` | aucune | 204 | n/a | `WaterController.java:161-164` |

### 1.2 FarmController et FieldController - `/api`

| Methode | Chemin | Parametres | Entree | Sortie | Donnees de lecture exposees | Preuve |
|---|---|---|---|---|---|---|
| GET | `/api/farms` | `page`, `size` optionnels | aucune | `List<FarmResponse>` ou page | champs exacts DTO non lus dans cette passe; preuve DTO: `FarmResponse` | `FarmController.java:36-43` |
| POST | `/api/farms` | aucun | `FarmCreateRequest` | `FarmResponse` | n/a | `FarmController.java:46-49` |
| GET | `/api/farms/{farmId}` | `farmId` | aucune | `FarmResponse` | champs exacts DTO non lus dans cette passe | `FarmController.java:52-54` |
| PUT | `/api/farms/{farmId}` | `farmId` | `FarmUpdateRequest` | `FarmResponse` | n/a | `FarmController.java:57-59` |
| DELETE | `/api/farms/{farmId}` | `farmId` | aucune | 204 | n/a | `FarmController.java:62-65` |
| GET | `/api/farms/{farmId}/fields` | `farmId`, `page`, `size` optionnels | aucune | `List<FieldResponse>` ou page | `id`, `farmId`, `name`, `areaHectares`, autres champs DTO non cites ici | `FieldController.java:33-41`, `FieldResponse.java` via recherche DTO |
| GET | `/api/fields` | `page`, `size`, `farmId` optionnels | aucune | `List<FieldResponse>` ou page | voir ci-dessus | `FieldController.java:44-53` |
| POST | `/api/fields` | aucun | `FieldCreateRequest` | `FieldResponse` | n/a | `FieldController.java:56-59` |
| GET | `/api/fields/{fieldId}` | `fieldId` | aucune | `FieldResponse` | voir ci-dessus | `FieldController.java:62-64` |
| PUT | `/api/fields/{fieldId}` | `fieldId` | `FieldUpdateRequest` | `FieldResponse` | n/a | `FieldController.java:67-69` |
| DELETE | `/api/fields/{fieldId}` | `fieldId` | aucune | 204 | n/a | `FieldController.java:72-75` |
| GET | `/api/fields/{fieldId}/zones` | `fieldId`, `page`, `size` optionnels | aucune | `List<ZoneResponse>` ou page | `id`, `fieldId`, `name`, `areaHectares`, `irrigationMethod`, `cropCoefficient`, `emitterCount`, `emitterNominalFlowLh`, `theoreticalFlowLh`, `createdAt` | `FarmController.java:68-77`, `ZoneResponse.java:11-17` |
| GET | `/api/zones` | `page`, `size`, `fieldId` optionnels | aucune | `List<ZoneResponse>` ou page | memes champs que ci-dessus | `FarmController.java:79-88`, `ZoneResponse.java:11-17` |
| POST | `/api/zones` | aucun | `ZoneCreateRequest` | `ZoneResponse` | n/a | `FarmController.java:91-94` |
| GET | `/api/zones/{zoneId}` | `zoneId` | aucune | `ZoneResponse` | memes champs que ci-dessus | `FarmController.java:97-99`, `ZoneResponse.java:11-17` |
| PUT | `/api/zones/{zoneId}` | `zoneId` | `ZoneUpdateRequest` | `ZoneResponse` | n/a | `FarmController.java:102-104` |
| DELETE | `/api/zones/{zoneId}` | `zoneId` | aucune | 204 | n/a | `FarmController.java:107-110` |

### 1.3 IrrigationController - `/api`

| Methode | Chemin | Parametres | Entree | Sortie | Donnees de lecture exposees | Preuve |
|---|---|---|---|---|---|---|
| GET | `/api/irrigations` | `page`, `size`, `zoneId` optionnels | aucune | `List<IrrigationScheduleResponse>` ou page | `id`, `zoneId`, `startTime`, `durationMinutes`, `waterQuantityLiters`, `status`, `createdBy`, `createdAt`, `postponeReason` | `IrrigationController.java:45-51`, `IrrigationScheduleResponse.java:7-14` |
| POST | `/api/irrigations` | aucun | `IrrigationScheduleCreateRequest` | `IrrigationScheduleResponse` | n/a | `IrrigationController.java:54-57` |
| GET | `/api/irrigations/{scheduleId}` | `scheduleId` | aucune | `IrrigationScheduleResponse` | memes champs que ci-dessus | `IrrigationController.java:60-62`, `IrrigationScheduleResponse.java:7-14` |
| PUT | `/api/irrigations/{scheduleId}` | `scheduleId` | `IrrigationScheduleUpdateRequest` | `IrrigationScheduleResponse` | n/a | `IrrigationController.java:65-67` |
| DELETE | `/api/irrigations/{scheduleId}` | `scheduleId` | aucune | 204 | n/a | `IrrigationController.java:70-73` |
| POST | `/api/irrigations/auto-trigger` | aucun | aucune | `Map<String,Object>` | `check_time`, `season`, `season_threshold_percent`, `et0_mm`, `created_count`, `created`, `skipped_count`, `skipped` | `IrrigationController.java:85-88`, `IrrigationAutomationService.java:138-147` |
| GET | `/api/irrigation/suggestions` | aucun | aucune | `Map<String,Object>` | `weather_available`, `rain_probability_threshold`, `rain_amount_threshold_mm`, `suggested_water_saving_liters`, `suggestions`; par suggestion: `schedule_id`, `zone_id`, `zone_name`, `start_time`, `planned_liters`, `rain_probability`, `rain_mm`, `potential_saving_liters`, `message` | `IrrigationController.java:91-93`, `IrrigationService.java:247-254`, `IrrigationService.java:257-275` |
| POST | `/api/irrigations/{scheduleId}/postpone` | `scheduleId` | `IrrigationPostponeRequest` optionnel | `IrrigationScheduleResponse` | n/a | `IrrigationController.java:99-103`, `IrrigationService.java:282-300` |
| GET | `/api/irrigation-logs` | `page`, `size`, `scheduleId` optionnels | aucune | `List<IrrigationLogResponse>` ou page | `id`, `scheduleId`, `actualStartTime`, `actualEndTime`, `waterUsedLiters`, `status`, `createdAt` | `IrrigationController.java:106-112`, `IrrigationLogResponse.java:7-11` |
| POST | `/api/irrigation-logs` | aucun | `IrrigationLogCreateRequest` | `IrrigationLogResponse` | n/a | `IrrigationController.java:119-122` |
| GET | `/api/irrigation-logs/{logId}` | `logId` | aucune | `IrrigationLogResponse` | memes champs que ci-dessus | `IrrigationController.java:125-127`, `IrrigationLogResponse.java:7-11` |
| PUT | `/api/irrigation-logs/{logId}` | `logId` | `IrrigationLogUpdateRequest` | `IrrigationLogResponse` | n/a | `IrrigationController.java:130-132` |
| DELETE | `/api/irrigation-logs/{logId}` | `logId` | aucune | 204 | n/a | `IrrigationController.java:135-138` |
| POST | `/api/irrigations/{scheduleId}/start` | `scheduleId` | aucune | `IrrigationLogResponse` | cree un log `started`, met schedule `running` | `IrrigationController.java:141-144`, `IrrigationService.java:171-184` |
| POST | `/api/irrigations/{scheduleId}/stop` | `scheduleId` | aucune | `IrrigationLogResponse` | met schedule `completed`, log `completed` | `IrrigationController.java:147-150`, `IrrigationService.java:187-207` |

### 1.4 Maintenance, quotas, pluie, alertes, dashboard, IoT, meteo, rapports

| Controleur | Endpoints | Sortie lecture principale | Preuve |
|---|---|---|---|
| `DripMaintenanceController` | GET/POST/GET id/PUT/DELETE `/api/drip-maintenance-logs`; GET `/api/drip-maintenance-logs/schedule` | logs maintenance et calendrier preventif par zone: `zone_id`, `zone_name`, `task_type`, `frequency`, `interval_days`, `last_done`, `next_due`, `days_remaining`, `status` | `DripMaintenanceController.java:33-72`, `DripMaintenanceService.java:172-185` |
| `WaterQuotaController` | CRUD `/api/water/quotas`; GET `/api/water/quotas/usage` | quotas: `id`, `targetType`, `targetId`, `quotaMonth`, `quotaLiters`, `label`; usage: `quota_id`, `target_type`, `target_id`, `target_name`, `quota_month`, `quota_liters`, `used_liters`, `remaining_liters`, `usage_percentage`, `status` | `WaterQuotaController.java:42-83`, `WaterQuotaResponse.java:10-14`, `WaterQuotaService.java:227-239` |
| `ZoneBlockMappingController` | CRUD `/api/water/zone-block-mappings` | `id`, `zoneId`, `blockCode`, `farmId`, `createdAt` | `ZoneBlockMappingController.java:31-59`, `ZoneBlockMappingResponse.java:7-15` |
| `RainwaterHarvestController` | CRUD `/api/rainwater-harvests`; GET `/coverage` | recolte: `id`, `sourceId`, `catchmentAreaM2`, `rainfallMm`, `runoffCoefficient`, `harvestedLiters`, `captureDate`, `createdAt`; coverage cles exactes non trouvees dans controleur | `RainwaterHarvestController.java:42-81`, `RainwaterHarvestResponse.java:8-21` |
| `NotificationController` | GET/POST/GET id/PATCH read/PATCH read-all/DELETE `/api/notifications` | `id`, `userId`, `title`, `message`, `type`, `read`, `actionUrl`, `createdAt` | `NotificationController.java:33-67`, `NotificationResponse.java:7-12` |
| `WaterDashboardController` | GET `/api/dashboard/water-savings`, `/savings-series`, `/water-balance`, `/leaks`, `/kpis`, `/activities`, `/alerts` | KPI: `daily_consumption_liters`, `monthly_consumption_liters`, `tank_level_percentage`, `irrigation_count_today`, `water_savings_percentage`, `anomaly_count`, `water_quality_status`, `sensor_availability_percentage`; alert dashboard: `type`, `title`, `message`, `timestamp` | `WaterDashboardController.java:66-127`, `WaterDashboardController.java:160-185` |
| `IotTelemetryController` | POST `/api/iot/telemetry` | `List<IotTelemetryService.Result>`: `type`, `sourceId`, `status`, `message` | `IotTelemetryController.java:23-35`, `IotTelemetryService.java:40-45` |
| `WeatherController` | GET `/api/weather/current`, `/api/weather/forecast` | current: `temperature`, `humidity`, `wind_speed`, `wind_direction`, `pressure`, `clouds`, `weather_condition`, `icon`; forecast: `date`, `temperature_min`, `temperature_max`, `humidity`, `wind_speed`, `rain_probability`, `weather_condition`, `icon` | `WeatherController.java:42-58`, `WeatherController.java:60-92` |
| `AIController` | GET `/api/ai/recommendations`, `/drought-prediction`; POST `/analyze` | `Map`/`List<Map>`; cles exactes non trouvees dans controleur | `AIController.java:24-36` |
| `ReportController` | GET `/api/reports/consumption`, `/irrigation`, `/quality`, `/consumption/csv`, `/irrigation/csv`, `/quality/csv` | TXT/CSV telechargeables; CSV colonnes: consumption `date`, `source_id`, `liters`, `farm_id`; irrigation `start`, `zone_id`, `duration_min`, `liters`, `status`; quality `date`, `source_id`, `ph`, `turbidity_ntu`, `status` | `ReportController.java:62-147`, `ReportController.java:163-237` |
| `MiscController` | GET `/api/health` | `status`, `database`, `timestamp` | `MiscController.java:34-43` |
| `HomeController` | GET `/` | redirect swagger UI | `HomeController.java:6-12` |

Pagination: `partiel`. Plusieurs listes acceptent `page`/`size` et retournent `PageResponse(content,page,size,totalElements,totalPages)`; d'autres listes restent non paginees (`WaterQuotaController.quotas`, dashboard, weather). Preuves: `PageResponse.java:5`, `WaterController.java:48-55`, `WaterQuotaController.java:42-50`.

Format d'erreur standard: `existe` pour le module watersupply via envelope `timestamp`, `status`, `error`, `code`, `message`. Preuve: `WaterSupplyExceptionHandler.java:112-119`.

Documentation OpenAPI/Swagger: `partiel`. Dependency springdoc presente et Swagger UI configure; annotations `@Tag`/`@Operation` sur certains controleurs seulement. Preuves: `backend/pom.xml:73-74`, `application.properties:17-18`, `WaterController.java:36`, `IrrigationController.java:33`.

## 2. Entites, tables, relations, index

### 2.1 Tables principales

| Domaine | Entite/table | Champs principaux | Relations / contraintes | Index | Preuve |
|---|---|---|---|---|---|
| Sources | `WaterSource` / `water_sources` | `id`, `created_at`, `farm_id`, `name`, `type`, `capacity_liters`, `current_level_liters`, `latitude`, `longitude` | FK `farm_id -> farms.id` | `idx_water_sources_farm_id` | `WaterSource.java:10-34`, `V1__init.sql:44-54`, `V2__watersupply_constraints_and_indexes.sql:55-57`, `V2__watersupply_constraints_and_indexes.sql:97` |
| Consommation | `WaterConsumption` / `water_consumption` | `farm_id`, `source_id`, `consumption_liters`, `consumption_date`, `irrigation_id`, `zone_id` | FK farm, source; FK zone ajoutee en V10 | `idx_water_consumption_farm_id`, `source_id`, `date`, `zone_date` | `WaterConsumption.java:10-36`, `V1__init.sql:60-68`, `V2__watersupply_constraints_and_indexes.sql:59-65`, `V10__auto_irrigation_and_flow_check.sql:20-28` |
| Qualite | `WaterQualityTest` / `water_quality_tests` | `source_id`, `ph`, `temperature_celsius`, `turbidity_ntu`, `conductivity_us_cm`, `salinity_ppt`, `test_date` | FK `source_id -> water_sources.id` | `idx_water_quality_tests_source_id`, `idx_water_quality_tests_test_date` | `WaterQualityTest.java:10-22`, `V1__init.sql:70-80`, `V2__watersupply_constraints_and_indexes.sql:67-69`, `V2__watersupply_constraints_and_indexes.sql:101-102` |
| Pluie | `RainwaterHarvest` / `rainwater_harvests` | `source_id`, `catchment_area_m2`, `rainfall_mm`, `runoff_coefficient`, `harvested_liters`, `capture_date` | FK source | `idx_rainwater_harvests_source_id`, `idx_rainwater_harvests_capture_date` | `RainwaterHarvest.java:11-40`, `V1__init.sql:114-123`, `V2__watersupply_constraints_and_indexes.sql:87-89`, `V2__watersupply_constraints_and_indexes.sql:108-109` |
| Humidite | `SoilMoistureReading` / `soil_moisture_readings` | `zone_id`, `depth_cm`, `moisture_percent`, `measured_at` | FK zone ON DELETE CASCADE | `idx_soil_zone_measured` | `SoilMoistureReading.java:14-32`, `V7__iot_devices_and_soil_moisture.sql:18-30` |
| Maintenance | `DripMaintenanceLog` / `drip_maintenance_logs` | `zone_id`, `maintenance_date`, `maintenance_type`, `filter_cleaned`, `clogging_detected`, `clogging_severity`, `emitter_replaced_count`, `notes`, `performed_by` | FK zone | `idx_drip_maintenance_logs_zone_id`, `idx_drip_maintenance_logs_maintenance_date` | `DripMaintenanceLog.java:10-41`, `V2__watersupply_constraints_and_indexes.sql:35-45`, `V2__watersupply_constraints_and_indexes.sql:91-93`, `V2__watersupply_constraints_and_indexes.sql:110-111` |
| Zones | `Zone` / `zones` | `field_id`, `name`, `area_hectares`, `irrigation_method`, `crop_coefficient`, `emitter_count`, `emitter_nominal_flow_lh` | FK field | `idx_zones_field_id` | `Zone.java:10-44`, `V1__init.sql:35-42`, `V5__zone_crop_coefficient.sql:1-4`, `V10__auto_irrigation_and_flow_check.sql:14-16`, `V2__watersupply_constraints_and_indexes.sql:51-53` |
| Champs | `Field` / `fields` | `farm_id`, `name`, `area_hectares`, `crop_type`, `soil_type`, `coordinates` | FK farm | `idx_fields_farm_id` | `Field.java:10-29`, `V1__init.sql:24-33`, `V2__watersupply_constraints_and_indexes.sql:47-49`, `V2__watersupply_constraints_and_indexes.sql:95` |
| Fermes | `Farm` / `farms` | `name`, `description`, `address`, `latitude`, `longitude`, `area_hectares` | aucune FK sortante | non trouve | `Farm.java:8-21`, `V1__init.sql:13-22` |
| IoT | `IotDevice` / `iot_devices` | `device_id`, `type`, `last_seen`, `battery_percent`, `rssi` | `device_id` unique | `idx_iot_device_uid`, `idx_iot_last_seen` | `IotDevice.java:15-34`, `V7__iot_devices_and_soil_moisture.sql:8-16`, `V7__iot_devices_and_soil_moisture.sql:31-32` |
| Quotas | `WaterQuota` / `water_quotas` | `target_type`, `target_id`, `quota_month`, `quota_liters`, `label` | check `target_type in ('farm','zone')`, positive, unique target-month | `idx_water_quotas_target`, `idx_water_quotas_month` | `WaterQuota.java:13-35`, `V8__water_quotas.sql:1-15` |
| Alertes | `Notification` / `notifications` | `user_id`, `title`, `message`, `type`, `is_read`, `action_url` | FK user NOT VALID | `idx_notifications_user_id`, `idx_notifications_type` | `Notification.java:10-33`, `V1__init.sql:103-112`, `V2__watersupply_constraints_and_indexes.sql:83-85`, `V2__watersupply_constraints_and_indexes.sql:106-107` |
| Irrigation planifiee | `IrrigationSchedule` / `irrigation_schedules` | `zone_id`, `start_time`, `duration_minutes`, `water_quantity_liters`, `status`, `created_by`, `postpone_reason`, `trigger_source` | FK zone, FK created_by NOT VALID | `idx_irrigation_schedules_zone_id`, `idx_irrigation_schedules_start_time` | `IrrigationSchedule.java:10-44`, `V1__init.sql:82-91`, `V6__irrigation_postpone_reason.sql:1-3`, `V10__auto_irrigation_and_flow_check.sql:7-10` |
| Irrigation logs | `IrrigationLog` / `irrigation_logs` | `schedule_id`, `actual_start_time`, `actual_end_time`, `water_used_liters`, `status` | FK schedule | `idx_irrigation_logs_schedule_id` | `IrrigationLog.java:10-29`, `V1__init.sql:93-101`, `V2__watersupply_constraints_and_indexes.sql:79-81`, `V2__watersupply_constraints_and_indexes.sql:105` |
| Zone-bloc Plants | `ZoneBlockMapping` / `zone_block_mapping` | `zone_id`, `block_code`, `farm_id` | unique `(zone_id, block_code)`, FK zone, FK farm | `idx_zone_block_mapping_zone_id`, `idx_zone_block_mapping_farm_block` | `ZoneBlockMapping.java:11-26`, `V11__zone_block_mapping.sql:1-13` |

### 2.2 Historique vs derniere valeur

- Niveau reservoir: `partiel`. Seule la derniere valeur est stockee dans `WaterSource.currentLevelLiters`; l'ingestion `level` met a jour cette valeur et ne cree pas de table historique de niveaux. Preuves: `WaterSource.java:25-31`, `IotTelemetryService.java:185-213`, `WaterService.java:97-109`.
- Consommation: `existe`. Historique conserve dans `water_consumption` avec `consumption_date`. Preuves: `WaterConsumption.java:21-27`, `WaterController.java:99-117`.
- Qualite: `existe`. Historique conserve dans `water_quality_tests` avec `test_date`. Preuves: `WaterQualityTest.java:17-22`, `WaterController.java:131-153`.
- Pluie: `existe`. Historique conserve dans `rainwater_harvests` avec `capture_date`. Preuves: `RainwaterHarvest.java:33-40`, `RainwaterHarvestController.java:42-64`.
- Humidite du sol: `existe`. Historique conserve dans `soil_moisture_readings`; commentaire explicite: "Stored separately to keep the history". Preuves: `SoilMoistureReading.java:10-15`.
- Maintenance: `existe`. Logs conserves dans `drip_maintenance_logs`. Preuves: `DripMaintenanceLog.java:10-41`, `DripMaintenanceController.java:33-55`.
- IoT devices: `partiel`. Seulement etat registre `last_seen`, `battery_percent`, `rssi`; pas d'historique brut de toutes telemetries. Preuves: `IotDevice.java:10-34`, `IotDeviceService.java:27-44`.

### 2.3 Stockage des alertes

Statut: `existe`. Les alertes sont persistees dans `notifications` et lisibles via `/api/notifications` et `/api/dashboard/alerts`. Preuves: `AlertService.java:44-77`, `NotificationController.java:33-67`, `WaterDashboardController.java:174-185`.

Types d'alertes prouves:
- Quota: warning a 80%, critical a 100%. Preuves: `WaterQuotaService.java:37-41`, `WaterQuotaService.java:202-211`.
- Qualite hors plage: notification warning si pH hors `[6.0, 7.5]` ou turbidite `> 5.0`. Preuves: `WaterService.java:301-320`.
- Reservoir de pluie: critical `overflow_risk`, warning `use_rainwater_first` si besoin non-irrigation configure. Preuves: `RainwaterTankMonitor.java:73-89`.
- Automatisation irrigation: info lors d'une irrigation automatique. Preuves: `IrrigationAutomationService.java:163-168`.
- Report d'irrigation: info lors du report. Preuves: `IrrigationService.java:295-299`.

Limite: certaines exceptions d'alerte sont avalees. Preuves: `WaterService.java:187-199`, `IotTelemetryService.java:225-230`, `IotTelemetryService.java:202-212`.

## 3. Tableau de couverture

### 3.1 Visitor Management

| Besoin | Statut | Preuve | Ce qui manque |
|---|---|---|---|
| V1 niveau du reservoir en temps reel lisible via API | existe | GET `/api/water/sources/{sourceId}/level`, champs `currentLevelLiters`, `levelPercentage`, `status`, `readAt`: `WaterController.java:72-75`, `WaterLevelResponse.java:13-22` | Historique de niveau absent; `readAt` est l'heure de lecture API, pas l'heure capteur prouvee (`WaterService.java:97-109`). |
| V2 pourcentage d'eau economisee et methode de calcul | existe | `/api/dashboard/water-savings`, calcul `needLiters - actualLiters`, `savings_percentage`: `WaterDashboardController.java:66-68`, `WaterEconomyService.java:124-155`; formule besoin: `WaterNeedService.java:9-15`, `WaterNeedService.java:63-70` | Validation metier des references FAO/Kc/efficiences a confirmer; valeurs par defaut existent. |
| V3 statut d'irrigation en cours avec derniere activation | partiel | Schedules ont `status`, start/stop modifient `running`/`completed`: `IrrigationService.java:171-207`; logs exposent `actualStartTime`: `IrrigationLogResponse.java:7-11` | Pas d'endpoint dedie "current irrigation status"; necessite filtrer `/api/irrigations` et `/api/irrigation-logs`. |
| V4 compteur de consommation en direct | partiel | Flow telemetry cree une consommation: `IotTelemetryService.java:216-231`; dashboard expose consommation quotidienne/mensuelle: `WaterDashboardController.java:104-121` | Pas de compteur live par source/zone avec derniere mesure et fenetre courte; pas de streaming. |
| V5 commande d'ouverture de vanne demo | absent | non trouve; `IotDeviceService` ne fait que `recordTelemetry`, `availability`, `listDevices`: `IotDeviceService.java:27-84` | Endpoint de commande, protocole appareil, securite et audit. |
| V6 periodes planifiees de maintenance/coupure d'eau interrogeables par date | partiel | Calendrier preventif derive par `/api/drip-maintenance-logs/schedule`: `DripMaintenanceController.java:70-72`, `DripMaintenanceService.java:125-142` | Pas d'entite "planned outage" avec debut, fin, motif; calendrier derive sans filtre date. |
| V7 notification quand un groupe visite les installations | absent | non trouve dans watersupply; notifications generiques existent: `NotificationController.java:42-45`, `AlertService.java:44-57` | Evenement/polling depuis Visitor Management, payload groupe/date/zone. |
| V8 texte de securite exposable | absent | non trouve dans watersupply | Endpoint ou configuration de briefing securite; source de verite a valider avec Site Security / Visitor Management. |

### 3.2 Product Transformation

| Besoin | Statut | Preuve | Ce qui manque |
|---|---|---|---|
| T1 consommation par lot: volume, activite, source, date, ID lot | partiel | Consommation stocke `consumptionLiters`, `sourceId`, `consumptionDate`, `farmId`, `zoneId`: `WaterConsumption.java:13-36`, `WaterConsumptionResponse.java:7-13` | `lotId` absent, `activity` absent; rattachement lot/activite non trouve. |
| T2 qualite eau de lavage conforme ou non par lot/periode | partiel | Qualite par source et date; rapports qualite indiquent conforme/non conforme: `WaterQualityTest.java:13-22`, `ReportController.java:125-147` | Pas de lien lot, pas de filtre periode dans API qualite hors rapports CSV; pas de type "lavage". |
| T3 prevision disponibilite selon taille/frequence des lots | absent | non trouve | Besoin previsionnel Product Transformation + capacite source + demande future; regle metier a confirmer. |
| T4 alerte capacite tampon insuffisante reservoir | partiel | Niveau source et water balance existent: `WaterLevelResponse.java:13-22`, `WaterEconomyService.java:213-241`; alertes reservoir pluie existent: `RainwaterTankMonitor.java:73-89` | Pas d'alerte comparee a demande lot future/tampon minimal. |

### 3.3 Plants

| Besoin | Statut | Preuve | Ce qui manque |
|---|---|---|---|
| P1 rattachement irrigation et consommation a un bloc `blockCode` | partiel | Table `zone_block_mapping`, API et DTO existent: `ZoneBlockMappingController.java:31-60`, `ZoneBlockMappingResponse.java:7-15`, `V11__zone_block_mapping.sql:1-13`; irrigation/consommation sont par `zoneId`: `IrrigationSchedule.java:13-19`, `WaterConsumption.java:31-36` | Les endpoints irrigation/consommation ne retournent pas encore `blockCode`; resolver non verifie dans cette passe. |
| P2 pluie: utilisation rain gauge a la place de `localRainfallMm` null cote Plants | partiel | Telemetrie `rain` cree `RainwaterHarvest` avec `rainfall_mm`: `IotTelemetryService.java:277-292`; API rainwater existe: `RainwaterHarvestController.java:42-81` | Aucun endpoint Plants-compatible par `blockCode`/date; pas de contrat explicite avec `/api/plants/growth-calendar`. |
| P3 besoin irrigation par bloc selon `treeCount`, `growthPhase`, `ageYears` | partiel | Besoin Water Supply calcule par zone avec `area`, `ET0`, `Kc`, efficience: `WaterNeedService.java:9-15`, `WaterNeedService.java:63-70`; Plants expose croissance par bloc selon contexte fourni mais non re-verifie ici | Pas d'utilisation de `treeCount`, `growthPhase`, `ageYears` dans Water Supply; coefficients a confirmer. |

### 3.4 Logique interne

| Besoin | Statut | Preuve | Ce qui manque |
|---|---|---|---|
| L1 irrigation declenchee par seuil humidite avec pluie recente | partiel | Auto-trigger selon humidite saisonniere, pas d'irrigation ouverte, delai 24h, ET0: `IrrigationAutomationService.java:76-88`, `IrrigationAutomationService.java:107-147` | Pluie recente non prise en compte dans `trigger()`; suggestions pluie existent separement pour reports: `IrrigationService.java:210-254`. |
| L2 bilan reservoir: niveau reel moins demande prevue | partiel | Water balance expose rainwater in, irrigation out, reserve level/capacity: `WaterEconomyService.java:213-241` | Pas de champ `demand_forecast_liters` ni `available_after_forecast_liters`. |
| L3 commande appareils: IoT peut envoyer commandes ou seulement recevoir telemetrie | absent pour commande, existe pour reception | Reception via `/api/iot/telemetry`: `IotTelemetryController.java:23-35`; service IoT ne contient que enregistrement/availability/list: `IotDeviceService.java:27-84` | Mecanisme outbound MQTT/HTTP, auth, idempotence, audit commande. |

## 4. Faisabilite des besoins manquants ou partiels

Format: prerequis presents, changements, dependances, effort, risque, verdict.

### V3 - Statut irrigation en cours avec derniere activation

- Prerequis: `IrrigationSchedule.status`, endpoints start/stop, `IrrigationLog.actualStartTime` existent (`IrrigationService.java:171-207`, `IrrigationLogResponse.java:7-11`).
- Changements: endpoint `GET /api/water/visitor/irrigation-status?zoneId=` qui retourne `active`, `lastActivationAt`, `scheduleId`, `zoneId`.
- Dependances: definition fonctionnelle de "en cours" (`status=running` seulement ou log sans `actualEndTime`).
- Effort: S, aggregation simple.
- Risque: donnees incoherentes si un schedule est `running` mais le log est absent.
- Verdict: faisable avec prerequis metier.

### V4 - Compteur de consommation en direct

- Prerequis: ingestion flow, `water_consumption`, repository de sommes (`IotTelemetryService.java:216-231`, `WaterConsumptionRepository` recherche `sumConsumption...`).
- Changements: endpoint `GET /api/water/visitor/live-consumption?sourceId=&zoneId=&windowMinutes=`.
- Dependances: definition de "direct" (derniere minute, jour courant, session visite).
- Effort: S a M selon granularite.
- Risque: latence si les capteurs envoient par batch; pas d'historique brut plus fin que `water_consumption`.
- Verdict: faisable avec prerequis.

### V5 / L3 - Commande d'ouverture de vanne demo

- Prerequis: registre IoT avec `device_id`, `type`, `last_seen` (`IotDevice.java:19-34`) et endpoint de reception.
- Changements: table `iot_commands(id, device_id, command_type, payload, status, requested_by, requested_at, acknowledged_at)`, endpoint `POST /api/iot/devices/{deviceId}/commands`, service outbound MQTT/HTTP.
- Dependances: protocole reel des appareils (MQTT, HTTP, LoRa gateway), securite, liste commandes autorisees, environnement demo.
- Effort: M si simulation HTTP/local; L si integration appareil reelle.
- Risque: securite et surete operationnelle elevees; commande physique dangereuse sans auth/role/audit.
- Verdict: faisable avec prerequis; bloque pour production sans protocole et politique securite.

### V6 - Maintenance/coupure planifiee

- Prerequis: calendrier preventif derive et logs maintenance (`DripMaintenanceService.java:125-142`).
- Changements: entite `WaterOutageSchedule` ou `PlannedWaterMaintenance` avec `startTime`, `endTime`, `reason`, `zoneId/sourceId`, `status`; endpoints filtres par date.
- Dependances: qui cree les coupures, nomenclature motif, impact source/zone.
- Effort: M.
- Risque: confusion avec `DripMaintenanceLog`, qui est un historique d'interventions et non un planning bloqueur.
- Verdict: faisable avec prerequis.

### V7 - Notification groupe visite

- Prerequis: notifications persistantes et API (`NotificationController.java:33-67`, `AlertService.java:44-77`).
- Changements: endpoint interne `POST /api/water/visitor-events` ou listener applicatif; payload `visitId`, `groupName`, `expectedAt`, `zones`.
- Dependances: Visitor Management doit publier evenement ou appeler l'API.
- Effort: S en polling/API simple, M en evenement asynchrone.
- Risque: couplage inter-module; doublons si retries non idempotents.
- Verdict: faisable avec prerequis. Option la plus simple avec l'existant: push HTTP depuis Visitor Management vers Water Supply, persiste en notification.

### V8 - Texte securite

- Prerequis: aucun specifique dans Water Supply; Site Security contient des zones et exigences dans un autre module, mais hors portee principale de ce document.
- Changements: endpoint `GET /api/water/visitor/safety-briefing` ou integration Site Security; stockage de contenu versionne.
- Dependances: responsables securite/visites pour valider textes, zones autorisees/interdites, risques.
- Effort: S si texte statique; M si contenu admin/versionne.
- Risque: responsabilite juridique; contenu obsolete.
- Verdict: faisable avec prerequis metier.

### T1 - Consommation par lot et activite

- Prerequis: `WaterConsumption` stocke volume, source, date, farm, zone (`WaterConsumption.java:13-36`); flow ingestion cree les consommations (`IotTelemetryService.java:216-231`).
- Changements: ajouter `lotId` et `activity` (`washing`, `cutting`, `dryer_cleaning`) a `water_consumption`, `WaterConsumptionCreateRequest`, `WaterConsumptionUpdateRequest`, `WaterConsumptionResponse`; migration DB; filtres API.
- Dependances: Product Transformation doit fournir `lotId` et une fenetre temporelle ou declencher debut/fin activite. L'ingestion flow n'a pas naturellement de lot; rattachement realiste via fenetre temporelle fournie par Product Transformation.
- Effort: M.
- Risque: attribution erronee si plusieurs lots/activites consomment en meme temps; besoin de reconciliation.
- Verdict: faisable avec prerequis.

### T2 - Qualite conforme par lot ou periode

- Prerequis: tests qualite par source/date, statut conforme dans rapport (`ReportController.java:125-147`).
- Changements: endpoint filtre par periode et/ou lot, eventuellement `lotId` sur `WaterQualityTest` ou table d'association lot-source-date.
- Dependances: regle "eau de lavage conforme" et mapping lot -> source/intervalle.
- Effort: M.
- Risque: preuve qualite insuffisante si test ponctuel hors fenetre de lavage.
- Verdict: faisable avec prerequis.

### T3 - Prevision disponibilite selon lots

- Prerequis: niveaux sources, capacites, consommation historique, water balance (`WaterLevelResponse.java:13-22`, `WaterEconomyService.java:213-241`).
- Changements: endpoint de simulation prenant taille/frequence lots, consommation estimee par activite, horizon; nouvelles regles metier.
- Dependances: coefficients L/lot ou L/kg par activite a confirmer par Product Transformation.
- Effort: L.
- Risque: metier/data eleve; prevision trompeuse si coefficients non valides.
- Verdict: faisable avec prerequis.

### T4 / L2 - Capacite tampon insuffisante

- Prerequis: niveau/capacite source, water balance, alertes (`WaterService.java:97-109`, `WaterEconomyService.java:213-241`, `AlertService.java:44-77`).
- Changements: `buffer_min_liters` par source ou par activite, demande prevue, alerte si `currentLevel - forecastDemand < buffer`.
- Dependances: seuil tampon, horizon, demande future lots/irrigation.
- Effort: M.
- Risque: faux positifs si demandes non synchronisees.
- Verdict: faisable avec prerequis.

### P1 - Exposer blockCode sur irrigation/consommation

- Prerequis: `ZoneBlockMapping` table/API existe; `WaterConsumption.zoneId` et `IrrigationSchedule.zoneId` existent.
- Changements: enrichir DTO ou endpoints de lecture avec `blockCode`, filtre `blockCode`, resolver robuste si plusieurs mappings.
- Dependances: cardinalite validee (une zone peut-elle correspondre a plusieurs blocs ? V11 autorise unique par couple zone+block, pas unique par zone).
- Effort: S a M.
- Risque: ambiguite si multi-blocs par zone.
- Verdict: faisable avec prerequis.

### P2 - Rain gauge pour Plants

- Prerequis: telemetry `rain` et historique `rainwater_harvests` (`IotTelemetryService.java:277-292`, `RainwaterHarvest.java:14-35`).
- Changements: endpoint `GET /api/water/plants/rainfall?blockCode=&from=&to=` avec mapping block -> zone -> farm/source si necessaire.
- Dependances: Plants doit definir comment il consomme la pluie: par bloc, ferme, source, ou station.
- Effort: M.
- Risque: rattachement spatial approximatif si une jauge couvre plusieurs blocs.
- Verdict: faisable avec prerequis.

### P3 - Besoin irrigation selon treeCount/growthPhase/ageYears

- Prerequis: besoin Water Supply calcule avec zone/Kc/ET0 (`WaterNeedService.java:63-70`), mapping block existe.
- Changements: importer ou appeler `/api/plants/growth-calendar`, ajouter formule par arbre/phase/age, stocker coefficients.
- Dependances: coefficients metier a confirmer; Plants doit fournir `treeCount`, `growthPhase`, `ageYears`.
- Effort: L.
- Risque: fort risque metier si coefficients inventes.
- Verdict: faisable avec prerequis.

### L1 - Pluie recente dans auto-trigger

- Prerequis: humidite du sol, auto-trigger, rainwater history, meteo forecast (`IrrigationAutomationService.java:107-147`, `RainwaterHarvest.java:14-35`, `IrrigationService.java:210-254`).
- Changements: dans `trigger()`, exclure ou reduire volume si pluie recente/prevue; ajouter cles `recent_rain_mm`, `rain_adjustment_reason`.
- Dependances: fenetre "pluie recente" et seuils mm a confirmer.
- Effort: M.
- Risque: sous-irrigation si jauge non representative.
- Verdict: faisable avec prerequis.

## 5. Contrats API proposes

### 5.1 Visitor status irrigation

`GET /api/water/visitor/irrigation-status?zoneId={uuid}`

Reponse 200:

```json
{
  "active": true,
  "zoneId": "00000000-0000-0000-0000-000000000001",
  "scheduleId": "00000000-0000-0000-0000-000000000002",
  "status": "running",
  "lastActivationAt": "2026-10-13T10:15:00Z",
  "plannedLiters": 500.0
}
```

Erreurs: 400 `MALFORMED_REQUEST`, 404 `NOT_FOUND`, 500 `INTERNAL_ERROR` selon format existant (`WaterSupplyExceptionHandler.java:112-119`).

### 5.2 Compteur live consommation

`GET /api/water/visitor/live-consumption?sourceId={uuid}&zoneId={uuid}&windowMinutes=15`

Reponse 200:

```json
{
  "sourceId": "00000000-0000-0000-0000-000000000010",
  "zoneId": "00000000-0000-0000-0000-000000000020",
  "windowMinutes": 15,
  "liters": 123.4,
  "lastMeasurementAt": "2026-10-13T10:20:00Z"
}
```

### 5.3 Commande vanne demo

`POST /api/iot/devices/{deviceId}/commands`

Requete:

```json
{
  "command": "OPEN_VALVE",
  "durationSeconds": 30,
  "reason": "visitor_demo"
}
```

Reponse 202:

```json
{
  "commandId": "00000000-0000-0000-0000-000000000030",
  "deviceId": "valve-demo-01",
  "command": "OPEN_VALVE",
  "status": "queued",
  "requestedAt": "2026-10-13T10:25:00Z"
}
```

Erreurs specifiques proposees: 404 appareil inconnu, 409 appareil offline, 403 role insuffisant une fois auth globale branchee.

### 5.4 Planning coupures / maintenance planifiee

`GET /api/water/outages?from=2026-10-13T00:00:00Z&to=2026-10-14T00:00:00Z&zoneId={uuid}`

Reponse 200:

```json
{
  "content": [
    {
      "id": "00000000-0000-0000-0000-000000000040",
      "zoneId": "00000000-0000-0000-0000-000000000020",
      "sourceId": null,
      "startTime": "2026-10-13T12:00:00Z",
      "endTime": "2026-10-13T14:00:00Z",
      "reason": "filter_cleaning",
      "status": "planned"
    }
  ]
}
```

### 5.5 Notification visite

`POST /api/water/visitor-events`

Requete:

```json
{
  "visitId": "VIS-2026-001",
  "groupName": "School group A",
  "expectedAt": "2026-10-13T11:00:00Z",
  "zones": ["00000000-0000-0000-0000-000000000020"]
}
```

Reponse 201:

```json
{
  "notificationId": "00000000-0000-0000-0000-000000000050",
  "type": "info",
  "title": "Visitor group near water facilities"
}
```

### 5.6 Briefing securite eau

`GET /api/water/visitor/safety-briefing`

Reponse 200:

```json
{
  "version": "2026-10-13",
  "allowedZones": ["demo_path", "observation_area"],
  "forbiddenZones": ["pump_room", "chemical_storage"],
  "risks": ["slippery_floor", "pressurized_lines"],
  "text": "A confirmer par les responsables securite."
}
```

### 5.7 Consommation par lot

`GET /api/water/consumption/by-lot?lotId=LOT-001`

Reponse 200:

```json
{
  "lotId": "LOT-001",
  "totalLiters": 420.0,
  "items": [
    {
      "consumptionId": "00000000-0000-0000-0000-000000000060",
      "activity": "washing",
      "sourceId": "00000000-0000-0000-0000-000000000010",
      "liters": 250.0,
      "consumptionDate": "2026-10-13T09:00:00Z"
    }
  ]
}
```

### 5.8 Qualite par lot ou periode

`GET /api/water/quality/compliance?lotId=LOT-001&from=2026-10-13T00:00:00Z&to=2026-10-14T00:00:00Z`

Reponse 200:

```json
{
  "lotId": "LOT-001",
  "compliant": true,
  "tests": [
    {
      "qualityTestId": "00000000-0000-0000-0000-000000000070",
      "sourceId": "00000000-0000-0000-0000-000000000010",
      "ph": 6.8,
      "turbidityNtu": 2.0,
      "status": "compliant",
      "testDate": "2026-10-13T08:30:00Z"
    }
  ]
}
```

### 5.9 Prevision disponibilite lots

`POST /api/water/product-transformation/availability-forecast`

Requete:

```json
{
  "sourceId": "00000000-0000-0000-0000-000000000010",
  "horizonDays": 7,
  "batches": [
    {
      "lotId": "LOT-001",
      "activity": "washing",
      "plannedAt": "2026-10-13T09:00:00Z",
      "sizeKg": 200
    }
  ]
}
```

Reponse 200:

```json
{
  "sourceId": "00000000-0000-0000-0000-000000000010",
  "currentLevelLiters": 5000.0,
  "forecastDemandLiters": 800.0,
  "availableAfterForecastLiters": 4200.0,
  "bufferStatus": "ok"
}
```

### 5.10 Rain gauge pour Plants

`GET /api/water/plants/rainfall?blockCode=A&from=2026-10-01&to=2026-10-13`

Reponse 200:

```json
{
  "blockCode": "A",
  "rainfallMm": 34.2,
  "source": "rain_gauge",
  "readings": [
    {
      "captureDate": "2026-10-12T08:00:00Z",
      "rainfallMm": 12.0
    }
  ]
}
```

### 5.11 Besoin irrigation par bloc

`POST /api/water/plants/irrigation-need`

Requete:

```json
{
  "blockCode": "A",
  "treeCount": 120,
  "growthPhase": "flowering",
  "ageYears": 4,
  "date": "2026-10-13"
}
```

Reponse 200:

```json
{
  "blockCode": "A",
  "needLiters": 1500.0,
  "method": "tree_growth_phase",
  "parametersStatus": "a confirmer"
}
```

## 6. Risques et incoherences

- `zone_block_mapping` existe dans le workspace, alors que le contexte disait qu'il n'existait pas. Preuves: `ZoneBlockMappingController.java:22-60`, `V11__zone_block_mapping.sql:1-13`.
- Le niveau reservoir est une derniere valeur (`currentLevelLiters`) sans historique temporel. Preuve: `WaterSource.java:25-31`, `IotTelemetryService.java:185-213`.
- Les valeurs par defaut `catchment_area_m2=180.0` et `runoff_coefficient=0.8` sont appliquees silencieusement si absentes en telemetrie rain; a confirmer metier. Preuve: `IotTelemetryService.java:282-289`.
- Plusieurs seuils sont codes en dur: qualite pH 6.0-7.5 et turbidite > 5 (`WaterService.java:301-330`), humidite saisonniere 40/45/50/55 (`IrrigationAutomationService.java:76-88`), delai 24h et duree 30 min (`IrrigationAutomationService.java:42-47`), efficience systeme et Kc par defaut (`WaterNeedService.java:20-48`). Tous sont a confirmer si non deja valides.
- Des erreurs d'alerting sont avalees pour proteger l'ingestion ou la creation de consommation. Preuves: `WaterService.java:187-199`, `IotTelemetryService.java:225-230`, `IotTelemetryService.java:202-212`.
- Les DTO dashboard en `Map<String,Object>` ne donnent pas un contrat compile-time stable. Preuves: `WaterDashboardController.java:66-185`, `IrrigationAutomationService.java:138-147`.
- Les relations sont souvent des UUID plats dans les entites (`WaterConsumption.farmId/sourceId/zoneId`, `Zone.fieldId`) meme si les FKs SQL existent; cela demande une discipline stricte sur les contrats inter-modules. Preuves: `WaterConsumption.java:13-36`, `Zone.java:13-23`, `V2__watersupply_constraints_and_indexes.sql:47-93`.
- `WaterQuotaService.zoneConsumption` commente une approximation historique divisant la consommation de ferme entre zones d'un field, alors que `WaterConsumption.zoneId` existe maintenant. Preuves: `WaterQuotaService.java:263-275`, `WaterConsumptionRepository.java:37-39` via recherche repository. Risque de calcul zone quota incoherent.

## 7. Questions a poser

1. Visitor Management: quel endpoint/payload prefere pour signaler une visite pres des installations eau: push HTTP, polling, ou evenement applicatif ?
2. Visitor Management: quel texte de securite est valide, et qui en est proprietaire: Water Supply, Site Security ou Visitor Management ?
3. Visitor Management: "consommation en direct" signifie quelle fenetre: derniere mesure, 1 min, 15 min, jour courant, ou session visite ?
4. Product Transformation: quel identifiant de lot stable (`lotId`) et quel referentiel d'activites exact (`washing`, `cutting`, `dryer_cleaning`) ?
5. Product Transformation: peut-il fournir des fenetres temporelles debut/fin par lot/activite pour rattacher les volumes flow ?
6. Product Transformation: quels coefficients L/kg ou L/lot utiliser pour la prevision et la capacite tampon ?
7. Plants: cardinalite attendue entre `blockCode` et `zoneId`: 1-1, 1-n, n-n ?
8. Plants: le rain gauge doit etre consomme par bloc, par ferme, ou par station meteo/source ?
9. Plants/agronomie: quels coefficients selon `treeCount`, `growthPhase`, `ageYears`; qui les valide ?
10. IoT/equipe terrain: protocole disponible pour commandes de vanne: MQTT, HTTP, passerelle proprietaire ? Comment confirmer l'execution ?
11. Securite: quels roles peuvent lancer une commande physique ? faut-il double confirmation / simulation uniquement ?
12. Donnees: doit-on historiser les niveaux de reservoir capteur pour audit, graphiques et relecture ?

## 8. Priorites proposees

1. Stabiliser le contrat inter-modules `zoneId <-> blockCode` et exposer `blockCode` dans lectures irrigation/consommation. Bloquant pour Plants.
2. Ajouter `lotId` + `activity` sur consommation, plus endpoint par lot. Bloquant pour Product Transformation.
3. Livrer a Visitor Management un premier paquet simple: `GET /api/water/sources/{id}/level` deja disponible, `GET /api/dashboard/water-savings` deja disponible, puis ajouter `GET /api/water/visitor/irrigation-status` et `GET /api/water/visitor/live-consumption`.
4. Creer planning de coupures/maintenance planifiee distinct de `DripMaintenanceLog`.
5. Ajouter notification visite via push HTTP depuis Visitor Management vers notifications Water Supply.
6. Ajouter endpoints Plants pluie par bloc et besoin irrigation par bloc, apres validation coefficients.
7. Implementer prevision disponibilite Product Transformation et alerte capacite tampon.
8. Commander une vanne demo seulement apres protocole IoT et politique securite; commencer par simulation si besoin de demo rapide.

