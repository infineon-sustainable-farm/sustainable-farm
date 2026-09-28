package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthTreatmentRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthCalculator.TreatmentSummary;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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
     * Turns a blank filter into no filter at all, as for the other lists of the
     * module.
     */
    private static String normalizeFilter(String value) {
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
                farmId, normalizeFilter(blockCode), from, to);
        return toResponses(findings).stream()
                .filter(finding -> status == null || finding.status() == status)
                .toList();
    }
}
