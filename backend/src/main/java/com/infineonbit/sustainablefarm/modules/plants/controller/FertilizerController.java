package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PurchaseRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerMovementService;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerService;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller managing the fertilizer catalogue and its stock.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code POST /api/plants/fertilizers} — add a fertilizer to the catalogue</li>
 * <li>{@code GET /api/plants/fertilizers} — the catalogue, each fertilizer with its stock</li>
 * <li>{@code GET /api/plants/fertilizers/{id}} — a single fertilizer with its stock</li>
 * <li>{@code POST /api/plants/fertilizers/{id}/purchases} — record a purchase, in FCFA or euros</li>
 * </ul>
 * <p>
 * The stock is never stored: it is computed on every read from the recorded
 * movements. Errors use the application-wide {@code ApiError} body: 400 with
 * {@code fieldErrors} for an invalid request, 404 for an unknown fertilizer, 409
 * when the name is already in the catalogue.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/fertilizers")
@AllArgsConstructor
@Tag(name = "Plants — Fertilizers", description = "Fertilizer catalogue, with a stock computed from the movements")
public class FertilizerController {

   private final FertilizerService fertilizerService;
   private final FertilizerMovementService fertilizerMovementService;

   @PostMapping
   @Operation(summary = "Add a fertilizer to the catalogue",
         description = "The name must not be in the catalogue yet, ignoring case, accents and surrounding spaces. "
               + "The new fertilizer has a stock of 0 until a purchase is recorded.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The created fertilizer"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "409", description = "A fertilizer with this name already exists")
   })
   public ResponseEntity<FertilizerResponse> createFertilizer(@Valid @RequestBody FertilizerRequest fertilizerRequest) {
      FertilizerResponse fertilizerResponse = fertilizerService.createFertilizer(fertilizerRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(fertilizerResponse);
   }

   @GetMapping
   @Operation(summary = "List the fertilizers",
         description = "Ordered by name. currentStock is computed from the recorded movements; belowThreshold is "
               + "true when a reorder threshold exists and the stock is at or below it.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The catalogue, possibly an empty list")
   })
   public ResponseEntity<List<FertilizerResponse>> getAllFertilizers() {
      List<FertilizerResponse> fertilizerResponses = fertilizerService.getAllFertilizers();
      return ResponseEntity.status(HttpStatus.OK).body(fertilizerResponses);
   }

   @GetMapping("/{id}")
   @Operation(summary = "Get one fertilizer by ID, with its stock")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The fertilizer"),
         @ApiResponse(responseCode = "404", description = "No fertilizer with this ID")
   })
   public ResponseEntity<FertilizerResponse> getFertilizerById(@PathVariable Long id) {
      FertilizerResponse fertilizerResponse = fertilizerService.getFertilizerById(id);
      return ResponseEntity.status(HttpStatus.OK).body(fertilizerResponse);
   }

   @PostMapping("/{id}/purchases")
   @Operation(summary = "Record a purchase of a fertilizer",
         description = "Adds the quantity, in the unit of the fertilizer, to its stock. totalCost is optional; its "
               + "currency is XOF when omitted, and a currency without a cost is ignored. The response gives the "
               + "cost in both currencies, converted with the rate stored in currency_rate.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The recorded purchase"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "404", description = "No fertilizer with this ID")
   })
   public ResponseEntity<FertilizerMovementResponse> recordPurchase(@PathVariable Long id,
                                                                    @Valid @RequestBody PurchaseRequest purchaseRequest) {
      FertilizerMovementResponse movementResponse = fertilizerMovementService.recordPurchase(id, purchaseRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(movementResponse);
   }
}
