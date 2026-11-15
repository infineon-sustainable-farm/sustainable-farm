package com.infineonbit.sustainablefarm.modules.producttransformation.integration.cropstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Crop Storage Entry Request
 * Maps to Crop Storage module API-005 contract
 *
 * Contract from Crop Storage team:
 * Convention: snake_case
 * Fields: batch_id, packaging_id, dried_quantity_kg, waste_quantity_kg (optional),
 *         product_stage (DRIED/PACKAGED), storage_location_id (ZONE-A1 style)
 * Error handling: 409 on capacity overflow
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropStorageEntryRequest {

    @JsonProperty("batch_id")
    private String batchId;

    @JsonProperty("packaging_id")
    private String packagingId;

    @JsonProperty("dried_quantity_kg")
    private BigDecimal driedQuantityKg;

    @JsonProperty("waste_quantity_kg")
    private BigDecimal wasteQuantityKg;

    @JsonProperty("product_stage")
    private String productStage; // DRIED or PACKAGED

    @JsonProperty("storage_location_id")
    private String storageLocationId; // ZONE-A1 style
}
