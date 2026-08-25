package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final WaterConsumptionRepository waterConsumptionRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final IrrigationScheduleRepository irrigationScheduleRepository;
    private final WaterQualityTestRepository waterQualityTestRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Placeholder en attendant l'integration des capteurs IoT.
     */
    @Value("${app.sensor.availability-percentage:100}")
    private int sensorAvailabilityPercentage;

    public DashboardController(
            WaterConsumptionRepository waterConsumptionRepository,
            WaterSourceRepository waterSourceRepository,
            IrrigationScheduleRepository irrigationScheduleRepository,
            WaterQualityTestRepository waterQualityTestRepository,
            NotificationRepository notificationRepository) {
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.waterSourceRepository = waterSourceRepository;
        this.irrigationScheduleRepository = irrigationScheduleRepository;
        this.waterQualityTestRepository = waterQualityTestRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * KPI "eau economisee" - metrique de valeur ("water saving").
     * eau_economisee = consommation_reference - consommation_reelle
     * taux_economie (%) = eau_economisee / consommation_reference * 100
     */
    @GetMapping("/water-savings")
    public Map<String, Object> waterSavings(@RequestParam(defaultValue = "month") String period) {
        Instant start = periodStart(period);
        Instant now = Instant.now();

        double referenceLiters = irrigationScheduleRepository.findAll().stream()
                .filter(schedule -> schedule.getStartTime() != null
                        && !schedule.getStartTime().isBefore(start)
                        && schedule.getStartTime().isBefore(now))
                .mapToDouble(schedule -> schedule.getWaterQuantityLiters() == null ? 0 : schedule.getWaterQuantityLiters())
                .sum();

        double actualLiters = waterConsumptionRepository.sumConsumptionSince(start);

        double savedLiters = Math.max(0, referenceLiters - actualLiters);
        double savingsPercent = referenceLiters <= 0 ? 0
                : Math.max(0, Math.round((savedLiters / referenceLiters) * 100));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("period", period);
        response.put("reference_liters", referenceLiters);
        response.put("actual_liters", actualLiters);
        response.put("saved_liters", savedLiters);
        response.put("savings_percentage", savingsPercent);
        return response;
    }

    @GetMapping("/kpis")
    public Map<String, Object> kpis() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = startOfDay.plusSeconds(86_400);

        double dailyConsumption = waterConsumptionRepository.sumConsumptionSince(startOfDay);
        double monthlyConsumption = waterConsumptionRepository.sumConsumptionSince(startOfMonth);
        double plannedMonthly = irrigationScheduleRepository.findAll().stream()
                .filter(schedule -> schedule.getStartTime() != null
                        && !schedule.getStartTime().isBefore(startOfMonth))
                .mapToDouble(schedule -> schedule.getWaterQuantityLiters() == null ? 0 : schedule.getWaterQuantityLiters())
                .sum();

        double waterSavings = plannedMonthly <= 0 ? 0
                : Math.max(0, Math.round(((plannedMonthly - monthlyConsumption) / plannedMonthly) * 100));

        double capacity = waterSourceRepository.findAll().stream()
                .mapToDouble(source -> source.getCapacityLiters() == null ? 0 : source.getCapacityLiters())
                .sum();
        double level = waterSourceRepository.findAll().stream()
                .mapToDouble(source -> source.getCurrentLevelLiters() == null ? 0 : source.getCurrentLevelLiters())
                .sum();
        double tankLevel = capacity == 0 ? 0 : Math.round((level / capacity) * 100);

        long anomalyCount = notificationRepository.findAll().stream()
                .filter(notification -> "critical".equalsIgnoreCase(notification.getType()))
                .count();

        return Map.of(
                "daily_consumption_liters", dailyConsumption,
                "monthly_consumption_liters", monthlyConsumption,
                "tank_level_percentage", tankLevel,
                "irrigation_count_today", irrigationScheduleRepository.countByStartTimeBetween(startOfDay, endOfDay),
                "water_savings_percentage", waterSavings,
                "anomaly_count", anomalyCount,
                "water_quality_status", waterQualityStatus(),
                "sensor_availability_percentage", sensorAvailabilityPercentage);
    }

    private Instant periodStart(String period) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        switch (period == null ? "month" : period.toLowerCase()) {
            case "day":
                return today.atStartOfDay().toInstant(ZoneOffset.UTC);
            case "week":
                return today.minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC);
            case "month":
            default:
                return today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }
    }

    private String waterQualityStatus() {
        return waterQualityTestRepository.findAll().stream()
                .max(Comparator.comparing(WaterQualityTest::getTestDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(test -> {
                    boolean phOutOfRange = test.getPh() != null && (test.getPh() < 6.0 || test.getPh() > 7.5);
                    boolean turbidityHigh = test.getTurbidityNtu() != null && test.getTurbidityNtu() > 5.0;
                    return phOutOfRange || turbidityHigh ? "warning" : "good";
                })
                .orElse("good");
    }

    @GetMapping("/activities")
    public List<Map<String, Object>> activities() {
        return waterConsumptionRepository.findAll().stream()
                .sorted(Comparator.comparing(WaterConsumption::getConsumptionDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .map(consumption -> Map.<String, Object>of(
                        "type", "consumption",
                        "message", consumption.getConsumptionLiters() + "L consommés",
                        "timestamp", consumption.getConsumptionDate() == null
                                ? consumption.getCreatedAt().toString() : consumption.getConsumptionDate().toString()))
                .toList();
    }

    @GetMapping("/alerts")
    public List<Map<String, Object>> alerts() {
        return notificationRepository.findAll().stream()
                .filter(notification -> Boolean.FALSE.equals(notification.getRead()))
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .limit(10)
                .map(notification -> Map.<String, Object>of(
                        "type", notification.getType(),
                        "title", notification.getTitle(),
                        "message", notification.getMessage(),
                        "timestamp", notification.getCreatedAt().toString()))
                .toList();
    }
}
