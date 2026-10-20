package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.YieldForecastMonths;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldComparisonService;

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

import java.time.YearMonth;

/**
 * REST controller comparing the actual mango yield with the expected yield.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/yield-comparison} — actual against expected yield of the planted varieties, by
 * variety, by block and in total, over the same months</li>
 * </ul>
 * <p>
 * Read-only. Both yields are computed on every request, the expected one with the rule of the yield forecast and
 * the actual one from the recorded harvests; nothing of them is stored.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/yield-comparison")
@AllArgsConstructor
@Tag(name = "Plants — Yield comparison", description = "Actual against expected yield, over the same months")
public class YieldComparisonController {

   private final YieldComparisonService yieldComparisonService;

   @GetMapping
   @Operation(summary = "Compare the actual and the expected yield",
         description = "Over whole months, the calendar year of today by default. The expected yield of a month is "
               + "that of GET /api/plants/yield-forecast for the same month; the actual yield adds up the harvests "
               + "dated in the months. Varieties without an agronomic reference, or with no tree left, have no "
               + "expected yield and are listed in notCompared with their harvests.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The comparison, possibly with no variety"),
         @ApiResponse(responseCode = "400", description = "months outside 1 to 24, or from not in the yyyy-MM format")
   })
   public ResponseEntity<YieldComparisonResponse> getYieldComparison(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"A\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "First month, as yyyy-MM; January of the current year by default") @RequestParam(name = "from", required = false) YearMonth from,
         @Valid @ParameterObject YieldForecastMonths window) {
      YieldComparisonResponse yieldComparisonResponse =
            yieldComparisonService.getYieldComparison(farmId, blockCode, from, window.months());
      return ResponseEntity.status(HttpStatus.OK).body(yieldComparisonResponse);
   }
}
