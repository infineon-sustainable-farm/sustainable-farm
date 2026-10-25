package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantScenarioRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.ScenarioVariantParams;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantScenarioResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.ScenarioAnalysisResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantScenarioService;
import com.infineonbit.sustainablefarm.modules.plants.service.ScenarioAnalysisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller of the planting scenarios: plans entered by the user, and
 * their cost-benefit analysis.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/scenarios} — every scenario</li>
 * <li>{@code GET /api/plants/scenarios/{id}} — one scenario</li>
 * <li>{@code POST /api/plants/scenarios} — enter a scenario</li>
 * <li>{@code PUT /api/plants/scenarios/{id}} — correct a scenario</li>
 * <li>{@code GET /api/plants/scenarios/{id}/analysis} — what the scenario gives, and what a variant changes</li>
 * </ul>
 * <p>
 * A scenario is a projection, not the planted orchard: the yield forecast never
 * reads it. Only its assumptions are stored; the analysis is computed on every
 * request. Outside the dev profile there is no scenario until one is entered.
 * Errors use the application-wide {@code ApiError} body: 400 with
 * {@code fieldErrors} for an invalid request, 404 for an unknown scenario, 409
 * when the name is taken, 422 for a variant the scenario cannot take.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/scenarios")
@AllArgsConstructor
@Tag(name = "Plants — Scenarios",
      description = "Planting plans entered by the user, and their cost-benefit analysis computed on every read")
public class PlantScenarioController {

   private final PlantScenarioService plantScenarioService;
   private final ScenarioAnalysisService scenarioAnalysisService;

   @GetMapping
   @Operation(summary = "List the scenarios",
         description = "Ordered by name, ignoring case and accents. Each value has its source.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The scenarios, possibly an empty list")
   })
   public ResponseEntity<List<PlantScenarioResponse>> getAllScenarios() {
      List<PlantScenarioResponse> scenarioResponses = plantScenarioService.getAllScenarios();
      return ResponseEntity.status(HttpStatus.OK).body(scenarioResponses);
   }

   @GetMapping("/{id}")
   @Operation(summary = "Get a scenario", description = "Its assumptions as entered, each with its source.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The scenario"),
         @ApiResponse(responseCode = "404", description = "No scenario with this ID")
   })
   public ResponseEntity<PlantScenarioResponse> getScenario(@PathVariable Long id) {
      PlantScenarioResponse scenarioResponse = plantScenarioService.getScenario(id);
      return ResponseEntity.status(HttpStatus.OK).body(scenarioResponse);
   }

   @PostMapping
   @Operation(summary = "Enter a scenario",
         description = "The name must not be taken, ignoring case, accents and surrounding spaces. Every amount "
               + "is in currency, XOF or EUR. lossRate, pricePerKg, variableCostPerKg and costPerAdditionalTree "
               + "are optional: left out, they stay unknown, never 0. Each source is optional, user_entry when "
               + "omitted.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The entered scenario"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "409", description = "A scenario with this name already exists")
   })
   public ResponseEntity<PlantScenarioResponse> createScenario(
         @Valid @RequestBody PlantScenarioRequest plantScenarioRequest) {
      PlantScenarioResponse scenarioResponse = plantScenarioService.createScenario(plantScenarioRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(scenarioResponse);
   }

   @PutMapping("/{id}")
   @Operation(summary = "Correct a scenario",
         description = "Every value is replaced, with the rules of the entry; an optional value left out is "
               + "removed. A source left out stays as it was when its value does not change, and becomes "
               + "user_entry when it does.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The corrected scenario"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "404", description = "No scenario with this ID"),
         @ApiResponse(responseCode = "409", description = "Another scenario has this name")
   })
   public ResponseEntity<PlantScenarioResponse> updateScenario(
         @PathVariable Long id, @Valid @RequestBody PlantScenarioRequest plantScenarioRequest) {
      PlantScenarioResponse scenarioResponse = plantScenarioService.updateScenario(id, plantScenarioRequest);
      return ResponseEntity.status(HttpStatus.OK).body(scenarioResponse);
   }

   @GetMapping("/{id}/analysis")
   @Operation(summary = "Analyse a scenario, and a variant of it",
         description = "In a year of full production: harvest = trees × yield per tree; sellable = harvest × "
               + "(1 - lossRate); revenue = sellable × price; operating cost = fixed cost + variable cost × "
               + "sellable; ROI = net benefit × 100 / investment. Year by year, the trees are n - 1 years old in "
               + "year n and give the share of their growth phase used by the yield forecast; paybackYear is the "
               + "first year whose cumulative net benefit, investment deducted, is 0 or more. A variant is asked "
               + "for with treeCountChange, yieldPerTreeChangePct, lossRate or additionalInvestment; change then "
               + "splits its annual benefit by cause. A result that needs a value not entered is null.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The analysis"),
         @ApiResponse(responseCode = "400", description = "A parameter out of bounds; fieldErrors names it"),
         @ApiResponse(responseCode = "404", description = "No scenario with this ID"),
         @ApiResponse(responseCode = "422",
               description = "The variant would leave no tree, or changes a loss rate the scenario does not have")
   })
   public ResponseEntity<ScenarioAnalysisResponse> getAnalysis(
         @PathVariable Long id, @Valid @ParameterObject ScenarioVariantParams variantParams) {
      ScenarioAnalysisResponse analysisResponse = scenarioAnalysisService.getAnalysis(id, variantParams);
      return ResponseEntity.status(HttpStatus.OK).body(analysisResponse);
   }
}
