package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.CurrencyRateRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.CurrencyRateResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.CurrencyRateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller of the EUR to XOF rate, which converts the cost of a
 * fertilizer purchase between FCFA and euros.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/currency-rates/EUR/XOF} — how many FCFA one euro is worth</li>
 * <li>{@code PUT /api/plants/currency-rates/EUR/XOF} — enter the rate, or replace it</li>
 * </ul>
 * <p>
 * The path names its only pair, the euro and the currency of the farm. Outside
 * the dev profile no rate exists until the user enters one; until then, each
 * cost is given only in its own currency. Errors use the application-wide
 * {@code ApiError} body: 400 with {@code fieldErrors} for an invalid request,
 * 404 when no rate is recorded yet, 409 when two first rates are sent at the
 * same time.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/currency-rates")
@AllArgsConstructor
@Tag(name = "Plants — Exchange rate", description = "How many FCFA one euro is worth, entered by the user")
public class CurrencyRateController {

   private final CurrencyRateService currencyRateService;

   @GetMapping("/EUR/XOF")
   @Operation(summary = "Get the EUR to XOF rate", description = "How many FCFA one euro is worth, with its source.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The rate"),
         @ApiResponse(responseCode = "404", description = "No rate recorded yet")
   })
   public ResponseEntity<CurrencyRateResponse> getEurToXofRate() {
      CurrencyRateResponse rateResponse = currencyRateService.getEurToXof();
      return ResponseEntity.status(HttpStatus.OK).body(rateResponse);
   }

   @PutMapping("/EUR/XOF")
   @Operation(summary = "Enter or replace the EUR to XOF rate",
         description = "201 for the first rate, 200 when it replaces the recorded one. source is optional, "
               + "user_entry when omitted. The rate counts at once for every cost read afterwards, including the "
               + "purchases recorded before it.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The rate that replaced the recorded one"),
         @ApiResponse(responseCode = "201", description = "The first rate"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "409", description = "Another first rate was being recorded at the same time; "
               + "nothing was saved")
   })
   public ResponseEntity<CurrencyRateResponse> saveEurToXofRate(
         @Valid @RequestBody CurrencyRateRequest currencyRateRequest) {
      CurrencyRateService.SavedRate savedRate = currencyRateService.saveEurToXof(currencyRateRequest);
      return ResponseEntity.status(savedRate.created() ? HttpStatus.CREATED : HttpStatus.OK).body(savedRate.rate());
   }
}
