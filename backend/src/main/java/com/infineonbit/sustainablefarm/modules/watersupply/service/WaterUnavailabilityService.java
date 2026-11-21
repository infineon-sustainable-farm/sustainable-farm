package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.UnavailabilityScope;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.UnavailabilityStatus;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterUnavailability;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IrrigationScheduleRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterUnavailabilityRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

    private final WaterUnavailabilityRepository outageRepository;
    private final WaterSourceRepository sourceRepository;
    private final FarmDirectory farmDirectory;
    private final IrrigationScheduleRepository scheduleRepository;
    private final IrrigationService irrigationService;
    private final AlertService alertService;

    public WaterUnavailabilityService(WaterUnavailabilityRepository outageRepository,
                                      WaterSourceRepository sourceRepository, FarmDirectory farmDirectory,
                                      IrrigationScheduleRepository scheduleRepository,
                                      IrrigationService irrigationService, AlertService alertService) {
        this.outageRepository = outageRepository;
        this.sourceRepository = sourceRepository;
        this.farmDirectory = farmDirectory;
        this.scheduleRepository = scheduleRepository;
        this.irrigationService = irrigationService;
        this.alertService = alertService;
    }

    /**
     * Periods matching every given filter: overlapping [from, to[, in force at {@code activeOn}
     * (cancelled excluded), concerning a zone or a source (directly or through their farm).
     */
    public List<WaterUnavailabilityResponse> search(Instant from, Instant to, Long zoneId, Long sourceId, Instant activeOn) {
        Long zoneFarmId = zoneId == null ? null : farmIdOfZone(zoneId);
        Long sourceFarmId = sourceId == null ? null : sourceRepository.findById(sourceId)
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
                predicates.add(cb.notEqual(root.get("status"), UnavailabilityStatus.CANCELLED));
            }
            if (zoneId != null) {
                predicates.add(cb.or(scopeIs(root, cb, UnavailabilityScope.ZONE, zoneId), scopeIs(root, cb, UnavailabilityScope.FARM, zoneFarmId)));
            }
            if (sourceId != null) {
                predicates.add(cb.or(scopeIs(root, cb, UnavailabilityScope.SOURCE, sourceId), scopeIs(root, cb, UnavailabilityScope.FARM, sourceFarmId)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return outageRepository.findAll(spec, Sort.by("startAt")).stream()
                .map(WaterUnavailabilityResponse::from)
                .toList();
    }

    /** True when a non-cancelled period covers the zone (or its farm) at the given instant. */
    public boolean isZoneUnavailable(Long zoneId, Instant at) {
        return !search(null, null, zoneId, null, at).isEmpty();
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

    public WaterUnavailabilityResponse get(Long id) {
        return WaterUnavailabilityResponse.from(getEntity(id));
    }

    @Transactional
    public WaterUnavailabilityResponse update(Long id, WaterUnavailabilityRequest request) {
        WaterUnavailability outage = apply(getEntity(id), request);
        rejectOverlap(outage, id);
        WaterUnavailability saved = outageRepository.save(outage);
        if (Boolean.TRUE.equals(request.postponeIrrigations())) {
            postponeIrrigations(saved);
        }
        return WaterUnavailabilityResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        outageRepository.delete(getEntity(id));
    }

    private WaterUnavailability apply(WaterUnavailability outage, WaterUnavailabilityRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        UnavailabilityScope scope = UnavailabilityScope.of(request.scope());
        if (scope == null) {
            throw new IllegalArgumentException("scope must be farm, source or zone");
        }
        UnavailabilityStatus status = request.status() == null ? UnavailabilityStatus.PLANNED
                : UnavailabilityStatus.of(request.status());
        if (status == null) {
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

    private void rejectOverlap(WaterUnavailability outage, Long selfId) {
        if (outage.getStatus() == UnavailabilityStatus.CANCELLED) {
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
        if (outage.getStatus() == UnavailabilityStatus.CANCELLED) {
            return 0;
        }
        List<Long> zoneIds = switch (outage.getScope()) {
            case ZONE -> List.of(outage.getScopeId());
            case FARM -> farmDirectory.listZonesOfFarm(outage.getScopeId()).stream()
                    .map(ZoneDto::id)
                    .toList();
            // A source is not linked to zones in the schema: nothing can be postponed safely.
            default -> List.of();
        };
        String reason = "Water unavailability" + (outage.getReason() == null ? "" : ": " + outage.getReason());
        int count = 0;
        for (Long zoneId : zoneIds) {
            for (var schedule : scheduleRepository.findOpenForZoneBetween(zoneId, outage.getStartAt(), outage.getEndAt())) {
                irrigationService.postpone(schedule.getId(), reason);
                count++;
            }
        }
        return count;
    }

    private void requireTarget(UnavailabilityScope scope, Long scopeId) {
        boolean exists = switch (scope) {
            case FARM -> farmDirectory.farmExists(scopeId);
            case SOURCE -> sourceRepository.existsById(scopeId);
            case ZONE -> farmDirectory.zoneExists(scopeId);
        };
        if (!exists) {
            throw new NotFoundException(switch (scope) {
                case FARM -> "Farm";
                case SOURCE -> "WaterSource";
                case ZONE -> "Zone";
            });
        }
    }

    private Long farmIdOfZone(Long zoneId) {
        return farmDirectory.findZone(zoneId).orElseThrow(() -> new NotFoundException("Zone")).farmId();
    }

    private static Predicate scopeIs(jakarta.persistence.criteria.Root<WaterUnavailability> root,
                                     jakarta.persistence.criteria.CriteriaBuilder cb, UnavailabilityScope scope, Long scopeId) {
        if (scopeId == null) {
            return cb.disjunction();
        }
        return cb.and(cb.equal(root.get("scope"), scope), cb.equal(root.get("scopeId"), scopeId));
    }

    private WaterUnavailability getEntity(Long id) {
        return outageRepository.findById(id).orElseThrow(() -> new NotFoundException("WaterUnavailability"));
    }
}
