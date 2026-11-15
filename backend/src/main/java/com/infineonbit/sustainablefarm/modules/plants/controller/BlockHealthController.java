package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.BlockHealthResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthInspectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller giving the current health of each block.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/block-health} — the health of each inspected block</li>
 * </ul>
 * <p>
 * Read-only. The health of a block is the score of its most recent inspection,
 * computed on every read from the inspections recorded through
 * {@link HealthInspectionController}.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/block-health")
@AllArgsConstructor
@Tag(name = "Plants — Block health", description = "Current health of each block, from its latest inspection")
public class BlockHealthController {

   private final HealthInspectionService healthInspectionService;

   @GetMapping
   @Operation(summary = "Current health of each block",
         description = "One line per inspected block: the score, date and category of its most recent "
               + "inspection, the latest by date then by identifier. A block with no inspection is not listed.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Each inspected block, possibly an empty list")
   })
   public ResponseEntity<List<BlockHealthResponse>> getBlockHealth(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId) {
      List<BlockHealthResponse> blockHealthResponses = healthInspectionService.getBlockHealth(farmId);
      return ResponseEntity.status(HttpStatus.OK).body(blockHealthResponses);
   }
}
