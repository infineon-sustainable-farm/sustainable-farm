package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import com.infineonbit.sustainablefarm.modules.plants.validation.OtherLabelMatchesIssue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * A problem seen during an inspection, sent inside {@link HealthInspectionRequest}.
 *
 * <p>{@code issueCode} is a code of the catalogue, in any case and with or
 * without surrounding spaces; the service trims and upper-cases it. A code with
 * the right form but missing from the catalogue is refused by the service with
 * a 422. {@code otherLabel} names the problem when the code is {@code OTHER},
 * and only then; {@link OtherLabelMatchesIssue} checks it, length included.
 *
 * <p>{@code treeLabel} is optional; the service trims and upper-cases it, and a
 * blank value counts as no tree.
 */
@OtherLabelMatchesIssue(codeField = "issueCode", labelField = "otherLabel")
public record HealthFindingRequest(
        @Schema(description = "Code of the catalogue, for example ANTHRACNOSE. Case and surrounding spaces "
                + "are ignored")
        @NotNull(message = "issueCode is required")
        @Pattern(regexp = "^\\s*[A-Za-z0-9_]{1,50}\\s*$",
                message = "issueCode must be a code of letters, digits and underscores, such as ANTHRACNOSE")
        String issueCode,

        @Schema(description = "Name of the problem, required when issueCode is OTHER and refused otherwise, "
                + "at most 60 characters")
        String otherLabel,

        @Pattern(regexp = "^\\s*([A-Za-z0-9-]{1,20})?\\s*$",
                message = "treeLabel must be up to 20 letters, digits or hyphens, such as 42 or R3-12")
        String treeLabel) {
}
