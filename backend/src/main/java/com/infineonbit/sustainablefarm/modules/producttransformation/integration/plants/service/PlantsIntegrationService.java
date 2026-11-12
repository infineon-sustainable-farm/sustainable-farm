package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
        try {
            ResponseEntity<PlantsHarvestResponse> response =
                    restTemplate.getForEntity(url, PlantsHarvestResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            // Plants answers 404 for an unknown harvest. Left as-is, the client
            // exception would bubble up as a 500 and look like an outage here.
            throw new ResourceNotFoundException("Plants harvest not found: " + harvestId);
        }
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
     * <p><strong>Identity convention (agreed with the Plants team, option B):</strong>
     * a harvest lot and a transformation lot are not the same thing — one harvest
     * can be split into several drying lots, and several harvests can be merged
     * into one transformation lot. Product Transformation therefore keeps its own
     * {@code batchId} (supplied by the caller), and stores the Plants identity in
     * two dedicated fields: {@code lotNumber} and {@code plantsHarvestId}. Both are
     * returned by the API, so a transformation lot can always be traced back to
     * the farm harvest and block it came from.
     *
     * <p>The variety name comes back from Plants as the planted name
     * ("Keitt", "keitt", "Amélie"...), so it is resolved through
     * {@link MangoVariety#fromPlants(String)} — the lenient resolver: an unmapped
     * variety must not refuse a whole harvest.
     *
     * <p>The quality grade is not part of the Plants harvest contract: the
     * caller remains responsible for it and must set it before persisting.
     * For the same reason {@code harvestId} (the harvest event primary key)
     * is left untouched: it is a Product Transformation identifier, distinct
     * from the batch id (see {@code DtoMapper}), and remains the caller's
     * responsibility.
     *
     * @param harvest  harvest returned by the Plants module
     * @param batchId  Product Transformation batch identifier (not the Plants lot)
     * @return a HarvestEvent carrying the Plants identity and values
     */
    public HarvestEvent toHarvestEvent(PlantsHarvestResponse harvest, String batchId) {
        if (harvest == null) {
            throw new IllegalArgumentException("Plants harvest must not be null");
        }
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException("batchId (Product Transformation lot) is required");
        }
        if (harvest.getHarvestDate() == null || harvest.getHarvestDate().isBlank()) {
            throw new IllegalArgumentException("Plants harvest date is required");
        }

        HarvestEvent event = new HarvestEvent();
        event.setPlantsHarvestId(harvest.getId());
        event.setLotNumber(harvest.getLotNumber());
        event.setBatchId(batchId.trim());
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
