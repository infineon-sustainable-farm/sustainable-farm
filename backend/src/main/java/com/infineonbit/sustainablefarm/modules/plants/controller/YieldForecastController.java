package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.YieldForecastMonths;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService;

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
 * REST controller exposing the expected mango yield by month.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/yield-forecast} — expected yield of the planted varieties, month by month</li>
 * </ul>
 * <p>
 * Read-only. The forecast is computed on every request from the recorded
 * plantings and the agronomic reference; nothing of it is stored.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/yield-forecast")
@AllArgsConstructor
@Tag(name = "Plants — Yield forecast", description = "Expected mango yield by month, from the recorded plantings")
public class YieldForecastController {

   private final YieldForecastService yieldForecastService;

   @GetMapping
   @Operation(summary = "Forecast the expected yield by month",
         description = "Only varieties with a recorded planting count. For each month: trees × yield per tree × "
               + "share of the growth phase on the first day of the month × share of the month in the harvest "
               + "season. Varieties absent from the agronomic reference are listed in varietiesWithoutReference.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The forecast; every month has a total, zeros included"),
         @ApiResponse(responseCode = "400", description = "months outside 1 to 24, or from not in the yyyy-MM format")
   })
   public ResponseEntity<YieldForecastResponse> getYieldForecast(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"A\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "First month, as yyyy-MM; the current month by default") @RequestParam(name = "from", required = false) YearMonth from,
         @Valid @ParameterObject YieldForecastMonths window) {
      YieldForecastResponse yieldForecastResponse =
            yieldForecastService.getYieldForecast(farmId, blockCode, from, window.months());
      return ResponseEntity.status(HttpStatus.OK).body(yieldForecastResponse);
   }
}
