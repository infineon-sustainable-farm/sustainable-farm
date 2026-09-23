package com.sustainablefarm.modules.producttransformation.integration.plants.impl;

import com.sustainablefarm.modules.producttransformation.integration.plants.dto.GrowthCalendarResponse;
import com.sustainablefarm.modules.producttransformation.integration.plants.dto.VarietyResponse;
import com.sustainablefarm.modules.producttransformation.integration.plants.service.PlantsIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of Plants module integration service
 * 
 * Contract: API-001
 * Consumes Plants API endpoints for variety and growth calendar data
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
public class PlantsIntegrationServiceImpl implements PlantsIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlantsIntegrationServiceImpl.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${integration.plants.base-url:http://localhost:8081}")
    private String plantsBaseUrl;
    
    @Value("${integration.plants.enabled:true}")
    private boolean integrationEnabled;
    
    public PlantsIntegrationServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Override
    public List<VarietyResponse> getAllVarieties() {
        if (!integrationEnabled) {
            logger.warn("Plants integration is disabled. Returning empty list.");
            return Collections.emptyList();
        }
        
        try {
            String url = plantsBaseUrl + "/api/plants/varieties";
            logger.info("Fetching varieties from Plants API: {}", url);
            
            VarietyResponse[] response = restTemplate.getForObject(url, VarietyResponse[].class);
            
            if (response == null) {
                logger.warn("Received null response from Plants API");
                return Collections.emptyList();
            }
            
            List<VarietyResponse> varieties = Arrays.asList(response);
            logger.info("Successfully retrieved {} varieties from Plants API", varieties.size());
            
            return varieties;
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching varieties from Plants API: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (ResourceAccessException e) {
            logger.error("Connection error fetching varieties from Plants API: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Unexpected error fetching varieties from Plants API", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<GrowthCalendarResponse> getGrowthCalendar(String blocParcelle, String idFerme) {
        if (!integrationEnabled) {
            logger.warn("Plants integration is disabled. Returning empty list.");
            return Collections.emptyList();
        }
        
        try {
            StringBuilder urlBuilder = new StringBuilder(plantsBaseUrl + "/api/plants/growth-calendar");
            
            // Add query parameters if provided
            if (blocParcelle != null && !blocParcelle.trim().isEmpty()) {
                urlBuilder.append("?bloc_parcelle=").append(blocParcelle.trim());
            }
            if (idFerme != null && !idFerme.trim().isEmpty()) {
                if (urlBuilder.toString().contains("?")) {
                    urlBuilder.append("&id_ferme=").append(idFerme);
                } else {
                    urlBuilder.append("?id_ferme=").append(idFerme);
                }
            }
            
            String url = urlBuilder.toString();
            logger.info("Fetching growth calendar from Plants API: {}", url);
            
            GrowthCalendarResponse[] response = restTemplate.getForObject(url, GrowthCalendarResponse[].class);
            
            if (response == null) {
                logger.warn("Received null response from Plants API");
                return Collections.emptyList();
            }
            
            List<GrowthCalendarResponse> calendar = Arrays.asList(response);
            logger.info("Successfully retrieved {} growth calendar entries from Plants API", calendar.size());
            
            return calendar;
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching growth calendar from Plants API: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (ResourceAccessException e) {
            logger.error("Connection error fetching growth calendar from Plants API: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Unexpected error fetching growth calendar from Plants API", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<VarietyResponse> getVarietiesByFarm(String farmId) {
        List<VarietyResponse> allVarieties = getAllVarieties();
        
        if (allVarieties.isEmpty()) {
            return Collections.emptyList();
        }
        
        return allVarieties.stream()
            .filter(variety -> farmId != null && farmId.equals(variety.getFarmId()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<VarietyResponse> getVarietiesByParcel(String parcelId) {
        if (parcelId == null || parcelId.trim().isEmpty()) {
            logger.warn("Parcel ID is null or empty. Returning empty list.");
            return Collections.emptyList();
        }
        
        // Normalize parcel ID to match Plants convention (single character)
        String normalizedParcelId = parcelId.trim();
        
        List<VarietyResponse> allVarieties = getAllVarieties();
        
        if (allVarieties.isEmpty()) {
            return Collections.emptyList();
        }
        
        return allVarieties.stream()
            .filter(variety -> normalizedParcelId.equals(variety.getParcelId()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<GrowthCalendarResponse> getGrowthCalendarByFarmAndParcel(String farmId, String parcelId) {
        if (farmId == null || farmId.trim().isEmpty() || parcelId == null || parcelId.trim().isEmpty()) {
            logger.warn("Farm ID or Parcel ID is null or empty. Returning empty list.");
            return Collections.emptyList();
        }
        
        // Normalize parcel ID
        String normalizedParcelId = parcelId.trim();
        
        return getGrowthCalendar(normalizedParcelId, farmId);
    }
}
