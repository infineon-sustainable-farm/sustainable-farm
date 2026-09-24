package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerMovementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller listing the movements of the fertilizer stocks.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/fertilizer-movements} — purchases, applications and losses, optionally filtered</li>
 * </ul>
 * <p>
 * Read-only. The movements are written through {@link FertilizerController}. The
 * applications of a block over a period give the GLOBALG.A.P. application record:
 * block, date, fertilizer name and type, quantity and applicator.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/fertilizer-movements")
@AllArgsConstructor
@Tag(name = "Plants — Fertilizer movements", description = "Purchases, applications and losses of fertilizer")
public class FertilizerMovementController {

   private final FertilizerMovementService fertilizerMovementService;

   @GetMapping
   @Operation(summary = "List fertilizer movements",
         description = "Every filter is optional and independent. Both dates are included. The farm and block "
               + "filters match applications only. A filter matching nothing, or a from date after the to date, "
               + "returns an empty list. Costs are given in both currencies with the stored rate.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching movements by date, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format, or an unknown "
               + "movementType or a non-numeric identifier")
   })
   public ResponseEntity<List<FertilizerMovementResponse>> getAllMovements(
         @Parameter(description = "Fertilizer identifier") @RequestParam(name = "fertilizerId", required = false) Long fertilizerId,
         @Parameter(description = "PURCHASE, APPLICATION, LOSS, ADJUSTMENT_IN or ADJUSTMENT_OUT") @RequestParam(name = "movementType", required = false) FertilizerMovementType movementType,
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"B\"") @RequestParam(name = "blockCode", required = false) String blockCode,
         @Parameter(description = "First movement date, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last movement date, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      List<FertilizerMovementResponse> movementResponses =
            fertilizerMovementService.getAllMovements(fertilizerId, movementType, farmId, blockCode, from, to);
      return ResponseEntity.status(HttpStatus.OK).body(movementResponses);
   }
}
