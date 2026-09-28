package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FindingTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthFindingService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for the problems seen during the inspections.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/health-findings} — the findings as a flat list, with their status</li>
 * <li>{@code POST /api/plants/health-findings/{id}/treatments} — record a treatment answering a finding</li>
 * </ul>
 * <p>
 * The findings are written with their inspection, through
 * {@link HealthInspectionController}. Errors use the application-wide
 * {@code ApiError} body: 400 with {@code fieldErrors} for an invalid request,
 * 404 for an unknown finding, 422 when a treatment predates the inspection or
 * its finding is resolved.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/health-findings")
@AllArgsConstructor
@Tag(name = "Plants — Health findings", description = "Problems seen during the inspections, with their status")
public class HealthFindingController {

   private final HealthFindingService healthFindingService;
   private final HealthTreatmentService healthTreatmentService;

   @GetMapping
   @Operation(summary = "List findings",
         description = "Every filter is optional and independent. The farm, the block and both dates, included, "
               + "are those of the inspection. The status, the treatment count, the last treatment date and "
               + "harvestAllowedFrom are computed from the treatments and the resolution.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching findings by inspection date, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format, or an unknown status")
   })
   public ResponseEntity<List<HealthFindingResponse>> getAllFindings(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"C\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "UNTREATED, IN_PROGRESS, TREATED or CLOSED_WITHOUT_TREATMENT") @RequestParam(name = "status", required = false) HealthFindingStatus status,
         @Parameter(description = "First inspection date, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last inspection date, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      List<HealthFindingResponse> findingResponses =
            healthFindingService.getAllFindings(farmId, blockCode, status, from, to);
      return ResponseEntity.status(HttpStatus.OK).body(findingResponses);
   }

   @PostMapping("/{id}/treatments")
   @Operation(summary = "Record a treatment answering a finding",
         description = "The farm, the block and the targeted issue are copied from the finding. The treatment "
               + "cannot predate the inspection, and a resolved finding cannot be treated any more.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The recorded treatment"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "404", description = "No finding with this ID"),
         @ApiResponse(responseCode = "422", description = "The treatment predates the inspection, or the finding "
               + "is resolved")
   })
   public ResponseEntity<HealthTreatmentResponse> recordTreatment(@PathVariable Long id,
                                                                  @Valid @RequestBody FindingTreatmentRequest treatmentRequest) {
      HealthTreatmentResponse treatmentResponse = healthTreatmentService.recordFindingTreatment(id, treatmentRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(treatmentResponse);
   }
}
