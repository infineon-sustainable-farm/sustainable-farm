package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;

import java.time.LocalDate;

/**
 * API representation of a problem seen during an inspection, with its status.
 *
 * <p>The farm, the block and the date are those of the inspection.
 * {@code status}, {@code treatmentCount}, {@code lastTreatedOn} and
 * {@code harvestAllowedFrom} are computed on every read from the treatments and
 * the resolution of the finding, never stored. {@code harvestAllowedFrom} is
 * the latest of the dates its treatments allow, {@code null} without a
 * treatment. {@code resolvedOn} and {@code resolutionNote} are {@code null}
 * until the finding is resolved.
 */
public record HealthFindingResponse(
        Long id,
        Long inspectionId,
        Integer farmId,
        String blockCode,
        LocalDate inspectedOn,
        String issueCode,
        String issueName,
        HealthIssueKind issueKind,
        String otherLabel,
        String treeLabel,
        HealthFindingStatus status,
        int treatmentCount,
        LocalDate lastTreatedOn,
        LocalDate harvestAllowedFrom,
        LocalDate resolvedOn,
        String resolutionNote) {
}
