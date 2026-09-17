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
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des quotas mensuels d'eau par ferme ou par zone (proposition P8).
 *
 * <p>Un quota fixe un volume maximal autorise pour un mois donne. Le service expose le CRUD,
 * le suivi de consommation du mois courant par quota, et declenche les alertes automatiques
 * {@code warning} a 80&nbsp;% et {@code critical} a 100&nbsp;% du quota via {@link AlertService}
 * (anti-doublon integre : une alerte deja levee n'est pas repetee a chaque mesure capteur).</p>
 */
@Service
@Transactional(readOnly = true)
public class WaterQuotaService {

    /** Seuil d'alerte "approche du quota" (fraction du quota). */
    public static final double WARNING_THRESHOLD = 0.80;

    /** Seuil d'alerte "quota atteint" (fraction du quota). */
    public static final double CRITICAL_THRESHOLD = 1.00;

    private static final String TARGET_FARM = "farm";
    private static final String TARGET_ZONE = "zone";

    private final WaterQuotaRepository quotaRepository;
    private final FarmRepository farmRepository;
    private final ZoneRepository zoneRepository;
    private final FieldRepository fieldRepository;
    private final WaterConsumptionRepository consumptionRepository;
    private final AlertService alertService;

    public WaterQuotaService(WaterQuotaRepository quotaRepository,
                             FarmRepository farmRepository,
                             ZoneRepository zoneRepository,
                             FieldRepository fieldRepository,
                             WaterConsumptionRepository consumptionRepository,
                             AlertService alertService) {
        this.quotaRepository = quotaRepository;
        this.farmRepository = farmRepository;
        this.zoneRepository = zoneRepository;
        this.fieldRepository = fieldRepository;
        this.consumptionRepository = consumptionRepository;
        this.alertService = alertService;
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
     * Cree un quota. Les erreurs de cible inconnue levent une {@link NotFoundException} (404),
     * une cible non coherente un {@link IllegalArgumentException} (400), et un doublon de mois
     * pour la meme cible est rejete par la contrainte d'unicite (409 via le gestionnaire global).
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
     * Suivi de tous les quotas pour le mois courant : consommation cumulee (mesures capteurs),
     * pourcentage utilise et statut d'alerte. C'est l'endpoint consomme par l'interface.
     */
    public List<Map<String, Object>> currentUsage() {
        return buildUsage(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1));
    }

    /**
     * Suivi d'un mois arbitraire : permet de consulter l'historique d'un mois clos.
     */
    public List<Map<String, Object>> usageForMonth(LocalDate monthStart) {
        return buildUsage(monthStart.withDayOfMonth(1));
    }

    /**
     * Verifie les seuils d'alerte pour la consommation enregistree d'une cible. Appele a chaque
     * ingestion d'une mesure de debit (capteurs IoT) et a chaque saisie manuelle de consommation.
     * Les alertes passent par {@link AlertService#raiseOnce} : pas de repetition toutes les minutes.
     */
    public void checkThresholds(UUID farmId, UUID zoneId, Instant consumptionDate) {
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
        String actionUrl = "/consommation";

        if (used >= CRITICAL_THRESHOLD * quotaLiters) {
            alertService.raiseOnce("critical", "Quota d'eau depasse - " + name,
                    String.format("Quota mensuel de %s atteint : %.0f L consommes sur %.0f L autorises (%.0f%%).",
                            name, used, quotaLiters, used / quotaLiters * 100),
                    actionUrl);
        } else if (used >= WARNING_THRESHOLD * quotaLiters) {
            alertService.raiseOnce("warning", "Quota d'eau bientot atteint - " + name,
                    String.format("Quota mensuel de %s bientot atteint : %.0f L consommes sur %.0f L autorises (%.0f%%).",
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

    /** Quota applicable : celui du mois demande s'il existe, sinon le plus recent anterieur. */
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
     * Consommation d'une zone : les mesures capteurs portent un farm_id et un source_id mais pas
     * de zone directe. On remonte donc de la zone a son champ, puis au farm du champ, et on
     * repartit la consommation du farm entre les zones de ce champ (les zones partagent l'eau de
     * la meme parcelle) : approximation documentee, a affiner avec des irrigations zonees.
     */
    private double zoneConsumption(Instant start, Instant end, UUID zoneId) {
        return zoneRepository.findById(zoneId)
                .flatMap(zone -> fieldRepository.findById(zone.getFieldId())
                        .map(field -> consumptionRepository
                                .sumConsumptionByFarmIdBetween(start, end, field.getFarmId())
                                / zoneCount(zone.getFieldId())))
                .orElse(0d);
    }

    private long zoneCount(UUID fieldId) {
        long count = zoneRepository.findByFieldId(fieldId).size();
        return count == 0 ? 1 : count;
    }

    private String targetName(String targetType, UUID targetId) {
        return switch (targetType) {
            case TARGET_FARM -> farmRepository.findById(targetId)
                    .map(farm -> farm.getName()).orElse("Ferme inconnue");
            case TARGET_ZONE -> zoneRepository.findById(targetId)
                    .map(zone -> zone.getName()).orElse("Zone inconnue");
            default -> "Cible inconnue";
        };
    }

    private String quotaLabel(String label, String targetType, UUID targetId, LocalDate month) {
        if (label != null && !label.isBlank()) {
            return label;
        }
        String prefix = TARGET_FARM.equals(targetType) ? "Quota ferme" : "Quota zone";
        return prefix + " - " + targetName(targetType, targetId) + " - " + YearMonth.from(month);
    }

    private String normalizeTargetType(String targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("Le type de cible du quota est obligatoire (farm ou zone).");
        }
        String normalized = targetType.trim().toLowerCase();
        if (!TARGET_FARM.equals(normalized) && !TARGET_ZONE.equals(normalized)) {
            throw new IllegalArgumentException("Type de cible invalide : farm ou zone attendus.");
        }
        return normalized;
    }

    private void requireFirstDayOfMonth(LocalDate quotaMonth) {
        if (quotaMonth.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("Le mois du quota doit etre le premier jour du mois (YYYY-MM-01).");
        }
    }

    private void requireTarget(String targetType, UUID targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException("La cible du quota est obligatoire.");
        }
        boolean exists = switch (targetType) {
            case TARGET_FARM -> farmRepository.findById(targetId).isPresent();
            case TARGET_ZONE -> zoneRepository.findById(targetId).isPresent();
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
