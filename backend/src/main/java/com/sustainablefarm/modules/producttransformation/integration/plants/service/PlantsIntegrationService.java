package com.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.sustainablefarm.modules.producttransformation.integration.plants.dto.GrowthCalendarResponse;
import com.sustainablefarm.modules.producttransformation.integration.plants.dto.VarietyResponse;

import java.util.List;

/**
 * Service interface for Plants module integration
 * 
 * Contract: API-001
 * Implements integration with Plants API for variety and growth calendar data
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
public interface PlantsIntegrationService {
    
    /**
     * Retrieves all mango varieties from Plants module
     * 
     * Contract: API-001
     * Endpoint: GET /api/plants/varieties
     * 
     * @return List of variety responses
     */
    List<VarietyResponse> getAllVarieties();
    
    /**
     * Retrieves growth calendar data from Plants module
     * 
     * Contract: API-001
     * Endpoint: GET /api/plants/growth-calendar
     * 
     * @param blocParcelle Optional filter by parcel/block identifier
     * @param idFerme Optional filter by farm identifier
     * @return List of growth calendar responses
     */
    List<GrowthCalendarResponse> getGrowthCalendar(String blocParcelle, String idFerme);
    
    /**
     * Retrieves varieties filtered by farm
     * 
     * @param farmId Farm identifier
     * @return List of varieties for the specified farm
     */
    List<VarietyResponse> getVarietiesByFarm(String farmId);
    
    /**
     * Retrieves varieties filtered by parcel
     * 
     * @param parcelId Parcel identifier (single character: A, B, C, ...)
     * @return List of varieties for the specified parcel
     */
    List<VarietyResponse> getVarietiesByParcel(String parcelId);
    
    /**
     * Retrieves growth calendar for a specific farm and parcel
     * 
     * @param farmId Farm identifier
     * @param parcelId Parcel identifier
     * @return Growth calendar data for the specified farm and parcel
     */
    List<GrowthCalendarResponse> getGrowthCalendarByFarmAndParcel(String farmId, String parcelId);
}
