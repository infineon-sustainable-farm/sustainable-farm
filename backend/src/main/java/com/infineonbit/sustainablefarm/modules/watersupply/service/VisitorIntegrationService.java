package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.SafetyBriefingRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorEventRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSafetyBriefing;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorEvent;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ConsumptionCriteria;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSafetyBriefingRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterVisitorEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read models and events for Visitor Management (V1-V8 of the integration audit).
 *
 * <p>Visitor Management reads (level, savings, irrigation status, live consumption, planned
 * unavailability, safety briefing) and pushes visit announcements. Nothing here commands equipment:
 * the demo valve goes through {@link IotCommandService}.</p>
 */
@Service
@Transactional(readOnly = true)
public class VisitorIntegrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorIntegrationService.class);
    /** Upper bound of the live window: beyond one day it is no longer "live". */
    private static final int MAX_LIVE_WINDOW_MINUTES = 24 * 60;
    private static final String LIST_SEPARATOR = "|";

    private final WaterVisitorEventRepository visitorEventRepository;
    private final WaterSafetyBriefingRepository safetyRepository;
    private final WaterConsumptionRepository consumptionRepository;
    private final WaterSourceRepository sourceRepository;
    private final FieldZoneRepository zoneRepository;
    private final WaterService waterService;
    private final IrrigationService irrigationService;
    private final WaterEconomyService economyService;
    private final WaterUnavailabilityService unavailabilityService;
    private final AlertService alertService;
    private final WaterIntegrationProperties properties;

    public VisitorIntegrationService(WaterVisitorEventRepository visitorEventRepository,
                                     WaterSafetyBriefingRepository safetyRepository,
                                     WaterConsumptionRepository consumptionRepository,
                                     WaterSourceRepository sourceRepository,
                                     FieldZoneRepository zoneRepository,
                                     WaterService waterService,
                                     IrrigationService irrigationService,
                                     WaterEconomyService economyService,
                                     WaterUnavailabilityService unavailabilityService,
                                     AlertService alertService,
                                     WaterIntegrationProperties properties) {
        this.visitorEventRepository = visitorEventRepository;
        this.safetyRepository = safetyRepository;
        this.consumptionRepository = consumptionRepository;
        this.sourceRepository = sourceRepository;
        this.zoneRepository = zoneRepository;
        this.waterService = waterService;
        this.irrigationService = irrigationService;
        this.economyService = economyService;
        this.unavailabilityService = unavailabilityService;
        this.alertService = alertService;
        this.properties = properties;
    }

    /**
     * Consumption counter (V4): volume measured over the last {@code windowMinutes} (default
     * {@code app.water.visitor.live-window-minutes}), for a source and/or a zone or the whole site.
     * "Live" is bounded by the sensors' sending period: no streaming, the client polls.
     */
    public Map<String, Object> liveConsumption(UUID sourceId, UUID zoneId, Integer windowMinutes) {
        if (sourceId != null && !sourceRepository.existsById(sourceId)) {
            throw new NotFoundException("WaterSource");
        }
        if (zoneId != null && !zoneRepository.existsById(zoneId)) {
            throw new NotFoundException("Zone");
        }
        int window = windowMinutes == null ? properties.visitorLiveWindowMinutes
                : Math.min(MAX_LIVE_WINDOW_MINUTES, Math.max(1, windowMinutes));
        Instant end = Instant.now();
        Instant start = end.minusSeconds(window * 60L);
        List<WaterConsumption> items = consumptionRepository.findAll(
                ConsumptionCriteria.window(sourceId, zoneId, start, end).toSpecification());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sourceId", sourceId);
        response.put("zoneId", zoneId);
        response.put("windowMinutes", window);
        response.put("liters", items.stream().map(WaterConsumption::getConsumptionLiters)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum());
        response.put("measurementCount", items.size());
        response.put("lastMeasurementAt", items.stream().map(WaterConsumption::getConsumptionDate)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
        return response;
    }

    /** Everything a visitor screen shows, in one call (V1 + V2 + V3 + V4 + V6). */
    public Map<String, Object> summary(UUID sourceId, UUID zoneId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sourceLevel", sourceId == null ? null : waterService.sourceLevel(sourceId));
        response.put("waterSavingsPercentage", savingsPercentage());
        response.put("irrigationStatus", zoneId == null ? null : irrigationService.irrigationStatus(zoneId));
        response.put("liveConsumption", liveConsumption(sourceId, zoneId, null));
        response.put("activeUnavailability", unavailabilityService.search(null, null, zoneId, sourceId, Instant.now()));
        return response;
    }

    /**
     * Visit announcement pushed by Visitor Management (V7). Idempotent on {@code visitId}: sending the
     * same visit again updates it instead of creating a duplicate. A notification is raised for the
     * water team, as a warning when a visited zone is under planned unavailability at the visit time.
     */
    @Transactional
    public Map<String, Object> visitorEvent(VisitorEventRequest request) {
        String visitId = request.visitId().trim();
        List<UUID> zones = request.zones() == null ? List.of() : request.zones().stream().distinct().toList();
        List<UUID> conflictingZones = zones.stream()
                .filter(zone -> unavailabilityService.isZoneUnavailable(zone, request.expectedAt()))
                .toList();

        WaterVisitorEvent event = visitorEventRepository.findByVisitId(visitId).orElseGet(WaterVisitorEvent::new);
        boolean created = event.getId() == null;
        event.setVisitId(visitId);
        event.setGroupName(request.groupName());
        event.setExpectedAt(request.expectedAt());
        event.setZones(zones.stream().map(UUID::toString).collect(Collectors.joining(LIST_SEPARATOR)));
        event.setConflict(!conflictingZones.isEmpty());
        WaterVisitorEvent saved = visitorEventRepository.save(event);

        // The title carries the group: AlertService de-duplicates on the title, so a re-sent visit
        // does not notify twice while two different groups each get their notification.
        String group = request.groupName() == null || request.groupName().isBlank() ? visitId : request.groupName().trim();
        group = group.length() > 80 ? group.substring(0, 80) : group;
        String title = (conflictingZones.isEmpty()
                ? "Visitor group near water facilities - "
                : "Visitor group expected during water unavailability - ") + group;
        alertService.raiseOnce(conflictingZones.isEmpty() ? "info" : "warning", title,
                group + " expected at " + request.expectedAt()
                        + (conflictingZones.isEmpty() ? "" : " on unavailable zone(s) " + conflictingZones),
                "/watersupply/visitor-events");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("eventId", saved.getId());
        response.put("visitId", saved.getVisitId());
        response.put("created", created);
        response.put("expectedAt", saved.getExpectedAt());
        response.put("zones", zones);
        response.put("conflict", saved.getConflict());
        response.put("conflictingZones", conflictingZones);
        response.put("notificationTitle", title);
        return response;
    }

    /** Current safety briefing (V8); a default text is returned (not stored) until one is published. */
    public Map<String, Object> safetyBriefing() {
        return briefingResponse(safetyRepository.findFirstByOrderByCreatedAtDesc().orElseGet(this::defaultBriefing));
    }

    /**
     * Publishes a new version of the briefing: the previous versions are kept (who said what to the
     * visitors, and when). Omitted fields are carried over from the current version.
     */
    @Transactional
    public Map<String, Object> updateSafetyBriefing(SafetyBriefingRequest request) {
        if (request.maxCapacity() != null && request.maxCapacity() < 1) {
            throw new IllegalArgumentException("maxCapacity must be at least 1");
        }
        WaterSafetyBriefing current = safetyRepository.findFirstByOrderByCreatedAtDesc().orElseGet(this::defaultBriefing);
        WaterSafetyBriefing next = new WaterSafetyBriefing();
        next.setVersion(request.version() == null || request.version().isBlank()
                ? LocalDate.now(ZoneOffset.UTC).toString() : request.version().trim());
        next.setAllowedZones(request.allowedZones() == null ? current.getAllowedZones() : join(request.allowedZones()));
        next.setForbiddenZones(request.forbiddenZones() == null ? current.getForbiddenZones() : join(request.forbiddenZones()));
        next.setRisks(request.risks() == null ? current.getRisks() : join(request.risks()));
        next.setText(request.text() == null ? current.getText() : request.text());
        next.setMaxCapacity(request.maxCapacity() == null ? current.getMaxCapacity() : request.maxCapacity());
        next.setGuideRequired(request.guideRequired() == null ? current.getGuideRequired() : request.guideRequired());
        return briefingResponse(safetyRepository.save(next));
    }

    private Object savingsPercentage() {
        try {
            return economyService.savings("month").get("savings_percentage");
        } catch (RuntimeException ex) {
            // Weather (ET0) unavailable: the rest of the visitor screen must stay readable.
            LOGGER.warn("Water savings unavailable for the visitor summary", ex);
            return null;
        }
    }

    /** Working default, flagged as such: the owner of this text (Water Supply or Site Security) is to be confirmed. */
    private WaterSafetyBriefing defaultBriefing() {
        WaterSafetyBriefing briefing = new WaterSafetyBriefing();
        briefing.setVersion("default");
        briefing.setAllowedZones(join(List.of("reservoir surroundings", "demonstration valve")));
        briefing.setForbiddenZones(join(List.of("electric pump room")));
        briefing.setRisks(join(List.of("slippery ground near the basin", "electrical hazard at the pump")));
        briefing.setText("Default text, not validated: to be confirmed by the safety owner (Site Security or Water Supply).");
        briefing.setGuideRequired(true);
        briefing.setMaxCapacity(properties.safetyDefaultMaxCapacity);
        return briefing;
    }

    private Map<String, Object> briefingResponse(WaterSafetyBriefing briefing) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("version", briefing.getVersion());
        response.put("allowedZones", split(briefing.getAllowedZones()));
        response.put("forbiddenZones", split(briefing.getForbiddenZones()));
        response.put("risks", split(briefing.getRisks()));
        response.put("text", briefing.getText());
        response.put("maxCapacity", briefing.getMaxCapacity());
        response.put("guideRequired", briefing.getGuideRequired());
        response.put("validated", briefing.getId() != null);
        response.put("publishedAt", briefing.getCreatedAt());
        return response;
    }

    private static String join(List<String> values) {
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty())
                .collect(Collectors.joining(LIST_SEPARATOR));
    }

    private static List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split("\\|"));
    }
}
