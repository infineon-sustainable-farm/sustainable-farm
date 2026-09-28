package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthInspectionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthInspectionResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthInspectionService;

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
 * REST controller recording the health inspections of the blocks.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/plants/health-inspections} — record an inspection with the problems seen</li>
 * <li>{@code GET /api/plants/health-inspections} — list of inspections, optionally filtered</li>
 * </ul>
 * <p>
 * Errors use the application-wide {@code ApiError} body: 400 with
 * {@code fieldErrors} for an invalid request, a finding's under its index;
 * 422 when an issue code is not in the catalogue, in which case nothing is
 * recorded.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/health-inspections")
@AllArgsConstructor
@Tag(name = "Plants — Health inspections", description = "Visits of the blocks, with a health score and the problems seen")
public class HealthInspectionController {

   private final HealthInspectionService healthInspectionService;

   @PostMapping
   @Operation(summary = "Record an inspection of a block",
         description = "The inspection and its findings are recorded together, or not at all. An empty findings "
               + "list records a visit with nothing found. The block needs no recorded planting.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The recorded inspection, with its findings"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "422", description = "An issue code is not in the catalogue")
   })
   public ResponseEntity<HealthInspectionResponse> recordInspection(
         @Valid @RequestBody HealthInspectionRequest inspectionRequest) {
      HealthInspectionResponse inspectionResponse = healthInspectionService.recordInspection(inspectionRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(inspectionResponse);
   }

   @GetMapping
   @Operation(summary = "List inspections",
         description = "Every filter is optional and independent. Both dates are included. A filter matching "
               + "nothing, or a from date after the to date, returns an empty list.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching inspections by date, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format")
   })
   public ResponseEntity<List<HealthInspectionResponse>> getAllInspections(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"C\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "First inspection date, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last inspection date, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      List<HealthInspectionResponse> inspectionResponses =
            healthInspectionService.getAllInspections(farmId, blockCode, from, to);
      return ResponseEntity.status(HttpStatus.OK).body(inspectionResponses);
   }
}
