package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterUnavailability;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterUnavailabilityRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Planned water unavailability (outage or maintenance) on a farm, a source or a zone.
 *
 * <p>Distinct from {@code DripMaintenanceLog}, which records interventions already done: an
 * unavailability is a forward-looking period that blocks water use. It is read by Visitor Management
 * (V6), checked before a valve command (6.1) and before an automatic irrigation (5.1). A farm-wide
 * period also applies to every zone of that farm. Cancelled periods are kept for history but never
 * block anything.</p>
 */
@Service
@Transactional(readOnly = true)
public class WaterUnavailabilityService {
    static final String SCOPE_FARM = "farm";
    static final String SCOPE_SOURCE = "source";
    static final String SCOPE_ZONE = "zone";
    private static final List<String> SCOPES = List.of(SCOPE_FARM, SCOPE_SOURCE, SCOPE_ZONE);
    private static final List<String> STATUSES = List.of("planned", "active", "cancelled");

    private final WaterUnavailabilityRepository outageRepository;
    private final FarmRepository farmRepository;
    private final WaterSourceRepository sourceRepository;
    private final FieldZoneRepository zoneRepository;
    private final FieldRepository fieldRepository;
    private final IrrigationScheduleRepository scheduleRepository;
    private final IrrigationService irrigationService;
    private final AlertService alertService;

    public WaterUnavailabilityService(WaterUnavailabilityRepository outageRepository, FarmRepository farmRepository,
                                      WaterSourceRepository sourceRepository, FieldZoneRepository zoneRepository,
                                      FieldRepository fieldRepository, IrrigationScheduleRepository scheduleRepository,
                                      IrrigationService irrigationService, AlertService alertService) {
        this.outageRepository = outageRepository;
        this.farmRepository = farmRepository;
        this.sourceRepository = sourceRepository;
        this.zoneRepository = zoneRepository;
        this.fieldRepository = fieldRepository;
        this.scheduleRepository = scheduleRepository;
        this.irrigationService = irrigationService;
        this.alertService = alertService;
    }

