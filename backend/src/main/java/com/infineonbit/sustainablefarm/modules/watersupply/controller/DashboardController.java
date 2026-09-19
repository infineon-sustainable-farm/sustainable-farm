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
public class DashboardController {
    private final WaterConsumptionRepository waterConsumptionRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final IrrigationScheduleRepository irrigationScheduleRepository;
    private final WaterQualityTestRepository waterQualityTestRepository;
    private final NotificationRepository notificationRepository;
    private final WaterEconomyService waterEconomyService;
    private final AIService aiService;
    private final IotDeviceService iotDeviceService;

    public DashboardController(
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
     * KPI "eau economisee" - metrique de valeur du module.
     *
     * <p>La reference n'est plus la somme des plannings saisis (peu fiable) mais le
     * <strong>besoin des cultures</strong> estime a partir de l'evapotranspiration du lieu
     * (FAO-56) : voir {@link WaterEconomyService}. La reponse contient donc le besoin, la
     * consommation reelle, l'eau economisee, les pertes par sur-irrigation, la pluie reutilisee
     * et le volume evite par les reports meteo.</p>
     */
    @GetMapping("/water-savings")
    public Map<String, Object> waterSavings(@RequestParam(defaultValue = "month") String period) {
        return waterEconomyService.savings(period);
    }

    /**
     * Serie cumulee de l'economie d'eau (courbe de progression) : chaque point compare le besoin
     * cumule des cultures a la consommation cumulee mesuree par les capteurs de debit.
     */
    @GetMapping("/savings-series")
    public Map<String, Object> savingsSeries(@RequestParam(defaultValue = "30") int days) {
        return waterEconomyService.savingsSeries(days);
    }

    /**
     * Bilan hydrique de la periode : entrees (pluie recuperee) et sorties (eau consommee)
     * confrontes au niveau des reservoirs, afin de rendre visibles les pertes.
     */
    @GetMapping("/water-balance")
    public Map<String, Object> waterBalance(@RequestParam(defaultValue = "month") String period) {
        return waterEconomyService.waterBalance(period);
    }

    /**
     * Anomalies detectees sur les mesures de debit (fuites probables). Ce diagnostic existait deja
     * cote service mais n'etait expose nulle part : il est desormais affiche dans l'interface.
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
        // L'economie d'eau se mesure par rapport au BESOIN des cultures (ET0 x Kc / efficacite du
        // systeme), et non par rapport aux plannings saisis : voir WaterEconomyService.
        Map<String, Object> monthEconomy = waterEconomyService.savings("month");
        double waterSavings = number(monthEconomy.get("savings_percentage"));
        Map<String, Object> sensorAvailability = iotDeviceService.availability();

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
                "sensor_availability_percentage", sensorAvailability.get("availabilityPercentage"));
    }

    /** Lecture tolerante d'une valeur numerique : le pourcentage d'economie peut etre un entier ou un decimal. */
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
