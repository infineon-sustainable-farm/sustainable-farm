package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Plants Integration Service
 * HTTP-based integration with Plants module for harvest data
 *
 * Contract from Plants team:
 * Endpoint: GET /api/plants/harvests
 * Fields: id, farmId, blockCode, varietyId, varietyName, harvestDate, quantityKg, source, lastUpdated
 * Convention: camelCase
 * Error handling: 422 for validation errors
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
// Explicit bean name: the watersupply module ships its own
// PlantsIntegrationService, and two @Service classes with the same simple
// name make the Spring context fail (ConflictingBeanDefinitionException).
@Service("productTransformationPlantsIntegrationService")
public class PlantsIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${plants.api.base-url:http://localhost:8080}")
    private String plantsApiBaseUrl;

    @Autowired
    public PlantsIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch all harvests from Plants module
     *
     * @return List of harvests from Plants
     */
    public List<PlantsHarvestResponse> getAllHarvests() {
        String url = plantsApiBaseUrl + "/api/plants/harvests";
        ResponseEntity<PlantsHarvestResponse[]> response = restTemplate.getForEntity(url, PlantsHarvestResponse[].class);
        return Arrays.asList(response.getBody());
    }

    /**
     * Fetch harvest by ID from Plants module
     *
     * @param harvestId Harvest ID from Plants
     * @return Harvest data from Plants
     */
    public PlantsHarvestResponse getHarvestById(Long harvestId) {
        String url = plantsApiBaseUrl + "/api/plants/harvests/" + harvestId;
        ResponseEntity<PlantsHarvestResponse> response = restTemplate.getForEntity(url, PlantsHarvestResponse.class);
        return response.getBody();
    }

    /**
     * Fetch harvests by farm ID from Plants module
     *
     * @param farmId Farm ID
     * @return List of harvests from Plants for the specified farm
     */
    public List<PlantsHarvestResponse> getHarvestsByFarm(Long farmId) {
        String url = plantsApiBaseUrl + "/api/plants/harvests?farmId=" + farmId;
        ResponseEntity<PlantsHarvestResponse[]> response = restTemplate.getForEntity(url, PlantsHarvestResponse[].class);
        return Arrays.asList(response.getBody());
    }

    /**
     * Fetch harvests by block code from Plants module
     *
     * @param blockCode Block code (e.g., "A", "B", "C")
     * @return List of harvests from Plants for the specified block
     */
    public List<PlantsHarvestResponse> getHarvestsByBlock(String blockCode) {
        String url = plantsApiBaseUrl + "/api/plants/harvests?blockCode=" + blockCode;
        ResponseEntity<PlantsHarvestResponse[]> response = restTemplate.getForEntity(url, PlantsHarvestResponse[].class);
        return Arrays.asList(response.getBody());
    }

    /**
     * Map a Plants harvest to the Product Transformation HarvestEvent.
     *
     * <p>The batch/lot identifier is supplied by the caller and is stored
     * as-is: this method never regenerates it, so the original lot number of
     * the harvest is preserved across imports. The Plants record id is kept in
     * {@code plantsHarvestId} to keep the cross-module link.
     *
     * <p>The variety name comes back from Plants as the planted name
     * ("Keitt", "keitt", "Amélie"...), so it is resolved through
     * {@link MangoVariety#fromPlants(String)}.
     *
     * <p>The quality grade is not part of the Plants harvest contract: the
     * caller remains responsible for it and must set it before persisting.
     * For the same reason {@code harvestId} (the harvest event primary key)
     * is left untouched: it is a Product Transformation identifier, distinct
     * from the batch id (see {@code DtoMapper}), and remains the caller's
     * responsibility.
     *
     * @param harvest          harvest returned by the Plants module
     * @param originalBatchId  original lot/batch identifier to preserve
     * @return a HarvestEvent carrying the Plants identity and values
     */
    public HarvestEvent toHarvestEvent(PlantsHarvestResponse harvest, String originalBatchId) {
        if (harvest == null) {
            throw new IllegalArgumentException("Plants harvest must not be null");
        }
        if (originalBatchId == null || originalBatchId.isBlank()) {
            throw new IllegalArgumentException("originalBatchId (Plants lot/batch) is required");
        }
        if (harvest.getHarvestDate() == null || harvest.getHarvestDate().isBlank()) {
            throw new IllegalArgumentException("Plants harvest date is required");
        }

        HarvestEvent event = new HarvestEvent();
        event.setPlantsHarvestId(harvest.getId());
        event.setBatchId(originalBatchId.trim());
        event.setHarvestDate(LocalDate.parse(harvest.getHarvestDate()));
        event.setMangoVariety(MangoVariety.fromPlants(harvest.getVarietyName()));
        event.setFarmId(harvest.getFarmId() != null ? String.valueOf(harvest.getFarmId()) : null);
        event.setBlockId(harvest.getBlockCode());
        event.setHarvestQuantityKg(harvest.getQuantityKg() != null
                ? BigDecimal.valueOf(harvest.getQuantityKg())
                : null);
        return event;
    }
}
