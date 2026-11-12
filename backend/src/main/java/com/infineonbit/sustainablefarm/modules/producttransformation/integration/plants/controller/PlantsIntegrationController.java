package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.controller;

import com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service.DtoMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto.response.HarvestEventResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.QualityGrade;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service.PlantsHarvestImportService;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service.PlantsIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Plants Integration Controller
 * Exposes Plants harvest data through Product Transformation module
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.1.0
 */
@RestController
@RequestMapping("/api/harvest-events/plants")
@Tag(name = "Plants Integration", description = "APIs for integrating with Plants module harvest data")
public class PlantsIntegrationController {

    private final PlantsIntegrationService plantsIntegrationService;
    private final PlantsHarvestImportService plantsHarvestImportService;
    private final DtoMapper dtoMapper;

    @Autowired
    public PlantsIntegrationController(PlantsIntegrationService plantsIntegrationService,
                                       PlantsHarvestImportService plantsHarvestImportService,
                                       DtoMapper dtoMapper) {
        this.plantsIntegrationService = plantsIntegrationService;
        this.plantsHarvestImportService = plantsHarvestImportService;
        this.dtoMapper = dtoMapper;
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

    @PostMapping("/import/{harvestId}")
    @Operation(summary = "Import a Plants harvest as a harvest event",
            description = "Creates the Product Transformation harvest event for a Plants harvest, keeping its lot "
                    + "number and Plants harvest id. Re-importing the same harvest returns the existing event "
                    + "(200) instead of creating a duplicate (201 only on first import).")
    public ResponseEntity<HarvestEventResponse> importHarvest(
            @Parameter(description = "Harvest ID from Plants") @PathVariable Long harvestId,
            @Parameter(description = "Quality grade assigned at reception; Plants does not record one")
            @RequestParam QualityGrade qualityGrade) {

        PlantsHarvestImportService.ImportOutcome outcome =
                plantsHarvestImportService.importHarvest(harvestId, qualityGrade);

        HarvestEventResponse response = dtoMapper.toResponse(outcome.event());
        return outcome.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(response)
                : ResponseEntity.ok(response);
    }
}
