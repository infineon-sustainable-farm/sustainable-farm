package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import com.infineonbit.sustainablefarm.modules.plants.validation.NewIssueName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to add a pest or a disease to the catalogue.
 *
 * <p>Validated like {@link FertilizerRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The code is not sent: the service derives it
 * from the name, and {@link NewIssueName} checks the name against that rule,
 * one message at a time. {@code kind} is a string checked by a pattern rather
 * than an enum, for the reason given in {@link FertilizerRequest}; it cannot be
 * {@code OTHER}, since the service adds "Other" itself.
 *
 * <p>The other components are optional, and trimmed by the service; a blank
 * value counts as none. The EPPO code is upper-cased, and a missing source is
 * recorded as {@code user_entry}.
 */
public record HealthIssueRequest(
        @Schema(description = "Name of the pest or disease, at most 50 characters. The code is derived from it: "
                + "\"Powdery mildew\" gives POWDERY_MILDEW", example = "Powdery mildew")
        @NotNull(message = "name is required")
        @NewIssueName
        String name,

        @Schema(allowableValues = {"PEST", "DISEASE"}, description = "Case and surrounding spaces are ignored")
        @NotNull(message = "kind is required")
        @Pattern(regexp = "^\\s*(PEST|DISEASE)\\s*$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "kind must be PEST or DISEASE")
        String kind,

        @Size(max = 255, message = "scientificName must be at most 255 characters")
        String scientificName,

        @Schema(description = "EPPO code of the organism, 1 to 10 letters or digits. Case is ignored")
        @Pattern(regexp = "^\\s*([A-Za-z0-9]{1,10})?\\s*$", message = "eppoCode must be 1 to 10 letters or digits")
        String eppoCode,

        @Schema(description = "Where the entry comes from; user_entry when omitted")
        @Size(max = 255, message = "source must be at most 255 characters")
        String source) {
}
