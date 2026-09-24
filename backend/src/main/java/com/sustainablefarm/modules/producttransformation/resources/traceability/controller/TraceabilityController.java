package com.sustainablefarm.modules.producttransformation.resources.traceability.controller;

import com.sustainablefarm.modules.producttransformation.resources.traceability.dto.response.TraceabilityResponse;
import com.sustainablefarm.modules.producttransformation.resources.traceability.service.TraceabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Traceability operations
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/traceability")
@Tag(name = "Traceability", description = "APIs for batch traceability from harvest to sales")
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    @Autowired
    public TraceabilityController(TraceabilityService traceabilityService) {
        this.traceabilityService = traceabilityService;
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get batch traceability", description = "Retrieves complete traceability chain for a batch from harvest to packaging")
    public ResponseEntity<TraceabilityResponse> getBatchTraceability(
            @Parameter(description = "Batch ID") @PathVariable String batchId) {
        TraceabilityResponse response = traceabilityService.getBatchTraceability(batchId);
        return ResponseEntity.ok(response);
    }
}