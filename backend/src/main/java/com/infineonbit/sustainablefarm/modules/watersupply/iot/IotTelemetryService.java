package com.infineonbit.sustainablefarm.modules.watersupply.iot;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.SoilMoistureReading;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.SoilMoistureReadingRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.service.DripMaintenanceService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.IotDeviceService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.RainwaterHarvestService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.WaterQuotaService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.WaterService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Routage des telemetries capteurs IoT vers les tables metiers du module.
 * Une mesure inconnue ou une valeur manquante n'interrompt jamais le lot :
 * chaque item repond individuellement (processed / rejected / not_routed_yet).
 */
@Service
public class IotTelemetryService {

    /** Record d'entree : une mesure d'un appareil. */
    public record Telemetry(String deviceId, String type, UUID sourceId, UUID zoneId,
                            Map<String, Object> values, Instant timestamp) {}

    /** Resultat individuel du routage. */
    public record Result(String type, UUID sourceId, String status, String message) {}

    private final WaterSourceRepository waterSourceRepository;
    private final WaterService waterService;
    private final RainwaterHarvestService rainwaterHarvestService;
    private final DripMaintenanceService dripMaintenanceService;
    private final SoilMoistureReadingRepository soilMoistureReadingRepository;
    private final IotDeviceService iotDeviceService;
    private final WaterQuotaService waterQuotaService;
    private final TransactionTemplate itemTransaction;

    public IotTelemetryService(WaterSourceRepository waterSourceRepository,
                               WaterService waterService,
                               RainwaterHarvestService rainwaterHarvestService,
                               DripMaintenanceService dripMaintenanceService,
                               SoilMoistureReadingRepository soilMoistureReadingRepository,
                               IotDeviceService iotDeviceService,
                               WaterQuotaService waterQuotaService,
                               PlatformTransactionManager transactionManager) {
        this.waterSourceRepository = waterSourceRepository;
        this.waterService = waterService;
        this.rainwaterHarvestService = rainwaterHarvestService;
        this.dripMaintenanceService = dripMaintenanceService;
        this.soilMoistureReadingRepository = soilMoistureReadingRepository;
        this.iotDeviceService = iotDeviceService;
        this.waterQuotaService = waterQuotaService;
        TransactionTemplate perItem = new TransactionTemplate(transactionManager);
        perItem.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.itemTransaction = perItem;
    }

    @SuppressWarnings("unchecked")
    public Telemetry parse(Object raw) {
        if (!(raw instanceof Map<?, ?> nodeMap)) {
            throw new IllegalArgumentException("Corps JSON attendu : objet de mesure");
        }
        Map<String, Object> node = (Map<String, Object>) nodeMap;
        String deviceId = node.get("device_id") == null ? null : String.valueOf(node.get("device_id"));
        String type = node.get("type") == null ? null : String.valueOf(node.get("type"));
        UUID sourceId = node.get("source_id") == null ? null : UUID.fromString(String.valueOf(node.get("source_id")));
        UUID zoneId = node.get("zone_id") == null ? null : UUID.fromString(String.valueOf(node.get("zone_id")));
        Instant timestamp = node.get("timestamp") == null ? Instant.now()
                : Instant.parse(String.valueOf(node.get("timestamp")));
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        if (node.get("values") instanceof Map<?, ?> v) {
            values.putAll((Map<String, Object>) v);
        }
        return new Telemetry(deviceId, type, sourceId, zoneId, values, timestamp);
    }

