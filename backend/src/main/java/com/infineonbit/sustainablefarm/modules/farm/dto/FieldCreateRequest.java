package com.infineonbit.sustainablefarm.modules.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * A new field (block). {@code code} is its optional Plants block code ("A", "B2"), the code Plants
 * and Product Transformation pick the block by.
 */
public record FieldCreateRequest(
        @NotNull Long farmId,
        @NotBlank String name,
        @NotNull Double areaHectares,
        String cropType,
        String soilType,
        String coordinates,
        @Pattern(regexp = FieldCreateRequest.CODE_PATTERN, message = "code must be letters and digits only")
        String code) {

    /** Letters and digits, or blank for no code; spaces around it are trimmed. */
    public static final String CODE_PATTERN = "^\\s*([A-Za-z0-9]{1,50})?\\s*$";
}
