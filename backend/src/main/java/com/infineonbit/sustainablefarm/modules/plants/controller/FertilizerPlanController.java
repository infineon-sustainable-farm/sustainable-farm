package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanCompletionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanStockDays;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanStockCheckResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
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
 * REST controller managing the applications of fertilizer planned ahead.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/plants/fertilizer-plans} — plan an application on a block</li>
 * <li>{@code GET /api/plants/fertilizer-plans} — the planned applications, optionally filtered</li>
 * <li>{@code POST /api/plants/fertilizer-plans/{id}/complete} — mark one as done, recording the real application</li>
 * <li>{@code POST /api/plants/fertilizer-plans/{id}/cancel} — cancel one</li>
 * <li>{@code GET /api/plants/fertilizer-plans/comparison} — planned and applied quantities by block</li>
 * <li>{@code GET /api/plants/fertilizer-plans/stock-check} — fertilizers whose stock does not cover the coming plans</li>
 * </ul>
 * <p>
 * No quantity is recommended: every quantity is entered by the farmer. Costs
 * are computed on every read at the last purchase price of the fertilizer.
 * Errors use the application-wide {@code ApiError} body: 400 with
 * {@code fieldErrors} for an invalid request, 404 for an unknown fertilizer or
 * plan, 409 for a plan already done or cancelled, 422 when the real
 * application exceeds the stock.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/fertilizer-plans")
@AllArgsConstructor
@Tag(name = "Plants — Fertilizer plans", description = "Applications of fertilizer planned ahead, compared with the real ones")
public class FertilizerPlanController {

   private final FertilizerPlanService fertilizerPlanService;

   @PostMapping
   @Operation(summary = "Plan an application of a fertilizer on a block",
         description = "The quantity is in the unit of the fertilizer and is the farmer's own figure. The block "
               + "must be an existing block of the farm referential (GET /api/farm/fields) and "
               + "needs no recorded planting. Planning takes nothing from the stock, so the quantity is not "
               + "compared with it. The planned cost is computed at the last purchase price of the fertilizer.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The planned application"),
         @ApiResponse(responseCode = "400", description = "Invalid request (fieldErrors lists the failing fields), "
               + "unknown block code, or a farm that does not exist or does not own the block"),
         @ApiResponse(responseCode = "404", description = "No fertilizer with the given fertilizerId")
   })
   public ResponseEntity<FertilizerPlanResponse> createPlan(@Valid @RequestBody FertilizerPlanRequest planRequest) {
      FertilizerPlanResponse planResponse = fertilizerPlanService.createPlan(planRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(planResponse);
   }

   @GetMapping
   @Operation(summary = "List the planned applications",
         description = "Every filter is optional and independent. Both dates are included and compare the planned "
               + "date. A filter matching nothing, or a from date after the to date, returns an empty list. "
               + "overdue is true for an application still PLANNED after its date; daysFromToday is the planned "
               + "date minus today. Costs are at the last purchase price, in both currencies with the stored "
               + "rate; null while no purchase of the fertilizer has a cost.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching planned applications by planned date, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format, or an unknown "
               + "status or a non-numeric identifier")
   })
   public ResponseEntity<List<FertilizerPlanResponse>> getAllPlans(
         @Parameter(description = "Fertilizer identifier") @RequestParam(name = "fertilizerId", required = false) Long fertilizerId,
         @Parameter(description = "PLANNED, DONE or CANCELLED") @RequestParam(name = "status", required = false) FertilizerPlanStatus status,
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Long farmId,
         @Parameter(description = "Block code of the farm referential, for example \"B\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "First planned date, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last planned date, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      List<FertilizerPlanResponse> planResponses =
            fertilizerPlanService.getAllPlans(fertilizerId, status, farmId, blockCode, from, to);
      return ResponseEntity.status(HttpStatus.OK).body(planResponses);
   }

   @PostMapping("/{id}/complete")
   @Operation(summary = "Mark a planned application as done",
         description = "Records the real application on the fertilizer, the farm and the block of the plan, takes "
               + "its quantity from the stock and links it to the plan. The quantity may differ from the planned "
               + "one. When the stock does not cover it, nothing is recorded and the plan stays PLANNED.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The planned application, DONE, with its real application"),
         @ApiResponse(responseCode = "400", description = "Invalid request (fieldErrors lists the failing fields), "
               + "or an applicatorPersonId the people directory does not know"),
         @ApiResponse(responseCode = "404", description = "No planned application with this ID"),
         @ApiResponse(responseCode = "409", description = "The planned application is already done or cancelled"),
         @ApiResponse(responseCode = "422", description = "The quantity exceeds the current stock")
   })
   public ResponseEntity<FertilizerPlanResponse> completePlan(@PathVariable Long id,
                                                              @Valid @RequestBody FertilizerPlanCompletionRequest completionRequest) {
      FertilizerPlanResponse planResponse = fertilizerPlanService.completePlan(id, completionRequest);
      return ResponseEntity.status(HttpStatus.OK).body(planResponse);
   }

   @PostMapping("/{id}/cancel")
   @Operation(summary = "Cancel a planned application",
         description = "Nothing is recorded in the stock. A cancelled application no longer counts in the "
               + "comparison or in the stock check.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The planned application, CANCELLED"),
         @ApiResponse(responseCode = "404", description = "No planned application with this ID"),
         @ApiResponse(responseCode = "409", description = "The planned application is already done or cancelled")
   })
   public ResponseEntity<FertilizerPlanResponse> cancelPlan(@PathVariable Long id) {
      FertilizerPlanResponse planResponse = fertilizerPlanService.cancelPlan(id);
      return ResponseEntity.status(HttpStatus.OK).body(planResponse);
   }

   @GetMapping("/comparison")
   @Operation(summary = "Compare planned and applied quantities by block",
         description = "One line per block and fertilizer. A plan counts in the period of its planned date, "
               + "with its real application once done, whatever the date of that application; cancelled "
               + "plans are left out. An application no plan is linked to counts in the period of its own "
               + "date. Without any date the period is the current calendar year, and the response gives "
               + "the period used. appliedQuantity counts both kinds of application, unplannedQuantity the "
               + "ones no plan is linked to. Both costs are at the last purchase price.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The period and its lines, possibly none"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format")
   })
   public ResponseEntity<FertilizerPlanComparisonResponse> getComparison(
         @Parameter(description = "First day, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last day, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      FertilizerPlanComparisonResponse comparisonResponse = fertilizerPlanService.getComparison(from, to);
      return ResponseEntity.status(HttpStatus.OK).body(comparisonResponse);
   }

   @GetMapping("/stock-check")
   @Operation(summary = "List the fertilizers whose stock does not cover the coming planned applications",
         description = "Counts the applications still PLANNED up to today plus days, late ones included, and "
               + "compares their total with the current stock of each fertilizer. A warning only: no plan is "
               + "refused because of it. days is 30 when omitted.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The window and its warnings, possibly none"),
         @ApiResponse(responseCode = "400", description = "days is not a number from 1 to 365; fieldErrors names it")
   })
   public ResponseEntity<FertilizerPlanStockCheckResponse> getStockCheck(@Valid @ParameterObject FertilizerPlanStockDays window) {
      FertilizerPlanStockCheckResponse stockCheckResponse = fertilizerPlanService.getStockCheck(window.days());
      return ResponseEntity.status(HttpStatus.OK).body(stockCheckResponse);
   }
}
