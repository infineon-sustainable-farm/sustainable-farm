package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.RainwaterHarvestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mesure de l'économie d'eau du domaine : c'est la métrique de valeur du module.
 *
 * <p>Principe agronomique (FAO-56) : la référence n'est pas le planning saisi à la main mais le
 * <strong>besoin des cultures</strong>, calculé à partir de l'évapotranspiration réelle du lieu
 * (ET0, récupérée par {@link AgroWeatherService}) :
 * {@code besoin = surface x ET0 x Kc / efficacité du système} (voir {@link WaterNeedService}).</p>
 *
 * <p>L'écart entre ce besoin et la consommation réellement mesurée par les capteurs de débit
 * représente l'eau économisée (ou perdue). Le service expose aussi :</p>
 * <ul>
 *   <li>la série cumulée de l'économie (progression dans le temps) ;</li>
 *   <li>le bilan hydrique entrées / sorties, qui rend visibles les pertes ;</li>
 *   <li>la part de pluie réutilisée et le volume évité par les reports météo.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class WaterEconomyService {

    private final ZoneRepository zoneRepository;
    private final IrrigationScheduleRepository scheduleRepository;
    private final WaterConsumptionRepository consumptionRepository;
    private final WaterSourceRepository sourceRepository;
    private final RainwaterHarvestRepository harvestRepository;
    private final WaterNeedService waterNeedService;
    private final AgroWeatherService agroWeatherService;

    public WaterEconomyService(
            ZoneRepository zoneRepository,
            IrrigationScheduleRepository scheduleRepository,
            WaterConsumptionRepository consumptionRepository,
            WaterSourceRepository sourceRepository,
            RainwaterHarvestRepository harvestRepository,
            WaterNeedService waterNeedService,
            AgroWeatherService agroWeatherService) {
        this.zoneRepository = zoneRepository;
        this.scheduleRepository = scheduleRepository;
        this.consumptionRepository = consumptionRepository;
        this.sourceRepository = sourceRepository;
        this.harvestRepository = harvestRepository;
        this.waterNeedService = waterNeedService;
        this.agroWeatherService = agroWeatherService;
    }

    /** Début de la période demandée (day, week, month). */
    public LocalDate periodStartDate(String period) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (period == null ? "month" : period.toLowerCase()) {
            case "day" -> today;
            case "week" -> today.minusDays(6);
            case "year" -> today.withDayOfYear(1);
            default -> today.withDayOfMonth(1);
        };
    }

    /**
     * ET0 moyenne de la période, ou valeur de repli si la météo est momentanément indisponible :
     * un tableau de bord ne doit jamais échouer à cause d'un service externe.
     */
    public double averageEt0(LocalDate from, LocalDate to) {
        try {
            return agroWeatherService.et0Between(from, to);
        } catch (RuntimeException ex) {
            return AgroWeatherService.FALLBACK_ET0_MM;
        }
    }

    /** Besoin hydrique quotidien de l'ensemble des zones pour une ET0 donnée. */
    public double dailyNeedLiters(double et0Mm) {
        return zoneRepository.findAll().stream()
                .mapToDouble(zone -> waterNeedService.needLiters(zone, et0Mm))
                .sum();
    }

    /** Détail par zone du besoin théorique : surface, Kc, efficacité et méthode d'irrigation. */
    public List<Map<String, Object>> needBreakdown(double et0Mm) {
        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (Zone zone : zoneRepository.findAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("zone_id", zone.getId());
            item.put("zone_name", zone.getName());
            item.put("area_hectares", zone.getAreaHectares());
            item.put("crop_coefficient", waterNeedService.cropCoefficient(zone));
            item.put("system_efficiency", waterNeedService.systemEfficiency(zone));
            item.put("irrigation_method", zone.getIrrigationMethod());
            item.put("irrigation_method_label", waterNeedService.methodLabel(zone));
            item.put("daily_need_liters", Math.round(waterNeedService.needLiters(zone, et0Mm)));
            breakdown.add(item);
        }
        return breakdown;
    }

    /**
     * Bilan d'économie d'eau de la période : besoin théorique des cultures confronté à la
     * consommation réellement mesurée par les capteurs de débit.
     *
     * <p>C'est la mesure de valeur du module. Un solde positif signifie que la consommation est
     * restée sous le besoin des cultures (eau économisée) ; un solde négatif signale une
     * sur-irrigation (eau perdue, à corriger en priorité).</p>
     */
    public Map<String, Object> savings(String period) {
        LocalDate from = periodStartDate(period);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long days = Math.max(1, ChronoUnit.DAYS.between(from, today) + 1);
        double et0Mm = averageEt0(from, today);

        double dailyNeed = dailyNeedLiters(et0Mm);
        double needLiters = dailyNeed * days;
        double actualLiters = consumptionRepository.sumConsumptionBetween(
                from.atStartOfDay().toInstant(ZoneOffset.UTC), Instant.now());
        double rainwaterLiters = harvestedBetween(from, today);

        double savedLiters = needLiters - actualLiters;
        double savingsPercent = needLiters <= 0 ? 0 : Math.round((savedLiters / needLiters) * 100);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("period", period);
        response.put("period_start", from.toString());
        response.put("period_days", days);
        response.put("et0_mm", round(et0Mm));
        response.put("daily_need_liters", Math.round(dailyNeed));
        response.put("need_liters", Math.round(needLiters));
        response.put("actual_liters", Math.round(actualLiters));
        response.put("saved_liters", Math.round(savedLiters));
        response.put("savings_percentage", savingsPercent);
        response.put("over_irrigation_liters", Math.round(Math.max(0, -savedLiters)));
        response.put("rainwater_reused_liters", Math.round(rainwaterLiters));
        response.put("rainwater_reused_percentage", needLiters <= 0 ? 0
                : Math.round((rainwaterLiters / needLiters) * 100));
        response.put("postponed_liters", Math.round(postponedLitersBetween(from, today)));
        response.put("need_breakdown", needBreakdown(et0Mm));
        return response;
    }

    /**
     * Série cumulée de l'économie d'eau, jour par jour : c'est la courbe qui montre la progression
     * dans le temps (litres économisés depuis le début de la fenêtre).
     */
    public Map<String, Object> savingsSeries(int days) {
        int window = Math.min(60, Math.max(1, days));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = today.minusDays(window - 1L);

        List<Map<String, Object>> points = new ArrayList<>();
        double cumulativeNeed = 0d;
        double cumulativeActual = 0d;

        for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
            double et0Mm = averageEt0(day, day);
            double need = dailyNeedLiters(et0Mm);
            double actual = consumptionRepository.sumConsumptionBetween(
                    day.atStartOfDay().toInstant(ZoneOffset.UTC),
                    day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));

            cumulativeNeed += need;
            cumulativeActual += actual;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.toString());
            point.put("et0_mm", round(et0Mm));
            point.put("need_liters", Math.round(need));
            point.put("actual_liters", Math.round(actual));
            point.put("rainwater_liters", Math.round(harvestedBetween(day, day)));
            point.put("saved_liters", Math.round(need - actual));
            point.put("cumulative_need_liters", Math.round(cumulativeNeed));
            point.put("cumulative_actual_liters", Math.round(cumulativeActual));
            point.put("cumulative_saved_liters", Math.round(cumulativeNeed - cumulativeActual));
            points.add(point);
        }

        double totalSaved = cumulativeNeed - cumulativeActual;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("window_days", window);
        response.put("from", from.toString());
        response.put("to", today.toString());
        response.put("total_saved_liters", Math.round(totalSaved));
        response.put("total_need_liters", Math.round(cumulativeNeed));
        response.put("total_actual_liters", Math.round(cumulativeActual));
        response.put("savings_percentage", cumulativeNeed <= 0 ? 0
                : Math.round((totalSaved / cumulativeNeed) * 100));
        response.put("points", points);
        return response;
    }

    /**
     * Bilan hydrique : entrées (pluie récupérée) et sorties (eau consommée), comparés au niveau des
     * réservoirs. Il rend visible l'eau qui n'arrive ni à la culture ni au stock, donc les pertes.
     */
    public Map<String, Object> waterBalance(String period) {
        LocalDate from = periodStartDate(period);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        double rainwaterIn = harvestedBetween(from, today);
        double irrigationOut = consumptionRepository.sumConsumptionBetween(
                from.atStartOfDay().toInstant(ZoneOffset.UTC), Instant.now());

        double capacity = sourceRepository.findAll().stream()
                .mapToDouble(source -> source.getCapacityLiters() == null ? 0 : source.getCapacityLiters())
                .sum();
        double currentLevel = sourceRepository.findAll().stream()
                .mapToDouble(source -> source.getCurrentLevelLiters() == null ? 0 : source.getCurrentLevelLiters())
                .sum();
        double unbalanced = Math.max(0, irrigationOut - rainwaterIn);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("period", period);
        response.put("period_start", from.toString());
        response.put("rainwater_in_liters", Math.round(rainwaterIn));
        response.put("irrigation_out_liters", Math.round(irrigationOut));
        response.put("reserve_level_liters", Math.round(currentLevel));
        response.put("reserve_capacity_liters", Math.round(capacity));
        response.put("reserve_level_percentage",
                capacity <= 0 ? 0 : Math.round((currentLevel / capacity) * 100));
        response.put("unbalanced_liters", Math.round(unbalanced));
        response.put("balance_status", capacity <= 0 ? "inconnu"
                : (currentLevel / capacity) >= 0.30 ? "equilibre" : "deficit");
        return response;
    }

    /** Volume d'eau de pluie récupéré sur un intervalle de dates (entrée du bilan). */
    private double harvestedBetween(LocalDate from, LocalDate to) {
        Instant startInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return harvestRepository.findAll().stream()
                .filter(harvest -> harvest.getCaptureDate() != null
                        && !harvest.getCaptureDate().isBefore(startInstant)
                        && harvest.getCaptureDate().isBefore(endInstant))
                .mapToDouble(harvest -> harvest.getHarvestedLiters() == null ? 0 : harvest.getHarvestedLiters())
                .sum();
    }

    /**
     * Volume évité grâce aux irrigations reportées : c'est l'économie obtenue en n'arrosant pas
     * quand la pluie annoncée couvrait le besoin de la culture.
     */
    public double postponedLitersBetween(LocalDate from, LocalDate to) {
        Instant startInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return scheduleRepository.findAll().stream()
                .filter(schedule -> "postponed".equalsIgnoreCase(schedule.getStatus())
                        && schedule.getStartTime() != null
                        && !schedule.getStartTime().isBefore(startInstant)
                        && schedule.getStartTime().isBefore(endInstant))
                .mapToDouble(schedule -> schedule.getWaterQuantityLiters() == null
                        ? 0 : schedule.getWaterQuantityLiters())
                .sum();
    }

    private double round(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private double sum(List<Double> values) {
        return values.stream().filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
    }
}