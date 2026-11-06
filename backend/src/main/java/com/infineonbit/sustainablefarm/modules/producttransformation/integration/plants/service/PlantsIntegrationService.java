package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

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
@Service
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
}
