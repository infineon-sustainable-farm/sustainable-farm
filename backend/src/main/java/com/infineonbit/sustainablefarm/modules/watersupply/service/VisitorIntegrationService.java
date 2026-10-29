package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.SafetyBriefingRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorEventRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSafetyBriefing;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorEvent;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorStop;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ConsumptionCriteria;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSafetyBriefingRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterVisitorEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 *
 * <p>As agreed with Visitor Management: a visit is identified by its registration id, its stops by
 * their name (mapped to zones and sources by {@link WaterVisitorStopService}), the consumption total
 * covers the visit's time slot, and the short window only gives the instantaneous flow rate.</p>
 */
@Service
@Transactional(readOnly = true)
public class VisitorIntegrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorIntegrationService.class);
    /** Upper bound of the live window: beyond one day it is no longer "live". */
    private static final int MAX_LIVE_WINDOW_MINUTES = 24 * 60;
    /** A visit session is a time slot: longer than a day it is not a visit. */
    private static final Duration MAX_SESSION = Duration.ofDays(1);
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
    private final WaterVisitorStopService stopService;
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
                                     WaterVisitorStopService stopService,
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
        this.stopService = stopService;
        this.alertService = alertService;
        this.properties = properties;
    }

    /** What a visitor screen looks at: explicit ids win over the zone/source mapped to the stop. */
    private record Target(UUID sourceId, UUID zoneId, String stop) {
    }

    public Map<String, Object> liveConsumption(UUID sourceId, UUID zoneId, Integer windowMinutes) {
        return liveConsumption(sourceId, zoneId, null, windowMinutes, null, null);
    }

    /**
     * Consumption counter (V4), for a source and/or a zone, a visit stop, or the whole site.
     *
     * <ul>
     *   <li>{@code session}: volume of the visit, from the start of its time slot ({@code sessionFrom})
     *       to its end ({@code sessionTo}), or to now while it is in progress. This is the total to show.</li>
     *   <li>{@code liters} and {@code flowRateLitersPerMinute}: the last {@code windowMinutes}
     *       (default {@code app.water.visitor.live-window-minutes}), for the instantaneous flow only.</li>
     * </ul>
     * "Live" is bounded by the sensors' sending period: no streaming, the client polls.
     */
    public Map<String, Object> liveConsumption(UUID sourceId, UUID zoneId, String stop, Integer windowMinutes,
                                               Instant sessionFrom, Instant sessionTo) {
        Target target = target(sourceId, zoneId, stop);
        if (target.sourceId() != null && !sourceRepository.existsById(target.sourceId())) {
            throw new NotFoundException("WaterSource");
        }
        if (target.zoneId() != null && !zoneRepository.existsById(target.zoneId())) {
            throw new NotFoundException("Zone");
        }
        int window = windowMinutes == null ? properties.visitorLiveWindowMinutes
                : Math.min(MAX_LIVE_WINDOW_MINUTES, Math.max(1, windowMinutes));
        Instant now = Instant.now();
        List<WaterConsumption> recent = measured(target, now.minusSeconds(window * 60L), now);
        double recentLiters = liters(recent);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sourceId", target.sourceId());
        response.put("zoneId", target.zoneId());
        response.put("stop", target.stop());
        response.put("windowMinutes", window);
        response.put("liters", recentLiters);
        response.put("measurementCount", recent.size());
        response.put("flowRateLitersPerMinute", Math.round(recentLiters / window * 100d) / 100d);
        response.put("lastMeasurementAt", recent.stream().map(WaterConsumption::getConsumptionDate)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
        response.put("session", sessionFrom == null ? null : session(target, sessionFrom, sessionTo, now));
        return response;
    }

    public Map<String, Object> summary(UUID sourceId, UUID zoneId) {
        return summary(sourceId, zoneId, null, null, null);
    }

    /** Everything a visitor screen shows, in one call (V1 + V2 + V3 + V4 + V6), for a source, a zone or a stop. */
    public Map<String, Object> summary(UUID sourceId, UUID zoneId, String stop, Instant sessionFrom, Instant sessionTo) {
        Target target = target(sourceId, zoneId, stop);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stop", target.stop());
        response.put("sourceLevel", target.sourceId() == null ? null : waterService.sourceLevel(target.sourceId()));
        response.put("waterSavingsPercentage", savingsPercentage());
        response.put("irrigationStatus", target.zoneId() == null ? null : irrigationService.irrigationStatus(target.zoneId()));
        response.put("liveConsumption", liveConsumption(target.sourceId(), target.zoneId(), null, null, sessionFrom, sessionTo));
        response.put("activeUnavailability",
                unavailabilityService.search(null, null, target.zoneId(), target.sourceId(), Instant.now()));
        return response;
    }

    private Target target(UUID sourceId, UUID zoneId, String stop) {
        if (stop == null || stop.isBlank()) {
            return new Target(sourceId, zoneId, null);
        }
        // An unmapped stop is not a water stop: 404 tells Visitor Management to show nothing for it.
        WaterVisitorStop mapped = stopService.resolve(stop).orElseThrow(() -> new NotFoundException("Visitor stop"));
        return new Target(sourceId != null ? sourceId : mapped.getSourceId(),
                zoneId != null ? zoneId : mapped.getZoneId(), mapped.getStopName());
    }

    private Map<String, Object> session(Target target, Instant from, Instant to, Instant now) {
        if (to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("sessionTo must be after sessionFrom");
        }
        if (Duration.between(from, to == null ? now : to).compareTo(MAX_SESSION) > 0) {
            throw new IllegalArgumentException("A visit session lasts at most one day");
        }
        // The total stops at "now" while the visit is in progress, and stays empty before it starts.
        Instant end = to == null || to.isAfter(now) ? now : to;
        List<WaterConsumption> items = end.isAfter(from) ? measured(target, from, end) : List.of();
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("from", from);
        session.put("to", to);
        session.put("inProgress", !from.isAfter(now) && (to == null || to.isAfter(now)));
        session.put("liters", liters(items));
        session.put("measurementCount", items.size());
        return session;
    }

    private List<WaterConsumption> measured(Target target, Instant from, Instant to) {
        return consumptionRepository.findAll(
                ConsumptionCriteria.window(target.sourceId(), target.zoneId(), from, to).toSpecification());
    }

    private static double liters(List<WaterConsumption> items) {
        return items.stream().map(WaterConsumption::getConsumptionLiters)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Visit announcement pushed by Visitor Management (V7). Idempotent on {@code visitId} (its
     * registration id): sending the same visit again updates it instead of creating a duplicate.
     * Stops are given by name; those the water team did not map are not water stops and come back in
     * {@code ignoredStops}. A notification is raised for the water team, as a warning when a visited
     * zone or tank is under planned unavailability at the visit time.
     */
    @Transactional
    public Map<String, Object> visitorEvent(VisitorEventRequest request) {
        String visitId = request.visitId().trim();
        List<String> stops = request.stops() == null ? List.of() : request.stops().stream()
                .filter(Objects::nonNull).map(String::trim).filter(stop -> !stop.isEmpty()).distinct().toList();
        List<WaterVisitorStop> mappedStops = new ArrayList<>();
        List<String> ignoredStops = new ArrayList<>();
        for (String stop : stops) {
            stopService.resolve(stop).ifPresentOrElse(mappedStops::add, () -> ignoredStops.add(stop));
        }
        List<UUID> zones = Stream.concat(
                        request.zones() == null ? Stream.<UUID>empty() : request.zones().stream(),
                        mappedStops.stream().map(WaterVisitorStop::getZoneId).filter(Objects::nonNull))
                .distinct()
                .toList();
        List<UUID> conflictingZones = zones.stream()
                .filter(zone -> unavailabilityService.isZoneUnavailable(zone, request.expectedAt()))
                .toList();
        List<String> conflictingStops = mappedStops.stream()
                .filter(stop -> (stop.getZoneId() != null && conflictingZones.contains(stop.getZoneId()))
                        || (stop.getSourceId() != null
                            && unavailabilityService.isSourceUnavailable(stop.getSourceId(), request.expectedAt())))
                .map(WaterVisitorStop::getStopName)
                .toList();
        boolean conflict = !conflictingZones.isEmpty() || !conflictingStops.isEmpty();

        WaterVisitorEvent event = visitorEventRepository.findByVisitId(visitId).orElseGet(WaterVisitorEvent::new);
        boolean created = event.getId() == null;
        event.setVisitId(visitId);
        event.setGroupName(request.groupName());
        event.setExpectedAt(request.expectedAt());
        event.setZones(zones.stream().map(UUID::toString).collect(Collectors.joining(LIST_SEPARATOR)));
        event.setStops(String.join(LIST_SEPARATOR, stops));
        event.setConflict(conflict);
        WaterVisitorEvent saved = visitorEventRepository.save(event);

        // The title carries the group: AlertService de-duplicates on the title, so a re-sent visit
        // does not notify twice while two different groups each get their notification.
        String group = request.groupName() == null || request.groupName().isBlank() ? visitId : request.groupName().trim();
        group = group.length() > 80 ? group.substring(0, 80) : group;
        String title = (conflict
                ? "Visitor group expected during water unavailability - "
                : "Visitor group near water facilities - ") + group;
        List<String> stopNames = mappedStops.stream().map(WaterVisitorStop::getStopName).toList();
        alertService.raiseOnce(conflict ? "warning" : "info", title,
                group + " expected at " + request.expectedAt()
                        + (stopNames.isEmpty() ? "" : " at " + String.join(", ", stopNames))
                        + (conflict ? ", water unavailable at "
                            + (conflictingStops.isEmpty() ? conflictingZones.toString() : String.join(", ", conflictingStops)) : ""),
                "/watersupply/visitors");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("eventId", saved.getId());
        response.put("visitId", saved.getVisitId());
        response.put("created", created);
        response.put("expectedAt", saved.getExpectedAt());
        response.put("zones", zones);
        response.put("stops", stopNames);
        response.put("ignoredStops", ignoredStops);
        response.put("conflict", saved.getConflict());
        response.put("conflictingZones", conflictingZones);
        response.put("conflictingStops", conflictingStops);
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
