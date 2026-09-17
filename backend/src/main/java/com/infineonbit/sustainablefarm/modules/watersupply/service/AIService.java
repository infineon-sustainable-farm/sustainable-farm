package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.service.AlertService;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Logique metier du module IA (recommandations, secheresse, anomalies).
 * Separee du controleur pour la testabilite et la clarte.
 */
@Service
public class AIService {

    private final WaterSourceRepository waterSourceRepository;
    private final ZoneRepository zoneRepository;
    private final WaterConsumptionRepository waterConsumptionRepository;
    private final AlertService alertService;

    public AIService(WaterSourceRepository waterSourceRepository,
                     ZoneRepository zoneRepository,
                     WaterConsumptionRepository waterConsumptionRepository,
                     AlertService alertService) {
        this.waterSourceRepository = waterSourceRepository;
        this.zoneRepository = zoneRepository;
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.alertService = alertService;
    }

    private double reservoirLevelPercent() {
        double totalCapacity = waterSourceRepository.findAll().stream()
                .mapToDouble(s -> s.getCapacityLiters() == null ? 0 : s.getCapacityLiters()).sum();
        double totalLevel = waterSourceRepository.findAll().stream()
                .mapToDouble(s -> s.getCurrentLevelLiters() == null ? 0 : s.getCurrentLevelLiters()).sum();
        return totalCapacity <= 0 ? 0 : (totalLevel / totalCapacity) * 100;
    }

    public List<Map<String, Object>> recommendations() {
        double levelPercent = reservoirLevelPercent();
        List<Map<String, Object>> recommendations = new ArrayList<>();

        if (levelPercent < 20) {
            recommendations.add(rec("critical",
                    "Niveau des reservoirs critique (" + Math.round(levelPercent) + "%). Reduire fortement les irrigations non-essentielles.",
                    "reservoir_level"));
        } else if (levelPercent < 35) {
            recommendations.add(rec("high",
                    "Reserve d'eau faible (" + Math.round(levelPercent) + "%). Privilégier le goutte-à-goutte et reserver l'eau aux cultures critiques.",
                    "reservoir_level"));
        } else if (levelPercent < 55) {
            recommendations.add(rec("medium",
                    "Reserve d'eau moderee (" + Math.round(levelPercent) + "%). Suivre le plan d'irrigation prevu.",
                    "reservoir_level"));
        } else {
            recommendations.add(rec("low",
                    "Reserve d'eau confortable (" + Math.round(levelPercent) + "%). Les besoins en irrigation peuvent etre couverts normalement.",
                    "reservoir_level"));
        }

        long dripZones = zoneRepository.findAll().stream()
                .filter(z -> "drip".equalsIgnoreCase(z.getIrrigationMethod())
                        || "goutte-a-goutte".equalsIgnoreCase(z.getIrrigationMethod())
                        || "goutte".equalsIgnoreCase(z.getIrrigationMethod()))
                .count();
        if (dripZones > 0) {
            recommendations.add(rec("info",
                    dripZones + " zone(s) equipee(s) en goutte-à-goutte : prioriser ces zones pour maximiser l'efficience de l'eau.",
                    "drip_efficiency"));
        }

        recommendations.add(rec("info",
                "Le systeme propose des recommandations; valider et creer les irrigations via l'interface de planification.",
                "user_action"));

        return recommendations;
    }

