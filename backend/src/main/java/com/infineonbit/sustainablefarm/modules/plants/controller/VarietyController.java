package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.VarietyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing read access to the mango varieties.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/varieties} — list of varieties, optionally filtered</li>
 * <li>{@code GET /api/plants/varieties/{id}} — a single variety</li>
 * </ul>
 * <p>
 * Read-only: the module exposes no write operation yet.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/varieties")
@AllArgsConstructor
@Tag(name = "Plants — Varieties", description = "Trees, spacing and yield by variety and block")
public class VarietyController {

   private final VarietyService varietyService;

   @GetMapping
   @Operation(summary = "List varieties",
         description = "Both filters are optional and independent. A filter matching no row returns an empty list.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching varieties, possibly an empty list")
   })
   public ResponseEntity<List<VarietyResponse>> getAllVarieties(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"A\"") @RequestParam(name = "blockCode", required = false) String blockCode) {
      List<VarietyResponse> varietyResponses = varietyService.getAllVarieties(farmId, blockCode);
      return ResponseEntity.status(HttpStatus.OK).body(varietyResponses);
   }

   @GetMapping("/{id}")
   @Operation(summary = "Get one variety by ID")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The variety"),
         @ApiResponse(responseCode = "404", description = "No variety with this ID")
   })
   public ResponseEntity<VarietyResponse> getVarietyById(@PathVariable Long id) {
      VarietyResponse varietyResponse = varietyService.getVarietyById(id);
      return ResponseEntity.status(HttpStatus.OK).body(varietyResponse);
   }
}
