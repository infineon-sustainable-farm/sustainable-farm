package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FindingResolutionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import com.infineonbit.sustainablefarm.modules.plants.exception.HealthFindingNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthTreatmentRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthCalculator.TreatmentSummary;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class HealthFindingService {

    private final HealthFindingRepository healthFindingRepository;
    private final HealthTreatmentRepository healthTreatmentRepository;

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     * Also turns a blank list filter into no filter, as for the other lists of
     * the module.
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Maps a stored finding to its API representation.
     *
     * @param finding    the stored finding, with its inspection and issue loaded
     * @param treatments what its treatments add up to
     * @return the API representation of that finding
     */
    private static HealthFindingResponse toResponse(HealthFinding finding, TreatmentSummary treatments) {
        HealthInspection inspection = finding.getInspection();
        HealthIssueReference issue = finding.getIssue();
        return new HealthFindingResponse(
                finding.getId(),
                inspection.getId(),
                inspection.getFarmId(),
                inspection.getBlockCode(),
                inspection.getInspectedOn(),
                issue.getCode(),
                issue.getName(),
                issue.getKind(),
                finding.getOtherLabel(),
                finding.getTreeLabel(),
                HealthCalculator.status(treatments.count(), finding.getResolvedOn()),
                treatments.count(),
                treatments.lastTreatedOn(),
                treatments.harvestAllowedFrom(),
                finding.getResolvedOn(),
                finding.getResolutionNote());
    }

    /**
     * Maps stored findings to their API representation, in the given order.
     * The treatments of every finding come from one query, skipped when there is
     * no finding.
     *
     * @param findings the stored findings, with their inspection and issue loaded
     * @return their API representations
     */
    public List<HealthFindingResponse> toResponses(List<HealthFinding> findings) {
        if (findings.isEmpty()) {
            return List.of();
        }
        Map<Long, List<HealthTreatment>> treatmentsByFinding = healthTreatmentRepository
                .findByFindingIds(findings.stream().map(HealthFinding::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(treatment -> treatment.getFinding().getId()));
        return findings.stream()
                .map(finding -> toResponse(finding,
                        HealthCalculator.summarize(treatmentsByFinding.getOrDefault(finding.getId(), List.of()))))
                .toList();
    }

    /**
     * Retrieves the findings matching the optional filters, as a flat list.
     *
     * <p>Every filter is independent and optional; a missing farm means every
     * farm, as for the other lists of the module. The farm, the block and both
     * dates, included, are those of the inspection. The status is computed from
     * the treatments, so its filter applies after that, in Java.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode raw block value as stored (for example {@code "C"}),
     *                  or {@code null} for every block
     * @param status    computed status, or {@code null} for every status
     * @param from      first inspection date, included, or {@code null}
     * @param to        last inspection date, included, or {@code null}
     * @return the matching findings, ordered by inspection date, inspection, then identifier
     */
    public List<HealthFindingResponse> getAllFindings(Integer farmId, String blockCode, HealthFindingStatus status,
                                                      LocalDate from, LocalDate to) {
        List<HealthFinding> findings = healthFindingRepository.findByOptionalFilters(
                farmId, normalizeOptionalText(blockCode), from, to);
        return toResponses(findings).stream()
                .filter(finding -> status == null || finding.status() == status)
                .toList();
    }

    private HealthFinding findFinding(Long findingId) {
        return healthFindingRepository.findById(findingId)
                .orElseThrow(() -> new HealthFindingNotFoundException(findingId));
    }

    private static ConflictException alreadyResolved(HealthFinding finding) {
        return new ConflictException("Finding " + finding.getId() + " was already resolved on "
                + finding.getResolvedOn());
    }

    /**
     * Resolves a finding: records the day it was seen to be settled, once.
     *
     * <p>The resolution cannot predate the inspection, nor the last treatment
     * of the finding; the same day is accepted. It is written by a conditional
     * update, so a second resolution is refused even when both are sent at the
     * same time.
     *
     * @param findingId the finding
     * @param request   the resolution, already validated
     * @return the resolved finding, with its status
     * @throws HealthFindingNotFoundException if no finding has this identifier
     * @throws ConflictException              if the finding is already resolved
     * @throws BusinessRuleException          if the resolution predates the
     *                                        inspection or the last treatment
     */
    @Transactional
    public HealthFindingResponse resolveFinding(Long findingId, FindingResolutionRequest request) {
        return resolveFinding(findingId, request, Instant.now());
    }

    /**
     * Same as {@link #resolveFinding(Long, FindingResolutionRequest)}, at an
     * explicit write time so the {@code lastUpdated} value can be tested.
     */
    HealthFindingResponse resolveFinding(Long findingId, FindingResolutionRequest request, Instant now) {
        HealthFinding finding = findFinding(findingId);
        if (finding.getResolvedOn() != null) {
            throw alreadyResolved(finding);
        }
        LocalDate inspectedOn = finding.getInspection().getInspectedOn();
        if (request.resolvedOn().isBefore(inspectedOn)) {
            throw new BusinessRuleException("The resolution date " + request.resolvedOn()
                    + " is before the inspection date " + inspectedOn + " of finding " + findingId);
        }
        TreatmentSummary treatments = HealthCalculator.summarize(
                healthTreatmentRepository.findByFindingIds(List.of(findingId)));
        if (treatments.lastTreatedOn() != null && request.resolvedOn().isBefore(treatments.lastTreatedOn())) {
            throw new BusinessRuleException("The resolution date " + request.resolvedOn()
                    + " is before the last treatment date " + treatments.lastTreatedOn() + " of finding " + findingId);
        }

        int written = healthFindingRepository.resolve(findingId, request.resolvedOn(),
                normalizeOptionalText(request.note()), now);
        // Read again: the update cleared the persistence context.
        HealthFinding current = findFinding(findingId);
        if (written == 0) {
            // Another request resolved the finding since it was read above.
            throw alreadyResolved(current);
        }
        return toResponse(current, treatments);
    }
}
