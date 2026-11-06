package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Water availability forecast of a source for planned Product Transformation batches.
 *
 * @param horizonDays forecast window, default 7 days; batches planned outside it are ignored
 * @param includeIrrigation adds the scheduled irrigations of the source's farm (default true)
 */
public record AvailabilityForecastRequest(
        @NotNull UUID sourceId,
        @Min(1) @Max(90) Integer horizonDays,
        Boolean includeIrrigation,
        List<@Valid Batch> batches) {

    /** A planned batch; the water demand is derived from its activity and size (coefficients to validate). */
    public record Batch(String lotId, @NotBlank String activity, Instant plannedAt, @PositiveOrZero Double sizeKg) {
    }
}
