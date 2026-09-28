package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FindingTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PreventiveTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.HealthFindingNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthTreatmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class HealthTreatmentService {

    /** {@code source} of every treatment written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private final HealthTreatmentRepository healthTreatmentRepository;
    private final HealthFindingRepository healthFindingRepository;
    private final HealthIssueService healthIssueService;

    /**
     * A code as stored: trimmed and upper-cased, so {@code " kg "} becomes
     * {@code "KG"}. The request validation already refused any other form.
     */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     * Also turns a blank list filter into no filter, as for the other lists of
     * the module.
     *
     * @param value the text as received, possibly {@code null}
     * @return the trimmed text, or {@code null} if it was null or blank
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Maps a stored treatment to its API representation. The exact decimals of
     * the database are sent as JSON numbers, and the harvest date is computed.
     *
     * @param treatment the stored treatment, with its targeted issue loaded
     * @return the API representation of that treatment
     */
    private static HealthTreatmentResponse toResponse(HealthTreatment treatment) {
        HealthIssueReference target = treatment.getTargetIssue();
        HealthFinding finding = treatment.getFinding();
        return new HealthTreatmentResponse(
                treatment.getId(),
                treatment.getFarmId(),
                treatment.getBlockCode(),
                target.getCode(),
                target.getName(),
                treatment.getTargetOtherLabel(),
                finding == null ? null : finding.getId(),
                treatment.getTreatedOn(),
                treatment.getProductName(),
                treatment.getActiveIngredient(),
                treatment.getQuantity().doubleValue(),
                treatment.getUnit(),
                treatment.getPreHarvestIntervalDays(),
                HealthCalculator.harvestAllowedFrom(treatment.getTreatedOn(), treatment.getPreHarvestIntervalDays()),
                treatment.getApplicator(),
                treatment.getEquipment(),
                treatment.getSource(),
                treatment.getLastUpdated());
    }

    /**
     * A new treatment with the columns shared by both routes. The quantity has
     * at most 3 decimals and the interval is a whole number, as the request
     * validation guarantees, so both conversions are exact.
     */
    private static HealthTreatment newTreatment(LocalDate treatedOn, String productName, String activeIngredient,
                                                Double quantity, String unit, Double preHarvestIntervalDays,
                                                String applicator, String equipment, Instant now) {
        HealthTreatment treatment = new HealthTreatment();
        treatment.setTreatedOn(treatedOn);
        treatment.setProductName(productName.trim());
        treatment.setActiveIngredient(activeIngredient.trim());
        treatment.setQuantity(BigDecimal.valueOf(quantity));
        treatment.setUnit(TreatmentUnit.valueOf(normalizeCode(unit)));
        treatment.setPreHarvestIntervalDays(preHarvestIntervalDays.intValue());
        treatment.setApplicator(applicator.trim());
        treatment.setEquipment(normalizeOptionalText(equipment));
        treatment.setSource(USER_ENTRY_SOURCE);
        treatment.setLastUpdated(now);
        return treatment;
    }

    /**
     * Records a treatment that answers a finding, in a single transaction.
     *
     * <p>The farm, the block, the targeted issue and its label are copied from
     * the finding and its inspection. The treatment is refused when the finding
     * is already resolved, or when it predates the inspection; the same day is
     * accepted.
     *
     * @param findingId the finding the treatment answers
     * @param request   the treatment, already validated
     * @return the recorded treatment
     * @throws HealthFindingNotFoundException if no finding has this identifier
     * @throws BusinessRuleException          if the finding is resolved, or the
     *                                        treatment predates the inspection
     */
    @Transactional
    public HealthTreatmentResponse recordFindingTreatment(Long findingId, FindingTreatmentRequest request) {
        return recordFindingTreatment(findingId, request, Instant.now());
    }

    /**
     * Same as {@link #recordFindingTreatment(Long, FindingTreatmentRequest)}, at
     * an explicit write time so the {@code lastUpdated} value can be tested.
     */
    HealthTreatmentResponse recordFindingTreatment(Long findingId, FindingTreatmentRequest request, Instant now) {
        HealthFinding finding = healthFindingRepository.findById(findingId)
                .orElseThrow(() -> new HealthFindingNotFoundException(findingId));
        HealthInspection inspection = finding.getInspection();
        if (finding.getResolvedOn() != null) {
            throw new BusinessRuleException("Finding " + findingId + " was resolved on " + finding.getResolvedOn()
                    + " and cannot be treated any more");
        }
        if (request.treatedOn().isBefore(inspection.getInspectedOn())) {
            throw new BusinessRuleException("The treatment date " + request.treatedOn()
                    + " is before the inspection date " + inspection.getInspectedOn() + " of finding " + findingId);
        }

        HealthTreatment treatment = newTreatment(request.treatedOn(), request.productName(),
                request.activeIngredient(), request.quantity(), request.unit(), request.preHarvestIntervalDays(),
                request.applicator(), request.equipment(), now);
        treatment.setFarmId(inspection.getFarmId());
        treatment.setBlockCode(inspection.getBlockCode());
        treatment.setTargetIssue(finding.getIssue());
        treatment.setTargetOtherLabel(finding.getOtherLabel());
        treatment.setFinding(finding);
        return toResponse(healthTreatmentRepository.save(treatment));
    }

    /**
     * Records a preventive treatment, which answers no finding.
     *
     * <p>The block needs no recorded planting. The targeted issue is a code of
     * the catalogue, compared without case; with {@code OTHER}, its label names
     * the problem.
     *
     * @param request the treatment, already validated
     * @return the recorded treatment
     * @throws BusinessRuleException if the targeted code is not in the catalogue
     */
    @Transactional
    public HealthTreatmentResponse recordPreventiveTreatment(PreventiveTreatmentRequest request) {
        return recordPreventiveTreatment(request, Instant.now());
    }

    /**
     * Same as {@link #recordPreventiveTreatment(PreventiveTreatmentRequest)}, at
     * an explicit write time so the {@code lastUpdated} value can be tested.
     */
    HealthTreatmentResponse recordPreventiveTreatment(PreventiveTreatmentRequest request, Instant now) {
        HealthIssueReference target = healthIssueService.getIssueByCode(request.targetIssueCode());

        HealthTreatment treatment = newTreatment(request.treatedOn(), request.productName(),
                request.activeIngredient(), request.quantity(), request.unit(), request.preHarvestIntervalDays(),
                request.applicator(), request.equipment(), now);
        treatment.setFarmId(request.farmId());
        treatment.setBlockCode(normalizeCode(request.blockCode()));
        treatment.setTargetIssue(target);
        treatment.setTargetOtherLabel(normalizeOptionalText(request.targetOtherLabel()));
        return toResponse(healthTreatmentRepository.save(treatment));
    }

    /**
     * Retrieves the treatment record, optionally filtered.
     *
     * <p>Every filter is independent and optional; a missing farm means every
     * farm, as for the other lists of the module. Both dates are included.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "C"}),
     *                  or {@code null} for every block
     * @param from      first treatment date, included, or {@code null}
     * @param to        last treatment date, included, or {@code null}
     * @return the matching treatments, ordered by date then identifier
     */
    public List<HealthTreatmentResponse> getAllTreatments(Integer farmId, String blockCode,
                                                          LocalDate from, LocalDate to) {
        return healthTreatmentRepository.findByOptionalFilters(farmId, normalizeOptionalText(blockCode), from, to)
                .stream()
                .map(HealthTreatmentService::toResponse)
                .toList();
    }
}
