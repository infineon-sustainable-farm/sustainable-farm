package com.infineonbit.sustainablefarm.modules.producttransformation.integration.cropstorage.controller;

import com.infineonbit.sustainablefarm.modules.producttransformation.integration.cropstorage.dto.CropStorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.cropstorage.dto.CropStorageEntryResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.cropstorage.service.CropStorageIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Crop Storage Integration Controller
 * Exposes Crop Storage integration through Product Transformation module
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/crop-storage")
@Tag(name = "Crop Storage Integration", description = "APIs for integrating with Crop Storage module")
public class CropStorageIntegrationController {

    private final CropStorageIntegrationService cropStorageIntegrationService;

    @Autowired
    public CropStorageIntegrationController(CropStorageIntegrationService cropStorageIntegrationService) {
        this.cropStorageIntegrationService = cropStorageIntegrationService;
    }

    @PostMapping("/entries")
    @Operation(summary = "Create storage entry in Crop Storage", description = "Creates a storage entry for a completed batch")
    public ResponseEntity<CropStorageEntryResponse> createStorageEntry(
            @Valid @RequestBody CropStorageEntryRequest request) {
        try {
            CropStorageEntryResponse response = cropStorageIntegrationService.createStorageEntry(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // Handle capacity overflow (409) and other errors
            // For now, re-throw to let global exception handler handle it
            throw e;
        }
    }

    @PostMapping("/entries/check-capacity")
    @Operation(summary = "Check storage capacity", description = "Checks if storage capacity is available for the specified entry")
    public ResponseEntity<Boolean> checkStorageCapacity(
            @Valid @RequestBody CropStorageEntryRequest request) {
        boolean hasCapacity = cropStorageIntegrationService.checkStorageCapacity(request);
        return ResponseEntity.ok(hasCapacity);
    }
}
