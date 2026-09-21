package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthCalendarResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.GrowthCalendarService;

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
 * REST controller exposing read access to the growth calendar.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/growth-calendar} — list of entries, optionally filtered</li>
 * <li>{@code GET /api/plants/growth-calendar/{id}} — a single entry</li>
 * </ul>
 * <p>
 * Read-only. Tree age and growth phase are computed from the planting date on
 * every read; they are never stored nor accepted as input.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/growth-calendar")
@AllArgsConstructor
@Tag(name = "Plants — Growth calendar", description = "Planting dates, computed tree age and growth phase by block")
public class GrowthCalendarController {

   private final GrowthCalendarService growthCalendarService;

   @GetMapping
   @Operation(summary = "List growth calendar entries",
         description = "Both filters are optional and independent. A filter matching no row returns an empty list. "
               + "Age and phase are computed from the planting date and are null when it is unknown.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching entries, possibly an empty list")
   })
   public ResponseEntity<List<GrowthCalendarResponse>> getAllGrowthCalendarEntries(
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "Raw block value as stored, for example \"A\"") @RequestParam(name = "blockCode", required = false) String blockCode) {
      List<GrowthCalendarResponse> growthCalendarResponses = growthCalendarService.obtainAllGrowthCalendarEntries(farmId, blockCode);
      return ResponseEntity.status(HttpStatus.OK).body(growthCalendarResponses);
   }

   @GetMapping("/{id}")
   @Operation(summary = "Get one growth calendar entry by ID")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The entry"),
         @ApiResponse(responseCode = "404", description = "No entry with this ID")
   })
   public ResponseEntity<GrowthCalendarResponse> getGrowthCalendarEntryById(@PathVariable Long id) {
      GrowthCalendarResponse growthCalendarResponse = growthCalendarService.obtainGrowthCalendarEntryById(id);
      return ResponseEntity.status(HttpStatus.OK).body(growthCalendarResponse);
   }
}
