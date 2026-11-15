package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQualityTestRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.service.AIService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.IotDeviceService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.WaterEconomyService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class WaterDashboardController {
    private final WaterConsumptionRepository waterConsumptionRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final IrrigationScheduleRepository irrigationScheduleRepository;
    private final WaterQualityTestRepository waterQualityTestRepository;
    private final NotificationRepository notificationRepository;
    private final WaterEconomyService waterEconomyService;
    private final AIService aiService;
    private final IotDeviceService iotDeviceService;

    public WaterDashboardController(
            WaterConsumptionRepository waterConsumptionRepository,
            WaterSourceRepository waterSourceRepository,
            IrrigationScheduleRepository irrigationScheduleRepository,
            WaterQualityTestRepository waterQualityTestRepository,
            NotificationRepository notificationRepository,
            WaterEconomyService waterEconomyService,
            AIService aiService,
            IotDeviceService iotDeviceService) {
        this.waterConsumptionRepository = waterConsumptionRepository;
        this.waterSourceRepository = waterSourceRepository;
        this.irrigationScheduleRepository = irrigationScheduleRepository;
        this.waterQualityTestRepository = waterQualityTestRepository;
        this.notificationRepository = notificationRepository;
        this.waterEconomyService = waterEconomyService;
        this.aiService = aiService;
        this.iotDeviceService = iotDeviceService;
    }

    /**
     * KPI "water saved" - the module's value metric.
     *
     * <p>The reference is no longer the sum of entered schedules (unreliable) but the
     * <strong>crop need</strong> estimated from the location's evapotranspiration
     * (FAO-56): see {@link WaterEconomyService}. The response therefore contains the need, the
     * actual consumption, the water saved, the over-irrigation losses, the reused rain
     * and the volume avoided by weather postponements.</p>
     */
    @GetMapping("/water-savings")
    public Map<String, Object> waterSavings(@RequestParam(defaultValue = "month") String period) {
        return waterEconomyService.savings(period);
    }

    /**
     * Cumulative series of water savings (progress curve): each point compares the crops'
     * cumulative need to the cumulative consumption measured by the flow sensors.
     */
    @GetMapping("/savings-series")
    public Map<String, Object> savingsSeries(@RequestParam(defaultValue = "30") int days) {
        return waterEconomyService.savingsSeries(days);
    }

    /**
     * Water balance of the period: inputs (harvested rain) and outputs (water consumed)
     * confronted with the reservoir levels, to make losses visible.
     */
    @GetMapping("/water-balance")
    public Map<String, Object> waterBalance(@RequestParam(defaultValue = "month") String period) {
        return waterEconomyService.waterBalance(period);
    }

    /**
     * Anomalies detected on flow measurements (probable leaks). This diagnosis already existed
     * service-side but was not exposed anywhere: it is now displayed in the UI.
     */
    @GetMapping("/leaks")
    public Map<String, Object> leaks() {
        return aiService.analyze();
    }

    @GetMapping("/kpis")
    public Map<String, Object> kpis() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = startOfDay.plusSeconds(86_400);

        double dailyConsumption = waterConsumptionRepository.sumConsumptionSince(startOfDay);
        double monthlyConsumption = waterConsumptionRepository.sumConsumptionSince(startOfMonth);
        // Water savings are measured against the crops' NEED (ET0 x Kc / system
        // efficiency), not against the entered schedules: see WaterEconomyService.
        Map<String, Object> monthEconomy = waterEconomyService.savings("month");
        double waterSavings = number(monthEconomy.get("savings_percentage"));
        Map<String, Object> sensorAvailability = iotDeviceService.availability();

        double capacity = waterSourceRepository.sumCapacityLiters();
        double level = waterSourceRepository.sumCurrentLevelLiters();
        double tankLevel = capacity == 0 ? 0 : Math.round((level / capacity) * 100);

        // Anomalies = critical alerts in the database (computed by the database, no full scan).
        long anomalyCount = notificationRepository.countByTypeIgnoreCase("critical");

        return Map.of(
                "daily_consumption_liters", dailyConsumption,
                "monthly_consumption_liters", monthlyConsumption,
                "tank_level_percentage", tankLevel,
                "irrigation_count_today", irrigationScheduleRepository.countByStartTimeBetween(startOfDay, endOfDay),
                "water_savings_percentage", waterSavings,
                "anomaly_count", anomalyCount,
                "water_quality_status", waterQualityStatus(),
                "sensor_availability_percentage", sensorAvailability.get("availabilityPercentage"));
    }

    /** Tolerant read of a numeric value: the savings percentage may be an integer or a decimal. */
    private static double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0d;
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
                        "message", consumption.getConsumptionLiters() + "L consumed",
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
