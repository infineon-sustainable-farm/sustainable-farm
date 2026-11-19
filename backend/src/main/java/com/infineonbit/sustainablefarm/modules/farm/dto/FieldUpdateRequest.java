package com.infineonbit.sustainablefarm.modules.farm.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Changes of a field. {@code code}: {@code null} keeps the Plants block code, a blank value clears
 * it, any other value sets it.
 */
public record FieldUpdateRequest(
        Long farmId,
        String name,
        Double areaHectares,
        String cropType,
        String soilType,
        String coordinates,
        @Pattern(regexp = FieldCreateRequest.CODE_PATTERN, message = "code must be letters and digits only")
        String code) {
}
