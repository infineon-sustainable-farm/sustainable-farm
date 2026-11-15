package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantAlertWithinDays;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantAlertService;

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
 * REST controller exposing the plant alerts.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/alerts} — the alerts of the day: harvest seasons, pre-harvest intervals, low stocks,
 * open health problems and nursery batches ready to transplant</li>
 * </ul>
 * <p>
 * Read-only. The alerts are computed on every request from the recorded data; nothing of them is stored, and they
 * block nothing.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/alerts")
@AllArgsConstructor
@Tag(name = "Plants — Alerts", description = "Harvest seasons, pre-harvest intervals, low stocks, open health "
      + "problems and nursery batches ready to transplant, computed on every request")
public class PlantAlertController {

   private final PlantAlertService plantAlertService;

   @GetMapping
   @Operation(summary = "List the plant alerts of the day",
         description = "CRITICAL first, then by date, the alerts without a date last. A harvest season starts on its "
               + "first month in which the trees give a yield, as in the yield forecast, and is announced "
               + "withinDays days before it; a season in progress is always listed. A fertilizer belongs to no "
               + "farm and no block: its stock alerts are left out when blockCode is given.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The alerts of the day, possibly none"),
         @ApiResponse(responseCode = "400", description = "withinDays outside 1 to 90, or a non-numeric parameter")
   })
   public ResponseEntity<PlantAlertResponse> getAlerts(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"C\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Valid @ParameterObject PlantAlertWithinDays window) {
      PlantAlertResponse plantAlertResponse = plantAlertService.getAlerts(farmId, blockCode, window.withinDays());
      return ResponseEntity.status(HttpStatus.OK).body(plantAlertResponse);
   }
}
