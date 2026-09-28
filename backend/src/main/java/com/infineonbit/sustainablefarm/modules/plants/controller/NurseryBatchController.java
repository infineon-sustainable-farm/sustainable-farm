package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryBatchResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.service.NurseryBatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller following the batches of young plants in the nursery.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/plants/nursery-batches} — start a batch, sown on the farm or purchased</li>
 * <li>{@code GET /api/plants/nursery-batches} — the batches with their computed counts and stage</li>
 * <li>{@code GET /api/plants/nursery-batches/{id}} — a single batch</li>
 * </ul>
 * <p>
 * The current number of plants, the survival rate and the current stage are
 * never stored: they are computed on every read from the recorded events.
 * Errors use the application-wide {@code ApiError} body: 400 with
 * {@code fieldErrors} for an invalid request, 404 for an unknown batch, 409
 * when the farm already has a batch with this code, 422 when the planned
 * transplant date is before the start date.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/nursery-batches")
@AllArgsConstructor
@Tag(name = "Plants — Nursery batches", description = "Young plants followed by batch, from the seed to the orchard")
public class NurseryBatchController {

   private final NurseryBatchService nurseryBatchService;

   @PostMapping
   @Operation(summary = "Start a nursery batch",
         description = "The code must not be used yet by a batch of the same farm, ignoring case. The supplier is "
               + "required for a purchased batch and refused otherwise. The initial stage is recorded as the first "
               + "stage change, dated on startedOn. The planned transplant date may be in the future.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The created batch, with its computed values"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "409", description = "The farm already has a batch with this code"),
         @ApiResponse(responseCode = "422", description = "The planned transplant date is before the start date")
   })
   public ResponseEntity<NurseryBatchResponse> createBatch(@Valid @RequestBody NurseryBatchRequest batchRequest) {
      NurseryBatchResponse batchResponse = nurseryBatchService.createBatch(batchRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(batchResponse);
   }

   @GetMapping
   @Operation(summary = "List the nursery batches",
         description = "Ordered by start date. Both filters are optional and independent. The stage filter "
               + "matches the current stage, computed from the stage changes.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching batches, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "An unknown stage or a non-numeric farm")
   })
   public ResponseEntity<List<NurseryBatchResponse>> getAllBatches(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "GERMINATION, ROOTSTOCK_GROWTH, GRAFTED, HARDENING or READY_TO_TRANSPLANT") @RequestParam(name = "stage", required = false) NurseryStage stage) {
      List<NurseryBatchResponse> batchResponses = nurseryBatchService.getAllBatches(farmId, stage);
      return ResponseEntity.status(HttpStatus.OK).body(batchResponses);
   }

   @GetMapping("/{id}")
   @Operation(summary = "Get one nursery batch by ID, with its computed values")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The batch"),
         @ApiResponse(responseCode = "404", description = "No batch with this ID")
   })
   public ResponseEntity<NurseryBatchResponse> getBatchById(@PathVariable Long id) {
      NurseryBatchResponse batchResponse = nurseryBatchService.getBatchById(id);
      return ResponseEntity.status(HttpStatus.OK).body(batchResponse);
   }
}
