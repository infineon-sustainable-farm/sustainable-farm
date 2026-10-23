package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.SoilMoistureReadingRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatic irrigation control driven by soil moisture (specification module 1.4:
 * the "water saving planning").
 *
 * <p>Rule applied to each zone: if the last soil moisture measurement drops below the
 * threshold of the current season AND no irrigation is running or planned AND the last
 * irrigation was more than {@value #MIN_HOURS_BETWEEN_IRRIGATIONS} hours ago, then a schedule
 * is created automatically ({@code trigger_source = auto}) with the volume derived from the zone's
 * agronomic need (ET0 x Kc / efficiency, see {@link WaterNeedService}).</p>
 *
 * <p>The same method serves the manual endpoint {@code POST /api/irrigations/auto-trigger} and the
 * scheduled job ({@link IrrigationAutoTriggerJob}): behavior is therefore identical regardless of
 * the origin, and each excluded zone is justified in the response.</p>
 */
@Service
@Transactional(readOnly = true)
public class IrrigationAutomationService {

    /** Minimum delay between two irrigations of the same zone (hours). */
    public static final int MIN_HOURS_BETWEEN_IRRIGATIONS = 24;

    /** Default duration of an automatic irrigation (minutes), the volume being computed separately. */
    public static final int DEFAULT_DURATION_MINUTES = 30;

    /** Origin recorded on the schedules created by this rule. */
    public static final String TRIGGER_SOURCE_AUTO = "auto";

    private final FieldZoneRepository fieldZoneRepository;
    private final SoilMoistureReadingRepository soilMoistureRepository;
    private final IrrigationScheduleRepository scheduleRepository;
    private final IrrigationLogRepository logRepository;
    private final WaterNeedService waterNeedService;
    private final AgroWeatherService agroWeatherService;
    private final AlertService alertService;

    public IrrigationAutomationService(
            FieldZoneRepository fieldZoneRepository,
            SoilMoistureReadingRepository soilMoistureRepository,
            IrrigationScheduleRepository scheduleRepository,
            IrrigationLogRepository logRepository,
            WaterNeedService waterNeedService,
            AgroWeatherService agroWeatherService,
            AlertService alertService) {
        this.fieldZoneRepository = fieldZoneRepository;
        this.soilMoistureRepository = soilMoistureRepository;
        this.scheduleRepository = scheduleRepository;
        this.logRepository = logRepository;
        this.waterNeedService = waterNeedService;
        this.agroWeatherService = agroWeatherService;
        this.alertService = alertService;
    }

    /**
     * Soil moisture threshold (%) below which an irrigation becomes necessary, per season.
     * Values taken from the 2026 specification work: the drier and hotter the season,
     * the more the soil must be kept moist.
     */
    public static double seasonalThresholdPercent(Month month) {
        return switch (month) {
            case JUNE, JULY, AUGUST, SEPTEMBER -> 40d;      // rainy season
            case APRIL, MAY, OCTOBER -> 45d;                // transition
            case NOVEMBER, DECEMBER, JANUARY -> 50d;        // cool dry season
            case FEBRUARY, MARCH -> 55d;                    // hot dry season
        };
    }

    /** Readable name of the season, reused in the API response. */
    public static String seasonLabel(Month month) {
        return switch (month) {
            case JUNE, JULY, AUGUST, SEPTEMBER -> "rainy";
            case APRIL, MAY, OCTOBER -> "transition";
            case NOVEMBER, DECEMBER, JANUARY -> "cool dry";
            case FEBRUARY, MARCH -> "hot dry";
        };
    }


    /**
     * Applies the moisture rule to all zones and creates the necessary schedules.
     *
     * @return a readable report: seasonal threshold applied, day's ET0, schedules created and
     *         excluded zones with their reason (a zone without a soil sensor cannot be driven)
     */
    @Transactional
    public Map<String, Object> trigger() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Month month = today.getMonth();
        double threshold = seasonalThresholdPercent(month);
        double et0 = agroWeatherService.et0Between(today, today);

        List<Map<String, Object>> created = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        for (Zone zone : fieldZoneRepository.findAll()) {
            Double moisture = soilMoistureRepository.findLatestMoisturePercentBefore(zone.getId(), now);
            if (moisture == null) {
                skipped.add(skipped(zone, "no soil moisture reading"));
                continue;
            }
            if (moisture >= threshold) {
                skipped.add(skipped(zone, "soil moisture above the seasonal threshold"));
                continue;
            }
            if (hasOpenIrrigation(zone.getId())) {
                skipped.add(skipped(zone, "an irrigation is already planned or running"));
                continue;
            }
            if (irrigatedRecently(zone.getId(), now)) {
                skipped.add(skipped(zone, "irrigated less than " + MIN_HOURS_BETWEEN_IRRIGATIONS + " hours ago"));
                continue;
            }
            created.add(create(zone, et0, moisture, threshold, month, now));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("check_time", now.toString());
        response.put("season", seasonLabel(month));
        response.put("season_threshold_percent", threshold);
        response.put("et0_mm", Math.round(et0 * 100d) / 100d);
        response.put("created_count", created.size());
        response.put("created", created);
        response.put("skipped_count", skipped.size());
        response.put("skipped", skipped);
        return response;
    }

    private Map<String, Object> create(Zone zone, double et0, double moisture, double threshold,
            Month month, Instant now) {
        double liters = Math.round(waterNeedService.needLiters(zone, et0));
        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setZoneId(zone.getId());
        schedule.setStartTime(now);
        schedule.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        schedule.setWaterQuantityLiters(liters);
        schedule.setStatus("scheduled");
        schedule.setTriggerSource(TRIGGER_SOURCE_AUTO);
        schedule.setCreatedBy(SystemUsers.IOT_SYSTEM_USER_ID);
        IrrigationSchedule saved = scheduleRepository.save(schedule);

        alertService.raiseOnce("info", "Automatic irrigation planned - " + zone.getName(),
                String.format(
                        "Soil moisture %.0f%% is below the %s season threshold (%.0f%%): %.0f L planned "
                                + "for zone \"%s\" (the crop needs it, the calendar alone does not).",
                        moisture, seasonLabel(month), threshold, liters, zone.getName()),
                "/watersupply/irrigation");

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("schedule_id", saved.getId());
        item.put("zone_id", zone.getId());
        item.put("zone_name", zone.getName());
        item.put("soil_moisture_percent", moisture);
        item.put("season_threshold_percent", threshold);
        item.put("planned_liters", liters);
        item.put("duration_minutes", DEFAULT_DURATION_MINUTES);
        item.put("trigger_source", TRIGGER_SOURCE_AUTO);
        return item;
    }

    private Map<String, Object> skipped(Zone zone, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("zone_id", zone.getId());
        item.put("zone_name", zone.getName());
        item.put("reason", reason);
        return item;
    }

    private boolean hasOpenIrrigation(UUID zoneId) {
        return scheduleRepository.findByZoneId(zoneId).stream()
                .anyMatch(schedule -> "scheduled".equalsIgnoreCase(schedule.getStatus())
                        || "running".equalsIgnoreCase(schedule.getStatus()));
    }

    private boolean irrigatedRecently(UUID zoneId, Instant now) {
        Instant since = now.minus(MIN_HOURS_BETWEEN_IRRIGATIONS, ChronoUnit.HOURS);
        List<IrrigationLog> recent = logRepository.findForZoneBetween(zoneId, since, now);
        return !recent.isEmpty();
    }
}
