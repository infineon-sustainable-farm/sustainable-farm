package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for Plants Harvest API Response
 * Maps to Plants module harvest data contract
 *
 * Contract from Plants team:
 * Endpoint: GET /api/plants/harvests
 * Fields: id, farmId, blockCode, varietyId, varietyName, harvestDate, quantityKg, source, lastUpdated
 * Convention: camelCase
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantsHarvestResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("farmId")
    private Long farmId;

    @JsonProperty("blockCode")
    private String blockCode;

    @JsonProperty("varietyId")
    private Long varietyId;

    @JsonProperty("varietyName")
    private String varietyName;

    @JsonProperty("harvestDate")
    private String harvestDate;

    @JsonProperty("quantityKg")
    private Double quantityKg;

    @JsonProperty("source")
    private String source;

    @JsonProperty("lastUpdated")
    private Instant lastUpdated;
}
