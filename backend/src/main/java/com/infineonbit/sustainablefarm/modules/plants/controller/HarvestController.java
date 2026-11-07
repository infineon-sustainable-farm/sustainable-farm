package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.ConcurrentPlantingRetry;
import com.infineonbit.sustainablefarm.modules.plants.service.HarvestService;

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
 * REST controller recording the harvests, variety by variety.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/plants/harvests} — record a harvest of a planted variety</li>
 * <li>{@code GET /api/plants/harvests} — list of harvests, optionally filtered, for example by lot number</li>
 * <li>{@code GET /api/plants/harvests/{id}} — a single harvest</li>
 * </ul>
 * <p>
 * Errors use the application-wide {@code ApiError} body: 400 with
 * {@code fieldErrors} for an invalid request, 404 for an unknown harvest, 409
 * when other harvests of the block took the next lot numbers at the same time,
 * 422 when the variety has no planting on that block or the harvest predates
 * it.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/harvests")
@AllArgsConstructor
@Tag(name = "Plants — Harvests", description = "Recorded harvests, variety by variety")
public class HarvestController {

   private final HarvestService harvestService;
   private final ConcurrentPlantingRetry concurrentPlantingRetry;

   @PostMapping
   @Operation(summary = "Record a harvest",
         description = "The variety must have a recorded planting on the block, and the harvest cannot predate it. "
               + "Several harvests on the same day are allowed, one per picking. Each harvest gets a lot number, "
               + "LOT-<year of the harvest date>-<block>-<sequence>, for example LOT-2026-A-002: the sequence "
               + "follows the order in which the harvests of the block and year are recorded. The server gives "
               + "the lot number; a lotNumber sent in the request is ignored.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The recorded harvest, with its lot number"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "409", description = "Other harvests of the block were being recorded at the "
               + "same time and took the next lot numbers; nothing was saved"),
         @ApiResponse(responseCode = "422", description = "No planting of this variety on this block, "
               + "or a harvest date before the planting date")
   })
   public ResponseEntity<HarvestResponse> recordHarvest(@Valid @RequestBody HarvestRequest harvestRequest) {
      // A harvest that lost a race on its lot number is run once more; see ConcurrentPlantingRetry.
      HarvestResponse harvestResponse = concurrentPlantingRetry.runRetryingOnce(harvestRequest.blockCode(),
            () -> harvestService.recordHarvest(harvestRequest));
      return ResponseEntity.status(HttpStatus.CREATED).body(harvestResponse);
   }

   @GetMapping
   @Operation(summary = "List harvests",
         description = "Every filter is optional and independent. The lot number is matched exactly, ignoring "
               + "surrounding spaces and case. Both dates are included. A filter matching nothing, such as an "
               + "unknown lot number, or a from date after the to date, returns an empty list.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching harvests by date, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format")
   })
   public ResponseEntity<List<HarvestResponse>> getAllHarvests(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"A\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "Lot number, for example \"LOT-2026-A-002\"; \" lot-2026-a-002 \" finds it too")
         @RequestParam(name = "lotNumber", required = false) String lotNumber,
         @Parameter(description = "First harvest date, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last harvest date, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      List<HarvestResponse> harvestResponses = harvestService.getAllHarvests(farmId, blockCode, lotNumber, from, to);
      return ResponseEntity.status(HttpStatus.OK).body(harvestResponses);
   }

   @GetMapping("/{id}")
   @Operation(summary = "Get one harvest by ID")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The harvest"),
         @ApiResponse(responseCode = "404", description = "No harvest with this ID")
   })
   public ResponseEntity<HarvestResponse> getHarvestById(@PathVariable Long id) {
      HarvestResponse harvestResponse = harvestService.getHarvestById(id);
      return ResponseEntity.status(HttpStatus.OK).body(harvestResponse);
   }
}
