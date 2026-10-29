package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.VarietyReferenceRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyReferenceResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.VarietyReferenceService;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller of the agronomic reference of the varieties: the yield of
 * one tree in full production and the harvest season, which the yield forecast
 * and the plant alerts use.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/variety-references} — the reference of every variety</li>
 * <li>{@code POST /api/plants/variety-references} — enter the reference of a variety</li>
 * <li>{@code PUT /api/plants/variety-references/{id}} — correct a reference</li>
 * </ul>
 * <p>
 * Outside the dev profile the reference starts empty: a planted variety is
 * listed apart by the forecast until its reference is entered. One reference
 * serves a variety on every farm and block. Errors use the application-wide
 * {@code ApiError} body: 400 with {@code fieldErrors} for an invalid request,
 * 404 for an unknown reference, 409 when the name is already in the reference.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/variety-references")
@AllArgsConstructor
@Tag(name = "Plants — Variety references",
      description = "Yield per tree and harvest season of each variety, with their sources, entered by the user")
public class VarietyReferenceController {

   private final VarietyReferenceService varietyReferenceService;

   @GetMapping
   @Operation(summary = "List the variety references",
         description = "Ordered by name, ignoring case and accents. Each value has its source.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The reference, possibly an empty list")
   })
   public ResponseEntity<List<VarietyReferenceResponse>> getAllReferences() {
      List<VarietyReferenceResponse> referenceResponses = varietyReferenceService.getAllReferences();
      return ResponseEntity.status(HttpStatus.OK).body(referenceResponses);
   }

   @PostMapping
   @Operation(summary = "Enter the reference of a variety",
         description = "The name must not be in the reference yet, ignoring case, accents and surrounding spaces. "
               + "yieldPerTreeKg is above 0 and at most 1000, with at most 1 decimal. The harvest months run "
               + "from 1 to 12; an end month lower than the start month is a season over the new year. "
               + "yieldSource and seasonSource are optional, user_entry when omitted.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The entered reference"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "409", description = "A reference with this name already exists")
   })
   public ResponseEntity<VarietyReferenceResponse> createReference(
         @Valid @RequestBody VarietyReferenceRequest varietyReferenceRequest) {
      VarietyReferenceResponse referenceResponse = varietyReferenceService.createReference(varietyReferenceRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(referenceResponse);
   }

   @PutMapping("/{id}")
   @Operation(summary = "Correct the reference of a variety",
         description = "Every value is replaced, with the rules of the entry. A source left out stays as it was "
               + "when its value does not change, and becomes user_entry when it does: the yield for "
               + "yieldSource, the two harvest months for seasonSource. The correction counts at once for every "
               + "forecast and alert read afterwards.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The corrected reference"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "404", description = "No reference with this ID"),
         @ApiResponse(responseCode = "409", description = "Another reference has this name")
   })
   public ResponseEntity<VarietyReferenceResponse> updateReference(
         @PathVariable Long id, @Valid @RequestBody VarietyReferenceRequest varietyReferenceRequest) {
      VarietyReferenceResponse referenceResponse =
            varietyReferenceService.updateReference(id, varietyReferenceRequest);
      return ResponseEntity.status(HttpStatus.OK).body(referenceResponse);
   }
}
