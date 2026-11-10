package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.controller;

import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service.PlantsIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Plants Integration Controller
 * Exposes Plants harvest data through Product Transformation module
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/harvest-events/plants")
@Tag(name = "Plants Integration", description = "APIs for integrating with Plants module harvest data")
public class PlantsIntegrationController {

    private final PlantsIntegrationService plantsIntegrationService;

    @Autowired
    public PlantsIntegrationController(PlantsIntegrationService plantsIntegrationService) {
        this.plantsIntegrationService = plantsIntegrationService;
    }

    @GetMapping("/all")
    @Operation(summary = "Get all harvests from Plants", description = "Retrieves all harvest data from Plants module")
    public ResponseEntity<List<PlantsHarvestResponse>> getAllHarvests() {
        List<PlantsHarvestResponse> harvests = plantsIntegrationService.getAllHarvests();
        return ResponseEntity.ok(harvests);
    }

    @GetMapping("/{harvestId}")
    @Operation(summary = "Get harvest by ID from Plants", description = "Retrieves specific harvest data from Plants module")
    public ResponseEntity<PlantsHarvestResponse> getHarvestById(
            @Parameter(description = "Harvest ID from Plants") @PathVariable Long harvestId) {
        PlantsHarvestResponse harvest = plantsIntegrationService.getHarvestById(harvestId);
        return ResponseEntity.ok(harvest);
    }

    @GetMapping("/farm/{farmId}")
    @Operation(summary = "Get harvests by farm from Plants", description = "Retrieves harvests from Plants for a specific farm")
    public ResponseEntity<List<PlantsHarvestResponse>> getHarvestsByFarm(
            @Parameter(description = "Farm ID") @PathVariable Long farmId) {
        List<PlantsHarvestResponse> harvests = plantsIntegrationService.getHarvestsByFarm(farmId);
        return ResponseEntity.ok(harvests);
    }

    @GetMapping("/block/{blockCode}")
    @Operation(summary = "Get harvests by block from Plants", description = "Retrieves harvests from Plants for a specific block")
    public ResponseEntity<List<PlantsHarvestResponse>> getHarvestsByBlock(
            @Parameter(description = "Block code (e.g., A, B, C)") @PathVariable String blockCode) {
        List<PlantsHarvestResponse> harvests = plantsIntegrationService.getHarvestsByBlock(blockCode);
        return ResponseEntity.ok(harvests);
    }
}
