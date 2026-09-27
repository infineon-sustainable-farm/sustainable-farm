package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.SoilMoistureReadingRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
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
 * Pilotage automatique de l'irrigation par l'humidite du sol (module 1.4 de la specification :
 * le « water saving planning »).
 *
 * <p>Regle appliquee a chaque zone : si la derniere mesure d'humidite du sol descend sous le
 * seuil de la saison en cours ET qu'aucun arrosage n'est en cours ou prevu ET que le dernier
 * arrosage remonte a plus de {@value #MIN_HOURS_BETWEEN_IRRIGATIONS} heures, alors un planning
 * est cree automatiquement ({@code trigger_source = auto}) avec le volume issu du besoin
 * agronomique de la zone (ET0 x Kc / efficacite, voir {@link WaterNeedService}).</p>
 *
 * <p>La meme methode sert a l'endpoint manuel {@code POST /api/irrigations/auto-trigger} et a la
 * tache planifiee ({@link IrrigationAutoTriggerJob}) : le comportement est donc identique quelle
 * que soit l'origine, et chaque zone ecartee est justifiee dans la reponse.</p>
 */
@Service
@Transactional(readOnly = true)
public class IrrigationAutomationService {

    /** Delai minimal entre deux arrosages d'une meme zone (heures). */
    public static final int MIN_HOURS_BETWEEN_IRRIGATIONS = 24;

    /** Duree par defaut d'un arrosage automatique (minutes), le volume etant calcule a part. */
    public static final int DEFAULT_DURATION_MINUTES = 30;

    /** Origine enregistree sur les plannings crees par cette regle. */
    public static final String TRIGGER_SOURCE_AUTO = "auto";

    private final ZoneRepository zoneRepository;
    private final SoilMoistureReadingRepository soilMoistureRepository;
    private final IrrigationScheduleRepository scheduleRepository;
    private final IrrigationLogRepository logRepository;
    private final WaterNeedService waterNeedService;
    private final AgroWeatherService agroWeatherService;
    private final AlertService alertService;

    public IrrigationAutomationService(
            ZoneRepository zoneRepository,
            SoilMoistureReadingRepository soilMoistureRepository,
            IrrigationScheduleRepository scheduleRepository,
            IrrigationLogRepository logRepository,
            WaterNeedService waterNeedService,
            AgroWeatherService agroWeatherService,
            AlertService alertService) {
        this.zoneRepository = zoneRepository;
        this.soilMoistureRepository = soilMoistureRepository;
        this.scheduleRepository = scheduleRepository;
        this.logRepository = logRepository;
        this.waterNeedService = waterNeedService;
        this.agroWeatherService = agroWeatherService;
        this.alertService = alertService;
    }

    /**
     * Seuil d'humidite du sol (%) sous lequel un arrosage devient necessaire, par saison.
     * Valeurs reprises du travail de specification 2026 : plus la saison est seche et chaude,
     * plus le sol doit etre maintenu humide.
     */
    public static double seasonalThresholdPercent(Month month) {
        return switch (month) {
            case JUNE, JULY, AUGUST, SEPTEMBER -> 40d;      // saison des pluies
            case APRIL, MAY, OCTOBER -> 45d;                // transition
            case NOVEMBER, DECEMBER, JANUARY -> 50d;        // saison seche fraiche
            case FEBRUARY, MARCH -> 55d;                    // saison seche chaude
        };
    }

    /** Nom lisible de la saison, repris dans la reponse de l'API. */
    public static String seasonLabel(Month month) {
        return switch (month) {
            case JUNE, JULY, AUGUST, SEPTEMBER -> "rainy";
            case APRIL, MAY, OCTOBER -> "transition";
            case NOVEMBER, DECEMBER, JANUARY -> "cool dry";
            case FEBRUARY, MARCH -> "hot dry";
        };
    }


    /**
     * Applique la regle d'humidite a toutes les zones et cree les plannings necessaires.
     *
     * @return un rapport lisible : seuil saisonnier applique, ET0 du jour, plannings crees et
     *         zones ecartees avec leur motif (une zone sans capteur de sol ne peut pas etre pilotee)
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
        for (Zone zone : zoneRepository.findAll()) {
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
