package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PreventiveTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthTreatmentService;

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
 * REST controller of the plant protection treatment record.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/plants/treatments} — record a preventive treatment, answering no finding</li>
 * <li>{@code GET /api/plants/treatments} — the treatment record, optionally filtered</li>
 * </ul>
 * <p>
 * A treatment answering a finding is recorded through
 * {@link HealthFindingController}; both kinds appear in the record. Errors use
 * the application-wide {@code ApiError} body: 400 with {@code fieldErrors} for
 * an invalid request, 422 when the targeted code is not in the catalogue.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/treatments")
@AllArgsConstructor
@Tag(name = "Plants — Treatments", description = "Plant protection treatments, the GLOBALG.A.P. treatment record")
public class HealthTreatmentController {

   private final HealthTreatmentService healthTreatmentService;

   @PostMapping
   @Operation(summary = "Record a preventive treatment",
         description = "The block needs no recorded planting. targetIssueCode is a code of the catalogue; with "
               + "OTHER, targetOtherLabel names the problem. harvestAllowedFrom is the treatment date plus the "
               + "pre-harvest interval.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The recorded treatment"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "422", description = "The targeted code is not in the catalogue")
   })
   public ResponseEntity<HealthTreatmentResponse> recordPreventiveTreatment(
         @Valid @RequestBody PreventiveTreatmentRequest treatmentRequest) {
      HealthTreatmentResponse treatmentResponse = healthTreatmentService.recordPreventiveTreatment(treatmentRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(treatmentResponse);
   }

   @GetMapping
   @Operation(summary = "List treatments",
         description = "Every filter is optional and independent. Both dates are included. A filter matching "
               + "nothing, or a from date after the to date, returns an empty list.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching treatments by date, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format")
   })
   public ResponseEntity<List<HealthTreatmentResponse>> getAllTreatments(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"C\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "First treatment date, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last treatment date, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      List<HealthTreatmentResponse> treatmentResponses =
            healthTreatmentService.getAllTreatments(farmId, blockCode, from, to);
      return ResponseEntity.status(HttpStatus.OK).body(treatmentResponses);
   }
}
