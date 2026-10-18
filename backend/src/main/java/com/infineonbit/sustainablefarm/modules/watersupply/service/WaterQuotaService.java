package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQuotaCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQuotaResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQuotaUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQuota;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQuotaRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monthly water quota management per farm or per zone (proposal P8).
 *
 * <p>A quota sets a maximum allowed volume for a given month. The service exposes the CRUD,
 * the current-month consumption tracking per quota, and triggers the automatic
 * {@code warning} at 80&nbsp;% and {@code critical} at 100&nbsp;% of the quota through {@link AlertService}
 * (built-in anti-duplicate: an already raised alert is not repeated at every sensor measurement).</p>
 */
@Service
@Transactional(readOnly = true)
public class WaterQuotaService {

    /** Alert threshold "quota approaching" (fraction of the quota). */
    public static final double WARNING_THRESHOLD = 0.80;

    /** Alert threshold "quota reached" (fraction of the quota). */
    public static final double CRITICAL_THRESHOLD = 1.00;

    private static final String TARGET_FARM = "farm";
    private static final String TARGET_ZONE = "zone";

    private final WaterQuotaRepository quotaRepository;
    private final FarmRepository farmRepository;
    private final FieldZoneRepository fieldZoneRepository;
    private final FieldRepository fieldRepository;
    private final WaterConsumptionRepository consumptionRepository;
    private final AlertService alertService;

    /** Last threshold evaluation per target: avoids recomputing at every meter pulse. */
    private final Map<String, Instant> lastThresholdChecks = new java.util.concurrent.ConcurrentHashMap<>();

    /** Minimum window between two threshold evaluations for the same target (seconds, 0 = disabled). */
    private final long checkThrottleSeconds;

    public WaterQuotaService(WaterQuotaRepository quotaRepository,
                             FarmRepository farmRepository,
                             FieldZoneRepository fieldZoneRepository,
                             FieldRepository fieldRepository,
                             WaterConsumptionRepository consumptionRepository,
                             AlertService alertService,
                             @Value("${app.quota.check-throttle-seconds:60}") long checkThrottleSeconds) {
        this.checkThrottleSeconds = Math.max(0, checkThrottleSeconds);
        this.alertService = alertService;
        this.consumptionRepository = consumptionRepository;
        this.fieldRepository = fieldRepository;
        this.fieldZoneRepository = fieldZoneRepository;
        this.farmRepository = farmRepository;
        this.quotaRepository = quotaRepository;
    }

    public List<WaterQuotaResponse> findQuotas() {
        return quotaRepository.findAll().stream()
                .sorted((a, b) -> {
                    int byMonth = b.getQuotaMonth().compareTo(a.getQuotaMonth());
                    return byMonth != 0 ? byMonth : a.getLabel().compareToIgnoreCase(b.getLabel());
                })
                .map(WaterQuotaResponse::from)
                .toList();
    }

    public List<WaterQuotaResponse> findQuotas(String targetType, UUID targetId) {
        String type = normalizeTargetType(targetType);
        return quotaRepository.findByTargetTypeAndTargetIdOrderByQuotaMonthDesc(type, targetId).stream()
                .map(WaterQuotaResponse::from)
                .toList();
    }

    public WaterQuotaResponse getQuota(UUID quotaId) {
        return WaterQuotaResponse.from(getQuotaEntity(quotaId));
    }

    /**
     * Creates a quota. Unknown target errors raise a {@link NotFoundException} (404),
     * an inconsistent target an {@link IllegalArgumentException} (400), and a duplicate month
     * for the same target is rejected by the uniqueness constraint (409 via the global handler).
     */
    @Transactional
    public WaterQuotaResponse createQuota(WaterQuotaCreateRequest request) {
        String targetType = normalizeTargetType(request.targetType());
        requireTarget(targetType, request.targetId());
        requireFirstDayOfMonth(request.quotaMonth());

        WaterQuota quota = new WaterQuota();
        quota.setTargetType(targetType);
        quota.setTargetId(request.targetId());
        quota.setQuotaMonth(request.quotaMonth());
        quota.setQuotaLiters(request.quotaLiters());
        quota.setLabel(quotaLabel(request.label(), targetType, request.targetId(), request.quotaMonth()));
        return WaterQuotaResponse.from(quotaRepository.save(quota));
    }

