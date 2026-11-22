package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * API-005 (draft of 2026-09-29, still to be aligned with Product Transformation):
 * storage entry notified once packaging is done. Field names stay snake_case on
 * the wire, per the contract.
 *
 * <p>What Crop Storage actually stores: batch_id (as the batch code),
 * dried_quantity_kg, packaging_date (UTC date), product_grade, harvest_id and,
 * in the IN movement reason, packaging_id. The other fields (waste_quantity_kg,
 * packaging_operator_id, packaging_location, farm_id, block_id, variety_id) are
 * validated but NOT persisted: Crop Storage has no approved column for them yet
 * and they stay owned by Product Transformation / Plants. Persisting them needs
 * a schema change approved by the team (see the module migration proposal).
 *
 * <p>Quantities are kg with at most 2 decimals (the stored precision), so a value
 * is never rounded silently. A missing optional quantity stays null, which is
 * distinct from 0.
 */
public record StorageEntryRequest(

        @NotBlank
        @JsonProperty("batch_id")
        String batchId,

        @NotBlank
        @JsonProperty("packaging_id")
        String packagingId,

        @NotNull
        @Positive
        @Digits(integer = 8, fraction = 2)
        @JsonProperty("dried_quantity_kg")
        BigDecimal driedQuantityKg,

        @PositiveOrZero
        @Digits(integer = 8, fraction = 2)
        @JsonProperty("waste_quantity_kg")
        BigDecimal wasteQuantityKg,

        // OffsetDateTime: a timestamp without an explicit offset is rejected (400).
        @NotNull
        @JsonProperty("packaging_date")
        OffsetDateTime packagingDate,

        @NotBlank
        @JsonProperty("packaging_operator_id")
        String packagingOperatorId,

        @NotBlank
        @JsonProperty("packaging_location")
        String packagingLocation,

        // Crop Storage zone id (numeric, as text). A PT zone code such as ZONE-A1
        // is not converted: the id-vs-code question is an open API-005 point.
        @JsonProperty("target_storage_location_id")
        String targetStorageLocationId,

        @NotBlank
        @JsonProperty("product_grade")
        String productGrade,

        // Stored as the numeric Plants harvest id; anything else is refused instead
        // of being silently dropped.
        @Pattern(regexp = "\\d{1,18}", message = "must be the numeric Plants harvest id")
        @JsonProperty("harvest_id")
        String harvestId,

        @NotBlank
        @JsonProperty("farm_id")
        String farmId,

        @NotBlank
        @JsonProperty("block_id")
        String blockId,

        @NotBlank
        @JsonProperty("variety_id")
        String varietyId) {
}
