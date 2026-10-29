package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.MaturityObservationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.MaturityObservationResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.MaturityObservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller recording the maturity seen in the field, variety by variety.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/plants/maturity-observations} — record a field observation of a planted variety</li>
 * <li>{@code GET /api/plants/maturity-observations} — list of observations, optionally filtered</li>
 * </ul>
 * <p>
 * An observation is never changed nor deleted: a newer one takes its place in
 * the harvest status. Errors use the application-wide {@code ApiError} body:
 * 400 with {@code fieldErrors} for an invalid request, 422 when the variety
 * has no planting on that block or the observation predates it.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/maturity-observations")
@AllArgsConstructor
@Tag(name = "Plants — Maturity observations", description = "Maturity seen in the field, variety by variety")
public class MaturityObservationController {

   private final MaturityObservationService maturityObservationService;

   @PostMapping
   @Operation(summary = "Record a maturity observation",
         description = "The variety must have a recorded planting on the block, and the observation cannot "
               + "predate it. A measured value goes with the BRIX or DRY_MATTER method only. Several "
               + "observations on the same day are allowed: the latest one counts.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The recorded observation"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "422", description = "No planting of this variety on this block, "
               + "or an observation date before the planting date")
   })
   public ResponseEntity<MaturityObservationResponse> recordObservation(
         @Valid @RequestBody MaturityObservationRequest observationRequest) {
      MaturityObservationResponse observationResponse =
            maturityObservationService.recordObservation(observationRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(observationResponse);
   }

   @GetMapping
   @Operation(summary = "List maturity observations",
         description = "Every filter is optional and independent. Both dates are included. A filter matching "
               + "nothing, or a from date after the to date, returns an empty list.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching observations by date, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format")
   })
   public ResponseEntity<List<MaturityObservationResponse>> getAllObservations(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"A\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "First observation date, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last observation date, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      List<MaturityObservationResponse> observationResponses =
            maturityObservationService.getAllObservations(farmId, blockCode, from, to);
      return ResponseEntity.status(HttpStatus.OK).body(observationResponses);
   }
}
