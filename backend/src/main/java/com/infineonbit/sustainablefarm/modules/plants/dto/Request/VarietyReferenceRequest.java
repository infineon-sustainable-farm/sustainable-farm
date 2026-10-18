package com.infineonbit.sustainablefarm.modules.plants.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request to enter or correct the agronomic reference of a variety: what one
 * tree yields in full production, and when the variety is harvested.
 *
 * <p>Validated like {@link PlantingRequest}: a 400 whose {@code fieldErrors}
 * lists each failing component. The name has the bounds of a planted variety
 * name, so every planted variety can have a reference; the service trims it.
 *
 * <p>The yield is above 0 and at most 1000 kg per tree, with at most 1
 * decimal. The source of the Zalka 2025 study (Wikifarmer) gives at most 2,500
 * fruits per tree, about 750 kg at the average fruit weight of 0.3 kg of its
 * "Cost per Mango" table: 1000 leaves a margin and still refuses a typing
 * slip such as 2200 for 220. Its three constraints can fail on the same value,
 * such as -0.15, so they share one message.
 *
 * <p>The harvest months run from 1 (January) to 12; an end month lower than
 * the start month is a season over the new year. Each source is optional and
 * trimmed by the service. Missing or blank, it is {@code user_entry} for a new
 * reference; on a correction, it stays as it was when its value does not
 * change, and becomes {@code user_entry} when it does.
 */
public record VarietyReferenceRequest(
        @Schema(description = "Variety name, as planted. Case, accents and surrounding spaces are ignored when "
                + "names are compared", example = "Keitt")
        @NotBlank(message = "varietyName is required")
        @Size(max = 255, message = "varietyName must be at most 255 characters")
        String varietyName,

        @Schema(description = "Yearly yield of one tree in full production, in kg", example = "220")
        @NotNull(message = "yieldPerTreeKg is required")
        @Positive(message = "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal")
        @DecimalMax(value = "1000",
                message = "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal")
        @Digits(integer = 4, fraction = 1,
                message = "yieldPerTreeKg must be greater than 0 and at most 1000, with at most 1 decimal")
        Double yieldPerTreeKg,

        @Schema(description = "Where the yield comes from", example = "Zalka_2025")
        @Size(max = 255, message = "yieldSource must be at most 255 characters")
        String yieldSource,

        @Schema(description = "First month of the harvest season, 1 (January) to 12", example = "5")
        @NotNull(message = "harvestStartMonth is required")
        @Min(value = 1, message = "harvestStartMonth must be between 1 and 12")
        @Max(value = 12, message = "harvestStartMonth must be between 1 and 12")
        Integer harvestStartMonth,

        @Schema(description = "Last month of the harvest season, 1 to 12; lower than the start month for a "
                + "season over the new year", example = "7")
        @NotNull(message = "harvestEndMonth is required")
        @Min(value = 1, message = "harvestEndMonth must be between 1 and 12")
        @Max(value = 12, message = "harvestEndMonth must be between 1 and 12")
        Integer harvestEndMonth,

        @Schema(description = "Where the harvest months come from", example = "varietal_guide_west_africa")
        @Size(max = 255, message = "seasonSource must be at most 255 characters")
        String seasonSource) {
}
