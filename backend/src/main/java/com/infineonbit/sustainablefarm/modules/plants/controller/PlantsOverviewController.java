package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantsOverviewService;

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

/**
 * REST controller exposing the figures of the Plants dashboard.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/overview} — total plants, plants in growth stage, plants ready for harvest, active
 * alerts and the trees by maturity status, today</li>
 * </ul>
 * <p>
 * Read-only. Each figure sums up a list of the module with its rules, on every request; nothing of them is stored.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/overview")
@AllArgsConstructor
@Tag(name = "Plants — Overview", description = "The figures of the Plants dashboard, computed on every request")
public class PlantsOverviewController {

   private final PlantsOverviewService plantsOverviewService;

   @GetMapping
   @Operation(summary = "Get the figures of the Plants dashboard of the day",
         description = "totalPlants: trees of the planted varieties, without the nursery. inGrowthStage: trees in "
               + "establishment (0 to 2 completed years) today. readyForHarvest: trees whose maturity status in "
               + "GET /api/plants/harvest-status is READY_FOR_HARVEST. activeAlerts: the alerts of GET "
               + "/api/plants/alerts with its default look-ahead, criticalAlerts the CRITICAL ones among them. "
               + "maturity: the trees of each maturityStatus of GET /api/plants/harvest-status, in the order "
               + "IMMATURE, RIPE, READY_FOR_HARVEST, then the trees without agronomic reference (maturityStatus "
               + "null). Every entry is sent, with 0 trees if no variety has its status; sharePct is treeCount × "
               + "100 / totalPlants, rounded half up to one decimal, and null when totalPlants is 0.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The figures, zeros when nothing is planted"),
         @ApiResponse(responseCode = "400", description = "A non-numeric farm identifier")
   })
   public ResponseEntity<PlantsOverviewResponse> getOverview(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"A\"") @RequestParam(name = "blockCode", required = false) String blockCode) {
      PlantsOverviewResponse plantsOverviewResponse = plantsOverviewService.getOverview(farmId, blockCode);
      return ResponseEntity.status(HttpStatus.OK).body(plantsOverviewResponse);
   }
}
