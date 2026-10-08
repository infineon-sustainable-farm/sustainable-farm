package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.GrowthPhaseYieldShareRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.GrowthPhaseYieldShareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller of the yield share of each growth phase, which the yield
 * forecast and the plant alerts apply to the full-production yield of a variety.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/growth-phase-yield-shares} — the share in effect of every phase</li>
 * <li>{@code PUT /api/plants/growth-phase-yield-shares/{code}} — correct the share of a phase</li>
 * </ul>
 * <p>
 * Every phase has a sourced default share, so the GET always lists the three
 * phases, and a PUT always replaces a share: it answers 200, never 201. The
 * forecast and the alerts read the shares on every request, so a correction
 * counts at once. Errors use the application-wide {@code ApiError} body: 400
 * with {@code fieldErrors} for an invalid request, 404 for an unknown code, 409
 * when two first corrections of a phase are sent at the same time.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/growth-phase-yield-shares")
@AllArgsConstructor
@Tag(name = "Plants — Growth phase yield shares",
      description = "Share of the full yield given in each growth phase, with a sourced default the user can correct")
public class GrowthPhaseYieldShareController {

   private final GrowthPhaseYieldShareService growthPhaseYieldShareService;

   @GetMapping
   @Operation(summary = "List the yield share of every growth phase",
         description = "The youngest phase first. yieldShare and source are in effect: the user's correction, or "
               + "the default of the phase. The default stays in defaultYieldShare and defaultSource.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The three phases")
   })
   public ResponseEntity<List<GrowthPhaseYieldShareResponse>> getAllShares() {
      List<GrowthPhaseYieldShareResponse> shareResponses = growthPhaseYieldShareService.getAllShares();
      return ResponseEntity.status(HttpStatus.OK).body(shareResponses);
   }

   @PutMapping("/{code}")
   @Operation(summary = "Correct the yield share of a growth phase",
         description = "code is ESTABLISHMENT, GRADUAL_PRODUCTION or FULL_PRODUCTION; case and surrounding spaces "
               + "are ignored. yieldShare runs from 0 to 1, with at most 3 decimals. source is optional, user_entry "
               + "when omitted. The share counts at once for every forecast and alert read afterwards.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The share as now in effect"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "404", description = "No growth phase with this code"),
         @ApiResponse(responseCode = "409", description = "Another first correction of the phase was being recorded "
               + "at the same time; nothing was saved")
   })
   public ResponseEntity<GrowthPhaseYieldShareResponse> saveShare(
         @PathVariable String code, @Valid @RequestBody GrowthPhaseYieldShareRequest growthPhaseYieldShareRequest) {
      GrowthPhaseYieldShareResponse shareResponse =
            growthPhaseYieldShareService.saveShare(code, growthPhaseYieldShareRequest);
      return ResponseEntity.status(HttpStatus.OK).body(shareResponse);
   }
}
