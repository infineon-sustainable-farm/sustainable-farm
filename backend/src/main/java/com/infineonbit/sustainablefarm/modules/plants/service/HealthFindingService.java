package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class HealthFindingService {

    /**
     * Maps a stored finding to its API representation. No treatment can be
     * recorded yet, so the status depends on the resolution alone.
     *
     * @param finding the stored finding, with its inspection and issue loaded
     * @return the API representation of that finding
     */
    private static HealthFindingResponse toResponse(HealthFinding finding) {
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
                HealthCalculator.status(0, finding.getResolvedOn()),
                finding.getResolvedOn(),
                finding.getResolutionNote());
    }

    /**
     * Maps stored findings to their API representation, in the given order.
     *
     * @param findings the stored findings, with their inspection and issue loaded
     * @return their API representations
     */
    public List<HealthFindingResponse> toResponses(List<HealthFinding> findings) {
        return findings.stream()
                .map(HealthFindingService::toResponse)
                .toList();
    }
}
