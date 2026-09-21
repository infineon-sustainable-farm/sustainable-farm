package com.sustainablefarm.modules.producttransformation.integration.plants.controller;

import com.sustainablefarm.modules.producttransformation.integration.plants.dto.GrowthCalendarResponse;
import com.sustainablefarm.modules.producttransformation.integration.plants.dto.VarietyResponse;
import com.sustainablefarm.modules.producttransformation.integration.plants.service.PlantsIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Plants module integration
 * 
 * Exposes Plants API data through Product Transformation backend
 * Contract: API-001
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/integration/plants")
@Tag(name = "Plants Integration", description = "APIs for integrating with Plants module")
public class PlantsIntegrationController {
    
    private final PlantsIntegrationService plantsIntegrationService;
    
    @Autowired
    public PlantsIntegrationController(PlantsIntegrationService plantsIntegrationService) {
        this.plantsIntegrationService = plantsIntegrationService;
    }
    
    @GetMapping("/varieties")
    @Operation(summary = "Get all varieties from Plants module", 
               description = "Retrieves all mango varieties from the Plants API")
    public ResponseEntity<List<VarietyResponse>> getAllVarieties() {
        List<VarietyResponse> varieties = plantsIntegrationService.getAllVarieties();
        return ResponseEntity.ok(varieties);
    }
    
    @GetMapping("/varieties/farm/{farmId}")
    @Operation(summary = "Get varieties by farm", 
               description = "Retrieves varieties filtered by farm ID from Plants API")
    public ResponseEntity<List<VarietyResponse>> getVarietiesByFarm(
            @Parameter(description = "Farm ID") @PathVariable String farmId) {
        List<VarietyResponse> varieties = plantsIntegrationService.getVarietiesByFarm(farmId);
        return ResponseEntity.ok(varieties);
    }
    
    @GetMapping("/varieties/parcel/{parcelId}")
    @Operation(summary = "Get varieties by parcel", 
               description = "Retrieves varieties filtered by parcel ID from Plants API")
    public ResponseEntity<List<VarietyResponse>> getVarietiesByParcel(
            @Parameter(description = "Parcel ID (single character: A, B, C, ...)") 
            @PathVariable String parcelId) {
        List<VarietyResponse> varieties = plantsIntegrationService.getVarietiesByParcel(parcelId);
        return ResponseEntity.ok(varieties);
    }
    
    @GetMapping("/growth-calendar")
    @Operation(summary = "Get growth calendar from Plants module", 
               description = "Retrieves growth calendar data from Plants API with optional filters")
    public ResponseEntity<List<GrowthCalendarResponse>> getGrowthCalendar(
            @Parameter(description = "Filter by parcel/block identifier") 
            @RequestParam(required = false) String blocParcelle,
            @Parameter(description = "Filter by farm identifier") 
            @RequestParam(required = false) String idFerme) {
        List<GrowthCalendarResponse> calendar = plantsIntegrationService.getGrowthCalendar(blocParcelle, idFerme);
        return ResponseEntity.ok(calendar);
    }
    
    @GetMapping("/growth-calendar/farm/{farmId}/parcel/{parcelId}")
    @Operation(summary = "Get growth calendar by farm and parcel", 
               description = "Retrieves growth calendar data for a specific farm and parcel")
    public ResponseEntity<List<GrowthCalendarResponse>> getGrowthCalendarByFarmAndParcel(
            @Parameter(description = "Farm ID") @PathVariable String farmId,
            @Parameter(description = "Parcel ID (single character: A, B, C, ...)") 
            @PathVariable String parcelId) {
        List<GrowthCalendarResponse> calendar = plantsIntegrationService.getGrowthCalendarByFarmAndParcel(farmId, parcelId);
        return ResponseEntity.ok(calendar);
    }
}