    /** Telemetry synthetique pour signaler une erreur de parsing sans faire echouer le lot. */
    public Telemetry errorTelemetry(Exception e) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        return new Telemetry(null, "?", null, null, values, Instant.now());
    }

    /**
     * Ingestion d'un lot. Chaque mesure est traitee dans SA PROPRE transaction : une mesure
     * invalide ne peut donc plus marquer la transaction du lot en rollback-only, ce qui
     * provoquait un 500 (UnexpectedRollbackException) au commit au lieu d'un simple rejet.
     */
    public List<Result> ingest(List<Telemetry> batch) {
        List<Result> results = new ArrayList<>();
        for (Telemetry t : batch) {
            results.add(ingestOne(t));
        }
        return results;
    }

    private Result ingestOne(Telemetry t) {
        // Une mesure illisible (JSON invalide) porte le motif du parseur : on le remonte tel quel.
        Object parseError = t.values() == null ? null : t.values().get("error");
        if (parseError != null) {
            return rejected(t, String.valueOf(parseError));
        }
        // Sans identifiant de capteur, aucune ecriture n'a de sens : on rejette avant toute requete.
        if (t.deviceId() == null || t.deviceId().isBlank()) {
            return rejected(t, "Champ 'device_id' manquant");
        }
        try {
            return itemTransaction.execute(status -> {
                // Enregistrement du capteur (P7) : last_seen mis à jour à chaque ingestion
                iotDeviceService.recordTelemetry(t.deviceId(), t.type(), t.timestamp(),
                        batteryFromValues(t.values()), rssiFromValues(t.values()));
                return route(t);
            });
        } catch (Exception e) {
            return rejected(t, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private Result rejected(Telemetry t, String message) {
        return new Result(t.type() == null ? "?" : t.type(), t.sourceId(), "rejected", message);
    }

    private Integer batteryFromValues(Map<String, Object> values) {
        if (values == null) return null;
        Object v = values.get("battery_percent");
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Integer rssiFromValues(Map<String, Object> values) {
        if (values == null) return null;
        Object v = values.get("rssi");
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private Result route(Telemetry t) {
        if (t.type() == null || "?".equals(t.type())) {
            Object err = t.values() != null ? t.values().get("error") : null;
            return new Result("?", t.sourceId(), "rejected",
                    err != null ? String.valueOf(err) : "Champ 'type' manquant");
        }
        return switch (t.type().toLowerCase()) {
            case "level" -> routeLevel(t);
            case "flow" -> routeFlow(t);
            case "quality" -> routeQuality(t);
            case "rain" -> routeRain(t);
            case "clogging" -> routeClogging(t);
            case "soil" -> routeSoil(t);
            default -> new Result(t.type(), t.sourceId(), "rejected", "Type de mesure inconnu");
        };
    }

    private Result routeLevel(Telemetry t) {
        WaterSource source = requireSource(t.sourceId());
        Double liters = doubleValue(t, "level_liters");
        if (liters == null) {
            Double percent = doubleValue(t, "level_percent");
            if (percent == null || source.getCapacityLiters() == null) {
                return new Result("level", t.sourceId(), "rejected", "Valeur 'level_liters' (ou 'level_percent') manquante");
            }
            liters = source.getCapacityLiters() * percent / 100.0;
        }
        source.setCurrentLevelLiters(liters);
        waterSourceRepository.save(source);
        return new Result("level", t.sourceId(), "processed", "Niveau reservoir = " + liters + " L");
    }

    private Result routeFlow(Telemetry t) {
        Double liters = doubleValue(t, "flow_liters");
        if (liters == null) {
            return new Result("flow", t.sourceId(), "rejected", "Valeur 'flow_liters' manquante");
        }
        WaterSource source = requireSource(t.sourceId());
        java.time.Instant measuredAt = t.timestamp() == null ? Instant.now() : t.timestamp();
        waterService.createConsumption(new WaterConsumptionCreateRequest(
                source.getFarmId(), source.getId(), liters, measuredAt, null));
        // Alerte quota (80 % / 100 %) : l'ingestion capteur est le chemin principal des mesures.
        try {
            waterQuotaService.checkThresholds(source.getFarmId(), t.zoneId(), measuredAt);
        } catch (RuntimeException ignored) {
            // La mesure est enregistree meme si l'evaluation de quota echoue.
        }
        return new Result("flow", t.sourceId(), "processed", "Consommation enregistree : " + liters + " L");
    }

    private Result routeQuality(Telemetry t) {
        Double ph = doubleValue(t, "ph");
        Double ntu = doubleValue(t, "turbidity_ntu");
        if (ph == null && ntu == null) {
            return new Result("quality", t.sourceId(), "rejected", "Valeurs 'ph' ou 'turbidity_ntu' manquantes");
        }
        waterService.createQualityTest(new WaterQualityCreateRequest(
                t.sourceId(),
                ph,
                doubleValue(t, "temperature_celsius"),
                ntu,
                doubleValue(t, "conductivity_us_cm"),
                doubleValue(t, "salinity_ppt"),
                t.timestamp() == null ? Instant.now() : t.timestamp()));
        boolean outOfRange = (ph != null && (ph < 6.0 || ph > 7.5)) || (ntu != null && ntu > 5.0);
        return new Result("quality", t.sourceId(), "processed",
                outOfRange ? "Mesure HORS SEUIL enregistree (alerte generee)" : "Mesure conforme enregistree");
    }
    private Result routeSoil(Telemetry t) {
        if (t.zoneId() == null) {
            return new Result("soil", t.sourceId(), "rejected", "Champ 'zone_id' manquant");
        }
        Double moisture = doubleValue(t, "moisture_percent");
        if (moisture == null) {
            return new Result("soil", t.sourceId(), "rejected", "Valeur 'moisture_percent' manquante");
        }
        if (moisture < 0 || moisture > 100) {
            return new Result("soil", t.sourceId(), "rejected", "'moisture_percent' hors plage (0-100)");
        }
        SoilMoistureReading reading = new SoilMoistureReading();
        reading.setZoneId(t.zoneId());
        Integer depth = t.values() != null && t.values().get("depth_cm") instanceof Number n
                ? n.intValue() : null;
        reading.setDepthCm(depth == null || depth < 0 ? 10 : depth);
        reading.setMoisturePercent(moisture);
        reading.setMeasuredAt(t.timestamp() == null ? Instant.now() : t.timestamp());
        soilMoistureReadingRepository.save(reading);
        return new Result("soil", t.zoneId(), "processed",
                "Humidite du sol = " + Math.round(moisture) + " % (zone " + t.zoneId() + ")");
    }



    private Result routeRain(Telemetry t) {
        Double mm = doubleValue(t, "rainfall_mm");
        if (mm == null) {
            return new Result("rain", t.sourceId(), "rejected", "Valeur 'rainfall_mm' manquante");
        }
        RainwaterHarvest harvest = new RainwaterHarvest();
        harvest.setSourceId(requireSource(t.sourceId()).getId());
        Double area = doubleValue(t, "catchment_area_m2");
        harvest.setCatchmentAreaM2(area == null ? 180.0 : area);
        harvest.setRainfallMm(mm);
        harvest.setRunoffCoefficient(doubleValue(t, "runoff_coefficient") == null ? 0.8 : doubleValue(t, "runoff_coefficient"));
        harvest.setCaptureDate(t.timestamp() == null ? Instant.now() : t.timestamp());
        RainwaterHarvest saved = rainwaterHarvestService.create(harvest);
        return new Result("rain", t.sourceId(), "processed",
                "Recolte pluviale enregistree : " + saved.getHarvestedLiters() + " L");
    }

    private Result routeClogging(Telemetry t) {
        if (t.zoneId() == null) {
            return new Result("clogging", t.sourceId(), "rejected", "Champ 'zone_id' manquant");
        }
        DripMaintenanceLog log = new DripMaintenanceLog();
        log.setZoneId(t.zoneId());
        log.setMaintenanceType("cleaning");
        log.setMaintenanceDate(t.timestamp() == null ? Instant.now() : t.timestamp());
        log.setFilterCleaned(false);
        log.setCloggingDetected(true);
        String severity = t.values() != null && t.values().get("severity") != null
                ? String.valueOf(t.values().get("severity")).toLowerCase() : "medium";
        log.setCloggingSeverity(severity);
        log.setEmitterReplacedCount(0);
        log.setNotes("Detection capteur IoT - debit anormal (" + severity + ")");
        log.setPerformedBy(t.deviceId() == null ? "Capteur IoT" : t.deviceId());
        dripMaintenanceService.create(log);
        return new Result("clogging", t.zoneId(), "processed", "Colmatage " + severity + " enregistre");
    }

    private WaterSource requireSource(UUID sourceId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("Champ 'source_id' manquant");
        }
        return waterSourceRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source inconnue : " + sourceId));
    }

    private Double doubleValue(Telemetry t, String key) {
        if (t.values() == null) {
            return null;
        }
        Object raw = t.values().get(key);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}