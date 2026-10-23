package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.RainwaterHarvestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
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
 * Measurement of the farm's water savings: this is the module's value metric.
 *
 * <p>Agronomic principle (FAO-56): the reference is not the hand-entered schedule but the
 * <strong>crops' water need</strong>, computed from the site's actual evapotranspiration
 * (ET0, fetched by {@link AgroWeatherService}):
 * {@code need = area x ET0 x Kc / system efficiency} (see {@link WaterNeedService}).</p>
 *
 * <p>The gap between that need and the consumption actually measured by the flow sensors
 * represents the water saved (or lost). The service also exposes:</p>
 * <ul>
 *   <li>the cumulative savings series (progress over time);</li>
 *   <li>the in / out water balance, which makes losses visible;</li>
 *   <li>the share of reused rainwater and the volume avoided by weather postponements.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class WaterEconomyService {

    private final FieldZoneRepository fieldZoneRepository;
    private final IrrigationScheduleRepository scheduleRepository;
    private final WaterConsumptionRepository consumptionRepository;
    private final WaterSourceRepository sourceRepository;
    private final RainwaterHarvestRepository harvestRepository;
    private final WaterNeedService waterNeedService;
    private final AgroWeatherService agroWeatherService;

    public WaterEconomyService(
            FieldZoneRepository fieldZoneRepository,
            IrrigationScheduleRepository scheduleRepository,
            WaterConsumptionRepository consumptionRepository,
            WaterSourceRepository sourceRepository,
            RainwaterHarvestRepository harvestRepository,
            WaterNeedService waterNeedService,
            AgroWeatherService agroWeatherService) {
        this.fieldZoneRepository = fieldZoneRepository;
        this.scheduleRepository = scheduleRepository;
        this.consumptionRepository = consumptionRepository;
        this.sourceRepository = sourceRepository;
        this.harvestRepository = harvestRepository;
        this.waterNeedService = waterNeedService;
        this.agroWeatherService = agroWeatherService;
    }

    /** Start of the requested period (day, week, month). */
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
     * Average ET0 of the period, or a fallback value when the weather service is temporarily
     * unavailable: a dashboard must never fail because of an external service.
     */
    public double averageEt0(LocalDate from, LocalDate to) {
        try {
            return agroWeatherService.et0Between(from, to);
        } catch (RuntimeException ex) {
            return AgroWeatherService.FALLBACK_ET0_MM;
        }
    }

    /** Daily water need of all zones for a given ET0. */
    public double dailyNeedLiters(double et0Mm) {
        return fieldZoneRepository.findAll().stream()
                .mapToDouble(zone -> waterNeedService.needLiters(zone, et0Mm))
                .sum();
    }

    /** Per-zone detail of the theoretical need: area, Kc, efficiency and irrigation method. */
    public List<Map<String, Object>> needBreakdown(double et0Mm) {
        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (Zone zone : fieldZoneRepository.findAll()) {
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
     * Water savings balance of the period: the crops' theoretical need set against the
     * consumption actually measured by the flow sensors.
     *
     * <p>This is the module's value measure. A positive balance means consumption stayed
     * under the crops' need (water saved); a negative balance flags over-irrigation
     * (water lost, to be fixed first).</p>
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
     * Cumulative water savings series, day by day: this is the curve showing the
     * progress over time (liters saved since the start of the window).
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
     * Water balance: inputs (recovered rain) and outputs (consumed water), compared with the
     * reservoir levels. It makes visible the water that reaches neither the crop nor the stock,
     * hence the losses.
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
        response.put("balance_status", capacity <= 0 ? "unknown"
                : (currentLevel / capacity) >= 0.30 ? "equilibre" : "deficit");
        return response;
    }

    /** Volume of rainwater recovered over a date interval (balance input), computed in the database. */
    private double harvestedBetween(LocalDate from, LocalDate to) {
        Instant startInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return harvestRepository.sumHarvestedLitersBetween(startInstant, endInstant);
    }

    /**
     * Volume avoided thanks to postponed irrigations: this is the saving made by not watering
     * when the announced rain covered the crop's need.
     */
    public double postponedLitersBetween(LocalDate from, LocalDate to) {
        Instant startInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return scheduleRepository.sumPostponedLitersBetween(startInstant, endInstant);
    }

    private double round(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private double sum(List<Double> values) {
        return values.stream().filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
    }
}