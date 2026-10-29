package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantAlertWithinDays;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.HarvestStatusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the maturity status and the estimated harvest date
 * of the planted varieties.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/harvest-status} — the status of the day of every planted variety, by block and
 * variety</li>
 * </ul>
 * <p>
 * Read-only. The statuses are estimated on every request with the rules of the plant alerts, and a field
 * observation of the season under way takes precedence; no status is stored, and none blocks anything.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/harvest-status")
@AllArgsConstructor
@Tag(name = "Plants — Harvest status", description = "Maturity status and estimated harvest date of the planted "
      + "varieties, computed on every request from the estimate and the field observations")
public class HarvestStatusController {

   private final HarvestStatusService harvestStatusService;

   @GetMapping
   @Operation(summary = "Get the harvest status of the day of the planted varieties",
         description = "The harvest window of a variety starts on the first month of its season in which the "
               + "trees give a yield, as in the yield forecast and the alerts. READY_FOR_HARVEST: the window is "
               + "open and no pre-harvest interval runs on the block; RIPE: the window opens within withinDays "
               + "days, or is open while a pre-harvest interval runs; IMMATURE otherwise. That estimate is "
               + "estimatedStatus. A field observation counts for the season under way on its day, or the next "
               + "one, until the last day of that season, and the latest one that counts gives maturityStatus, "
               + "with statusSource field_observation; READY_FOR_HARVEST is still held back to RIPE while a "
               + "pre-harvest interval runs. A variety absent from the agronomic reference has no status, and "
               + "its latest observation is shown without counting.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The status of every planted variety, possibly none"),
         @ApiResponse(responseCode = "400", description = "withinDays outside 1 to 90, or a non-numeric parameter")
   })
   public ResponseEntity<HarvestStatusResponse> getHarvestStatus(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"A\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Valid @ParameterObject PlantAlertWithinDays window) {
      HarvestStatusResponse harvestStatusResponse =
            harvestStatusService.getHarvestStatus(farmId, blockCode, window.withinDays());
      return ResponseEntity.status(HttpStatus.OK).body(harvestStatusResponse);
   }
}
