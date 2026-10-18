package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantingResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.ConcurrentPlantingRetry;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller recording the orchard, block by block, as dated plantings.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/plants/plantings} — record the planting of a variety on a block</li>
 * </ul>
 * <p>
 * Errors use the application-wide {@code ApiError} body: 400 with
 * {@code fieldErrors} for an invalid request, 409 when the variety is already
 * planted on that block, or when another planting of the block was being
 * recorded at the same time and could not be caught up with.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/plantings")
@AllArgsConstructor
@Tag(name = "Plants — Plantings", description = "Dated plantings, recorded block by block")
public class PlantingController {

   private final PlantingService plantingService;
   private final ConcurrentPlantingRetry concurrentPlantingRetry;

   @PostMapping
   @Operation(summary = "Record a planting",
         description = "Creates the variety row of the block if it does not exist yet, and moves the block's "
               + "calendar date back when this planting is the oldest. The current number of trees is then "
               + "computed from the recorded events.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The recorded planting"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "409", description = "This variety is already planted on this block, or another "
               + "planting of the block was being recorded at the same time; nothing was saved")
   })
   public ResponseEntity<PlantingResponse> recordPlanting(@Valid @RequestBody PlantingRequest plantingRequest) {
      // A planting that lost a race on a new block or triple is run once more; see ConcurrentPlantingRetry.
      PlantingResponse plantingResponse = concurrentPlantingRetry.runRetryingOnce(plantingRequest.blockCode(),
            () -> plantingService.recordPlanting(plantingRequest));
      return ResponseEntity.status(HttpStatus.CREATED).body(plantingResponse);
   }
}