    /**
     * Periods matching every given filter: overlapping [from, to[, in force at {@code activeOn}
     * (cancelled excluded), concerning a zone or a source (directly or through their farm).
     */
    public List<WaterUnavailabilityResponse> search(Instant from, Instant to, UUID zoneId, UUID sourceId, Instant activeOn) {
        UUID zoneFarmId = zoneId == null ? null : farmIdOfZone(zoneId);
        UUID sourceFarmId = sourceId == null ? null : sourceRepository.findById(sourceId)
                .orElseThrow(() -> new NotFoundException("WaterSource")).getFarmId();
        Specification<WaterUnavailability> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThan(root.<Instant>get("endAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<Instant>get("startAt"), to));
            }
            if (activeOn != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Instant>get("startAt"), activeOn));
                predicates.add(cb.greaterThan(root.<Instant>get("endAt"), activeOn));
                predicates.add(cb.notEqual(cb.lower(root.get("status")), "cancelled"));
            }
            if (zoneId != null) {
                predicates.add(cb.or(scopeIs(root, cb, SCOPE_ZONE, zoneId), scopeIs(root, cb, SCOPE_FARM, zoneFarmId)));
            }
            if (sourceId != null) {
                predicates.add(cb.or(scopeIs(root, cb, SCOPE_SOURCE, sourceId), scopeIs(root, cb, SCOPE_FARM, sourceFarmId)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return outageRepository.findAll(spec, Sort.by("startAt")).stream()
                .map(WaterUnavailabilityResponse::from)
                .toList();
    }

    /** True when a non-cancelled period covers the zone (or its farm) at the given instant. */
    public boolean isZoneUnavailable(UUID zoneId, Instant at) {
        return !search(null, null, zoneId, null, at).isEmpty();
    }

    /** True when a non-cancelled period covers the source (or its farm) at the given instant. */
    public boolean isSourceUnavailable(UUID sourceId, Instant at) {
        return !search(null, null, null, sourceId, at).isEmpty();
    }

    /** Non-cancelled periods in force at the given instant, every scope. */
    public List<WaterUnavailabilityResponse> activeAt(Instant at) {
        return outageRepository.activeAt(at).stream().map(WaterUnavailabilityResponse::from).toList();
    }

    @Transactional
    public WaterUnavailabilityResponse create(WaterUnavailabilityRequest request) {
        WaterUnavailability outage = apply(new WaterUnavailability(), request);
        rejectOverlap(outage, null);
        WaterUnavailability saved = outageRepository.save(outage);
        int postponed = Boolean.TRUE.equals(request.postponeIrrigations()) ? postponeIrrigations(saved) : 0;
        alertService.raise("warning", "Water unavailability planned",
                saved.getScope() + " " + saved.getScopeId() + " unavailable from " + saved.getStartAt()
                        + " to " + saved.getEndAt()
                        + (saved.getReason() == null ? "" : " (" + saved.getReason() + ")")
                        + (postponed == 0 ? "" : ", " + postponed + " irrigation(s) postponed"),
                "/watersupply/outages");
        return WaterUnavailabilityResponse.from(saved);
    }

    public WaterUnavailabilityResponse get(UUID id) {
        return WaterUnavailabilityResponse.from(getEntity(id));
    }

    @Transactional
    public WaterUnavailabilityResponse update(UUID id, WaterUnavailabilityRequest request) {
        WaterUnavailability outage = apply(getEntity(id), request);
        rejectOverlap(outage, id);
        WaterUnavailability saved = outageRepository.save(outage);
        if (Boolean.TRUE.equals(request.postponeIrrigations())) {
            postponeIrrigations(saved);
        }
        return WaterUnavailabilityResponse.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        outageRepository.delete(getEntity(id));
    }

    private WaterUnavailability apply(WaterUnavailability outage, WaterUnavailabilityRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        String scope = request.scope().trim().toLowerCase(Locale.ROOT);
        if (!SCOPES.contains(scope)) {
            throw new IllegalArgumentException("scope must be farm, source or zone");
        }
        String status = request.status() == null ? "planned" : request.status().trim().toLowerCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("status must be planned, active or cancelled");
        }
        requireTarget(scope, request.scopeId());
        outage.setScope(scope);
        outage.setScopeId(request.scopeId());
        outage.setStartAt(request.startAt());
        outage.setEndAt(request.endAt());
        outage.setReason(request.reason());
        outage.setStatus(status);
        return outage;
    }

    private void rejectOverlap(WaterUnavailability outage, UUID selfId) {
        if ("cancelled".equals(outage.getStatus())) {
            return;
        }
        boolean overlaps = outageRepository.overlaps(outage.getScope(), outage.getScopeId(), outage.getStartAt(), outage.getEndAt())
                .stream().anyMatch(existing -> !existing.getId().equals(selfId));
        if (overlaps) {
            throw new IllegalArgumentException("Water unavailability overlaps an existing period on the same scope");
        }
    }

    /** Postpones the scheduled/running irrigations of the impacted zones during the period. */
    private int postponeIrrigations(WaterUnavailability outage) {
        if ("cancelled".equals(outage.getStatus())) {
            return 0;
        }
        List<UUID> zoneIds = switch (outage.getScope()) {
            case SCOPE_ZONE -> List.of(outage.getScopeId());
            case SCOPE_FARM -> fieldRepository.findByFarmId(outage.getScopeId()).stream()
                    .flatMap(field -> zoneRepository.findByFieldId(field.getId()).stream())
                    .map(Zone::getId)
                    .toList();
            // A source is not linked to zones in the schema: nothing can be postponed safely.
            default -> List.of();
        };
        String reason = "Water unavailability" + (outage.getReason() == null ? "" : ": " + outage.getReason());
        int count = 0;
        for (UUID zoneId : zoneIds) {
            for (var schedule : scheduleRepository.findOpenForZoneBetween(zoneId, outage.getStartAt(), outage.getEndAt())) {
                irrigationService.postpone(schedule.getId(), reason);
                count++;
            }
        }
        return count;
    }

    private void requireTarget(String scope, UUID scopeId) {
        boolean exists = switch (scope) {
            case SCOPE_FARM -> farmRepository.existsById(scopeId);
            case SCOPE_SOURCE -> sourceRepository.existsById(scopeId);
            default -> zoneRepository.existsById(scopeId);
        };
        if (!exists) {
            throw new NotFoundException(switch (scope) {
                case SCOPE_FARM -> "Farm";
                case SCOPE_SOURCE -> "WaterSource";
                default -> "Zone";
            });
        }
    }

    private UUID farmIdOfZone(UUID zoneId) {
        Zone zone = zoneRepository.findById(zoneId).orElseThrow(() -> new NotFoundException("Zone"));
        return Optional.ofNullable(zone.getFieldId()).flatMap(fieldRepository::findById).map(Field::getFarmId).orElse(null);
    }

    private static Predicate scopeIs(jakarta.persistence.criteria.Root<WaterUnavailability> root,
                                     jakarta.persistence.criteria.CriteriaBuilder cb, String scope, UUID scopeId) {
        if (scopeId == null) {
            return cb.disjunction();
        }
        return cb.and(cb.equal(root.get("scope"), scope), cb.equal(root.get("scopeId"), scopeId));
    }

    private WaterUnavailability getEntity(UUID id) {
        return outageRepository.findById(id).orElseThrow(() -> new NotFoundException("WaterUnavailability"));
    }
}
