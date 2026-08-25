package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.time.Instant;
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

    public AIService(WaterSourceRepository waterSourceRepository,
                     ZoneRepository zoneRepository,
                     WaterConsumptionRepository waterConsumptionRepository) {
        this.waterSourceRepository = waterSourceRepository;
        this.zoneRepository = zoneRepository;
        this.waterConsumptionRepository = waterConsumptionRepository;
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
        String risk;
        String advice;
        if (levelPercent < 15) {
            risk = "CRITICAL";
            advice = "Peniere d'eau imminente. Arreter les usages non-essentiels et mobiliser des sources de secours.";
        } else if (levelPercent < 30) {
            risk = "HIGH";
            advice = "Risque eleve. Restreindre l'irrigation aux cultures prioritaires et annuler les irrigations reportables.";
        } else if (levelPercent < 50) {
            risk = "MEDIUM";
            advice = "Risque moyen. Surveiller le niveau des reservoirs et maintenir le goutte-à-goutte.";
        } else {
            risk = "LOW";
            advice = "Risque faible. Les ressources en eau sont suffisantes pour la periode.";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("risk_level", risk);
        result.put("reservoir_level_percentage", Math.round(levelPercent));
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