    @Transactional
    public WaterQuotaResponse updateQuota(UUID quotaId, WaterQuotaUpdateRequest request) {
        WaterQuota quota = getQuotaEntity(quotaId);
        if (request.quotaMonth() != null) {
            requireFirstDayOfMonth(request.quotaMonth());
            quota.setQuotaMonth(request.quotaMonth());
        }
        if (request.quotaLiters() != null) {
            quota.setQuotaLiters(request.quotaLiters());
        }
        if (request.label() != null) {
            quota.setLabel(request.label());
        }
        return WaterQuotaResponse.from(quotaRepository.save(quota));
    }

    @Transactional
    public void deleteQuota(UUID quotaId) {
        quotaRepository.delete(getQuotaEntity(quotaId));
    }

    /**
     * Tracking of all quotas for the current month: cumulative consumption (sensor measurements),
     * percentage used and alert status. This is the endpoint consumed by the UI.
     */
    public List<Map<String, Object>> currentUsage() {
        return buildUsage(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1));
    }

    /**
     * Tracking of an arbitrary month: allows consulting the history of a closed month.
     */
    public List<Map<String, Object>> usageForMonth(LocalDate monthStart) {
        return buildUsage(monthStart.withDayOfMonth(1));
    }

    /**
     * Checks the alert thresholds for the recorded consumption of a target. Called at each
     * ingestion of a flow measurement (IoT sensors) and at each manual consumption entry.
     * Alerts go through {@link AlertService#raiseOnce}: no repetition every minute.
     */
    public void checkThresholds(UUID farmId, UUID zoneId, Instant consumptionDate) {
        // A flow meter can send several measurements per minute: recomputing the quota at
        // every pulse would not change the result and would cost one aggregation per measurement.
        if (throttled(farmId, zoneId)) {
            return;
        }
        LocalDate month = consumptionDate == null
                ? LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
                : consumptionDate.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        if (zoneId != null) {
            checkThreshold("zone", zoneId, month);
        }
        if (farmId != null) {
            checkThreshold("farm", farmId, month);
        }
    }

    /** True if the target was already evaluated within the throttle window (0 disables the mechanism). */
    private boolean throttled(UUID farmId, UUID zoneId) {
        if (checkThrottleSeconds == 0) {
            return false;
        }
        String key = String.valueOf(farmId) + ':' + zoneId;
        Instant now = Instant.now();
        Instant previous = lastThresholdChecks.get(key);
        if (previous != null && previous.plusSeconds(checkThrottleSeconds).isAfter(now)) {
            return true;
        }
        lastThresholdChecks.put(key, now);
        return false;
    }

    private void checkThreshold(String targetType, UUID targetId, LocalDate monthStart) {
        WaterQuota quota = currentQuota(targetType, targetId, monthStart);
        if (quota == null) {
            return;
        }
        double quotaLiters = quota.getQuotaLiters() == null ? 0 : quota.getQuotaLiters();
        if (quotaLiters <= 0) {
            return;
        }
        double used = usedLiters(targetType, targetId, monthStart);
        String name = targetName(targetType, targetId);
        String actionUrl = "/consumption";

        if (used >= CRITICAL_THRESHOLD * quotaLiters) {
            alertService.raiseOnce("critical", "Water quota exceeded - " + name,
                    String.format("Monthly quota for %s reached: %.0f L used out of %.0f L allowed (%.0f%%).",
                            name, used, quotaLiters, used / quotaLiters * 100),
                    actionUrl);
        } else if (used >= WARNING_THRESHOLD * quotaLiters) {
            alertService.raiseOnce("warning", "Water quota almost reached - " + name,
                    String.format("Monthly quota for %s almost reached: %.0f L used out of %.0f L allowed (%.0f%%).",
                            name, used, quotaLiters, used / quotaLiters * 100),
                    actionUrl);
        }
    }

    private List<Map<String, Object>> buildUsage(LocalDate monthStart) {
        LocalDate monthEnd = monthStart.plusMonths(1);
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (WaterQuota quota : quotaRepository.findAll()) {
            if (quota.getQuotaMonth() == null || quota.getQuotaMonth().isBefore(monthStart)
                    || !quota.getQuotaMonth().isBefore(monthEnd)) {
                continue;
            }
            double quotaLiters = quota.getQuotaLiters() == null ? 0 : quota.getQuotaLiters();
            double used = usedLiters(quota.getTargetType(), quota.getTargetId(), monthStart);
            double ratio = quotaLiters <= 0 ? 0 : used / quotaLiters;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("quota_id", quota.getId());
            item.put("target_type", quota.getTargetType());
            item.put("target_id", quota.getTargetId());
            item.put("target_name", targetName(quota.getTargetType(), quota.getTargetId()));
            item.put("quota_month", quota.getQuotaMonth().toString());
            item.put("quota_liters", quotaLiters);
            item.put("used_liters", Math.round(used));
            item.put("remaining_liters", Math.round(quotaLiters - used));
            item.put("usage_percentage", Math.round(ratio * 100));
            item.put("status", quotaLiters <= 0 ? "unknown"
                    : ratio >= CRITICAL_THRESHOLD ? "exceeded"
                    : ratio >= WARNING_THRESHOLD ? "warning" : "ok");
            items.add(item);
        }
        return items;
    }

    /** Quota that applies: the requested month's if it exists, otherwise the most recent earlier one. */
    private WaterQuota currentQuota(String targetType, UUID targetId, LocalDate monthStart) {
        return quotaRepository
                .findFirstByTargetTypeAndTargetIdAndQuotaMonthBetweenOrderByQuotaMonthDesc(
                        targetType, targetId, monthStart, monthStart.plusMonths(1))
                .orElse(null);
    }

    private double usedLiters(String targetType, UUID targetId, LocalDate monthStart) {
        Instant start = monthStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = monthStart.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return switch (targetType) {
            case TARGET_FARM -> consumptionRepository.sumConsumptionByFarmIdBetween(start, end, targetId);
            case TARGET_ZONE -> zoneConsumption(start, end, targetId);
            default -> 0d;
        };
    }

    /**
     * Consumption of a zone: sensor measurements carry a farm_id and a source_id but no
     * direct zone. So we walk from the zone to its field, then to the field's farm, and split
     * the farm's consumption between the zones of that field (zones share the water of the
     * same plot): documented approximation, to be refined with zone-level irrigations.
     */
    private double zoneConsumption(Instant start, Instant end, UUID zoneId) {
        return fieldZoneRepository.findById(zoneId)
                .flatMap(zone -> fieldRepository.findById(zone.getFieldId())
                        .map(field -> consumptionRepository
                                .sumConsumptionByFarmIdBetween(start, end, field.getFarmId())
                                / zoneCount(zone.getFieldId())))
                .orElse(0d);
    }

    private long zoneCount(UUID fieldId) {
        long count = fieldZoneRepository.findByFieldId(fieldId).size();
        return count == 0 ? 1 : count;
    }

    private String targetName(String targetType, UUID targetId) {
        return switch (targetType) {
            case TARGET_FARM -> farmRepository.findById(targetId)
                    .map(farm -> farm.getName()).orElse("Unknown farm");
            case TARGET_ZONE -> fieldZoneRepository.findById(targetId)
                    .map(zone -> zone.getName()).orElse("Unknown zone");
            default -> "Unknown target";
        };
    }

    private String quotaLabel(String label, String targetType, UUID targetId, LocalDate month) {
        if (label != null && !label.isBlank()) {
            return label;
        }
        String prefix = TARGET_FARM.equals(targetType) ? "Farm quota" : "Zone quota";
        return prefix + " - " + targetName(targetType, targetId) + " - " + YearMonth.from(month);
    }

    private String normalizeTargetType(String targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("The quota target type is required (farm or zone).");
        }
        String normalized = targetType.trim().toLowerCase();
        if (!TARGET_FARM.equals(normalized) && !TARGET_ZONE.equals(normalized)) {
            throw new IllegalArgumentException("Invalid target type: farm or zone expected.");
        }
        return normalized;
    }

    private void requireFirstDayOfMonth(LocalDate quotaMonth) {
        if (quotaMonth.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("The quota month must be the first day of the month (YYYY-MM-01).");
        }
    }

    private void requireTarget(String targetType, UUID targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException("The quota target is required.");
        }
        boolean exists = switch (targetType) {
            case TARGET_FARM -> farmRepository.findById(targetId).isPresent();
            case TARGET_ZONE -> fieldZoneRepository.findById(targetId).isPresent();
            default -> false;
        };
        if (!exists) {
            throw new NotFoundException(TARGET_FARM.equals(targetType) ? "Farm" : "Zone");
        }
    }

    private WaterQuota getQuotaEntity(UUID quotaId) {
        return quotaRepository.findById(quotaId)
                .orElseThrow(() -> new NotFoundException("WaterQuota"));
    }
}
