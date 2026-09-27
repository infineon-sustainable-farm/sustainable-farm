package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detection du colmatage et des fuites d'un reseau goutte-a-goutte (module 6.2 de la specification).
 *
 * <p>Principe : le debit theorique d'une zone est connu par son reseau
 * ({@code debit = nombre de goutteurs x debit nominal}), donc le volume attendu sur une duree
 * d'arrosage l'est aussi. Le compteur de debit mesure le volume reellement distribue. Un volume
 * mesure nettement inferieur au volume attendu signe un colmatage (l'eau n'arrive plus aux
 * plantes), nettement superieur une fuite (l'eau part ailleurs).</p>
 *
 * <p>Le calcul ne se prononce que si les trois informations existent : reseau decrit sur la zone,
 * arrosage effectif dans la fenetre observee, mesure de debit rattachee a la zone. Sinon la
 * reponse porte un statut explicite ({@code network_not_described}, {@code no_irrigation},
 * {@code no_measurement}) : un diagnostic sans donnee vaut mieux qu'une fausse alerte.</p>
 */
@Service
@Transactional(readOnly = true)
public class DripFlowCheckService {

    /** Fenetre d'observation du debit reel (heures). */
    public static final int OBSERVATION_HOURS = 24;

    /** Sous 90 % du volume attendu, le colmatage est probable. */
    public static final double LOW_FLOW_RATIO = 0.90d;

    /** Au dessus de 110 %, une fuite est probable. */
    public static final double HIGH_FLOW_RATIO = 1.10d;

    private final ZoneRepository zoneRepository;
    private final WaterConsumptionRepository consumptionRepository;
    private final IrrigationLogRepository logRepository;
    private final AlertService alertService;

    public DripFlowCheckService(
            ZoneRepository zoneRepository,
            WaterConsumptionRepository consumptionRepository,
            IrrigationLogRepository logRepository,
            AlertService alertService) {
        this.zoneRepository = zoneRepository;
        this.consumptionRepository = consumptionRepository;
        this.logRepository = logRepository;
        this.alertService = alertService;
    }

    /**
     * Diagnostic de debit d'une zone sur la derniere fenetre d'observation.
     *
     * @throws NotFoundException si la zone n'existe pas
     */
    @Transactional
    public Map<String, Object> check(UUID zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new NotFoundException("Zone"));

        Instant now = Instant.now();
        Instant windowStart = now.minus(OBSERVATION_HOURS, ChronoUnit.HOURS);
        List<IrrigationLog> cycles = logRepository.findForZoneBetween(zoneId, windowStart, now);
        double runningHours = runningHours(cycles, now);
        double measuredLiters = consumptionRepository.sumConsumptionByZoneIdBetween(windowStart, now, zoneId);
        Double theoreticalFlow = zone.theoreticalFlowLitersPerHour();
        Double theoreticalLiters = theoreticalFlow == null ? null : theoreticalFlow * runningHours;

        String status = status(theoreticalFlow, runningHours, measuredLiters);
        Double measuredFlow = runningHours > 0 ? measuredLiters / runningHours : null;
        Double ratio = theoreticalLiters != null && theoreticalLiters > 0 ? measuredLiters / theoreticalLiters : null;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("zone_id", zone.getId());
        response.put("zone_name", zone.getName());
        response.put("irrigation_method", zone.getIrrigationMethod());
        response.put("window_start", windowStart.toString());
        response.put("window_end", now.toString());
        response.put("observation_hours", OBSERVATION_HOURS);
        response.put("emitter_count", zone.getEmitterCount());
        response.put("emitter_nominal_flow_lh", zone.getEmitterNominalFlowLh());
        response.put("theoretical_flow_lh", round(theoreticalFlow));
        response.put("measured_flow_lh", round(measuredFlow));
        response.put("running_hours", round(runningHours));
        response.put("theoretical_liters", round(theoreticalLiters));
        response.put("measured_liters", round(measuredLiters));
        response.put("ratio", ratio == null ? null : Math.round(ratio * 100d) / 100d);
        response.put("status", status);
        response.put("message", message(status, zone, measuredFlow, theoreticalFlow));
        response.put("alert_raised", raiseAlert(status, zone, ratio));
        return response;
    }

    private String status(Double theoreticalFlow, double runningHours, double measuredLiters) {
        if (theoreticalFlow == null) {
            return "network_not_described";
        }
        if (runningHours <= 0) {
            return "no_irrigation";
        }
        if (measuredLiters <= 0) {
            return "no_measurement";
        }
        double ratio = measuredLiters / (theoreticalFlow * runningHours);
        if (ratio < LOW_FLOW_RATIO) {
            return "possible_clogging";
        }
        if (ratio > HIGH_FLOW_RATIO) {
            return "possible_leak";
        }
        return "ok";
    }

    /** Duree d'arrosage cumulee des cycles demarres dans la fenetre (un cycle en cours compte jusqu'a maintenant). */
    private double runningHours(List<IrrigationLog> cycles, Instant now) {
        double hours = 0d;
        for (IrrigationLog cycle : cycles) {
            Instant start = cycle.getActualStartTime();
            Instant end = cycle.getActualEndTime() == null ? now : cycle.getActualEndTime();
            if (start == null || end.isBefore(start)) {
                continue;
            }
            hours += Duration.between(start, end).toMinutes() / 60d;
        }
        return hours;
    }

    private Double round(Double value) {
        return value == null ? null : Math.round(value * 10d) / 10d;
    }

    private String message(String status, Zone zone, Double measuredFlow, Double theoreticalFlow) {
        String name = zone.getName() == null ? "zone" : zone.getName();
        return switch (status) {
            case "network_not_described" -> "Zone \"" + name + "\" has no emitter data: record the emitter count "
                    + "and the nominal flow per emitter to enable the flow check.";
            case "no_irrigation" -> "No irrigation cycle started in the last " + OBSERVATION_HOURS
                    + " hours for zone \"" + name + "\": nothing to compare yet.";
            case "no_measurement" -> "No flow measurement is linked to zone \"" + name + "\": the flow meter "
                    + "telemetry must carry the zone_id to be compared with the network rating.";
            case "possible_clogging" -> String.format(
                    "Measured flow %.0f L/h is below %.0f%% of the network rating (%.0f L/h) for zone \"%s\": "
                            + "emitters are probably clogged and the crop is under-irrigated.",
                    measuredFlow == null ? 0d : measuredFlow, LOW_FLOW_RATIO * 100d,
                    theoreticalFlow == null ? 0d : theoreticalFlow, name);
            case "possible_leak" -> String.format(
                    "Measured flow %.0f L/h is above %.0f%% of the network rating (%.0f L/h) for zone \"%s\": "
                            + "water is probably leaking outside the crop.",
                    measuredFlow == null ? 0d : measuredFlow, HIGH_FLOW_RATIO * 100d,
                    theoreticalFlow == null ? 0d : theoreticalFlow, name);
            default -> String.format(
                    "Measured flow is consistent with the network rating for zone \"%s\" (%.0f L/h).",
                    name, measuredFlow == null ? 0d : measuredFlow);
        };
    }

    private boolean raiseAlert(String status, Zone zone, Double ratio) {
        String name = zone.getName() == null ? "zone" : zone.getName();
        String detail = String.format(
                "Zone \"%s\": measured flow is %.0f%% of the network rating over the last %d hours.",
                name, (ratio == null ? 0d : ratio) * 100d, OBSERVATION_HOURS);
        if ("possible_clogging".equals(status)) {
            return alertService.raiseOnce("warning", "Possible clogging - " + name, detail,
                    "/watersupply/maintenance").isPresent();
        }
        if ("possible_leak".equals(status)) {
            return alertService.raiseOnce("critical", "Possible leak - " + name, detail,
                    "/watersupply/maintenance").isPresent();
        }
        return false;
    }
}