    public Map<String, Object> droughtPrediction() {
        double levelPercent = reservoirLevelPercent();

        // 1) Suivi de consommation : moyenne quotidienne sur les 7 derniers jours.
        Instant since = Instant.now().minus(java.time.Duration.ofDays(7));
        List<WaterConsumption> recent = waterConsumptionRepository.findAll().stream()
                .filter(c -> c.getConsumptionDate() != null && c.getConsumptionDate().isAfter(since))
                .toList();
        double dailyAverage = recent.stream()
                .mapToDouble(c -> c.getConsumptionLiters() == null ? 0 : c.getConsumptionLiters())
                .sum() / 7.0;

        // 2) Quantite d eau contenue dans les reservoirs.
        double totalReserve = waterSourceRepository.findAll().stream()
                .mapToDouble(s -> s.getCurrentLevelLiters() == null ? 0 : s.getCurrentLevelLiters())
                .sum();
        double totalCapacity = waterSourceRepository.findAll().stream()
                .mapToDouble(s -> s.getCapacityLiters() == null ? 0 : s.getCapacityLiters())
                .sum();

        // 3) Jours de reserve restants au rythme de consommation observe.
        Double daysRemaining = dailyAverage > 0 ? totalReserve / dailyAverage : null;
        String daysText = daysRemaining == null ? "n/d (aucune consommation recente)"
                : String.format("%.1f", daysRemaining) + " jours";

        // 4) Risque = combinaison du niveau des reservoirs ET de la consommation observee.
        String risk;
        String advice;
        if (levelPercent < 15 || (daysRemaining != null && daysRemaining <= 1)) {
            risk = "CRITICAL";
            advice = "Penurie imminente : " + daysText + " de reserve au rythme actuel ("
                    + String.format("%.0f", dailyAverage) + " L/jour). Arreter les usages non-essentiels et mobiliser des sources de secours.";
        } else if (levelPercent < 30 || (daysRemaining != null && daysRemaining <= 3)) {
            risk = "HIGH";
            advice = "Risque eleve : environ " + daysText + " de reserve (consommation moyenne "
                    + String.format("%.0f", dailyAverage) + " L/jour). Restreindre l'irrigation aux cultures prioritaires.";
        } else if (levelPercent < 50 || (daysRemaining != null && daysRemaining <= 7)) {
            risk = "MEDIUM";
            advice = "Risque modere : environ " + daysText + " de reserve (consommation moyenne "
                    + String.format("%.0f", dailyAverage) + " L/jour). Surveiller les niveaux et maintenir le goutte-a-goutte.";
        } else {
            risk = "LOW";
            advice = "Risque faible : les reservoirs (" + String.format("%.0f", totalReserve)
                    + " L) couvrent plus de 7 jours de consommation moyenne ("
                    + String.format("%.0f", dailyAverage) + " L/jour).";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("risk_level", risk);
        result.put("reservoir_level_percentage", Math.round(levelPercent));
        result.put("total_reserve_liters", Math.round(totalReserve));
        result.put("total_capacity_liters", Math.round(totalCapacity));
        result.put("daily_average_consumption_liters_7d", Math.round(dailyAverage));
        result.put("consumption_window_days", 7);
        result.put("days_of_reserve_remaining", daysRemaining);
        result.put("confidence", 0.85);
        result.put("recommendations", advice);
        result.put("prediction_date", Instant.now().toString());
        return result;
    }

    public Map<String, Object> analyze() {
        List<WaterConsumption> consumptions = waterConsumptionRepository.findAll();
        double average = consumptions.stream()
                .mapToDouble(c -> c.getConsumptionLiters() == null ? 0 : c.getConsumptionLiters())
                .average().orElse(0);

        List<Map<String, Object>> anomalies = new ArrayList<>();
        for (WaterConsumption c : consumptions) {
            if (c.getConsumptionLiters() == null || average <= 0) {
                continue;
            }
            double deviation = (c.getConsumptionLiters() - average) / average;
            if (deviation > 0.5) {
                anomalies.add(Map.of(
                        "type", "possible_leak",
                        "message", "Consommation anormalement elevee (+" + Math.round(deviation * 100) + "% vs moyenne)",
                        "amount_liters", c.getConsumptionLiters(),
                        "date", c.getConsumptionDate() == null ? c.getCreatedAt().toString() : c.getConsumptionDate().toString()));
            }
        }

        // Notification automatique critique si des anomalies sont detectees (P4b)
        if (!anomalies.isEmpty()) {
            double totalSuspect = anomalies.stream()
                    .mapToDouble(a -> ((Number) a.get("amount_liters")).doubleValue())
                    .sum();
            alertService.raise("critical",
                    "Anomalie de consommation detectee",
                    "Detection de " + anomalies.size() + " point(s) de consommation anormale " +
                            "(ecart > 50 % vs moyenne). Volume suspecte : " + Math.round(totalSuspect) + " L. " +
                            "Verifier les vannes et les deduire.",
                    "/consumption");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("anomalies_count", anomalies.size());
        result.put("anomalies", anomalies);
        result.put("average_consumption_liters", average);
        result.put("analyzed_records", consumptions.size());
        result.put("analyzed_at", Instant.now().toString());
        return result;
    }

    private Map<String, Object> rec(String priority, String recommendation, String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("priority", priority);
        map.put("recommendation", recommendation);
        map.put("recommendation_type", type);
        map.put("confidence_score", 0.9);
        return map;
    }
}
