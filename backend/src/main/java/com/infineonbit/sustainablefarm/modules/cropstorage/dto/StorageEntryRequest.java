package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * API-005: storage entry notified by Product Transformation once packaging is
 * done. Field names stay snake_case on the wire, per the contract. The Plants
 * traceability ids (farm_id, block_id, variety_id) and the PT operator and
 * location are accepted and validated but not persisted: Crop Storage traces
 * through harvestId, the rest stays owned by Product Transformation (open
 * point of the API-005 feedback).
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
        @JsonProperty("dried_quantity_kg")
        BigDecimal driedQuantityKg,

        @JsonProperty("waste_quantity_kg")
        BigDecimal wasteQuantityKg,

        @NotNull
        @JsonProperty("packaging_date")
        OffsetDateTime packagingDate,

        @NotBlank
        @JsonProperty("packaging_operator_id")
        String packagingOperatorId,

        @NotBlank
        @JsonProperty("packaging_location")
        String packagingLocation,

        @JsonProperty("target_storage_location_id")
        String targetStorageLocationId,

        @NotBlank
        @JsonProperty("product_grade")
        String productGrade,

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