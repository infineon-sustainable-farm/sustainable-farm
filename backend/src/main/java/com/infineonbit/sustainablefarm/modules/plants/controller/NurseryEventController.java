package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryEventResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.service.NurseryEventService;

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
 * REST controller listing what happened to the nursery batches.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/nursery-events} — stage changes, losses and transplants, optionally filtered</li>
 * </ul>
 * <p>
 * Read-only. The events are written through {@link NurseryBatchController}.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/nursery-events")
@AllArgsConstructor
@Tag(name = "Plants — Nursery events", description = "Stage changes, losses and transplants of the nursery batches")
public class NurseryEventController {

   private final NurseryEventService nurseryEventService;

   @GetMapping
   @Operation(summary = "List nursery events",
         description = "Every filter is optional and independent. The farm is that of the batch. Both dates are "
               + "included. A filter matching nothing, such as an unknown batch, or a from date after the to date, "
               + "returns an empty list.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Matching events by date, possibly an empty list"),
         @ApiResponse(responseCode = "400", description = "A date is not in the yyyy-MM-dd format, or an unknown "
               + "eventType or a non-numeric identifier")
   })
   public ResponseEntity<List<NurseryEventResponse>> getAllEvents(
         @Parameter(description = "Batch identifier") @RequestParam(name = "batchId", required = false) Long batchId,
         @Parameter(description = "Farm identifier") @RequestParam(name = "farmId", required = false) Integer farmId,
         @Parameter(description = "STAGE_CHANGE, LOSS or TRANSPLANT") @RequestParam(name = "eventType", required = false) NurseryEventType eventType,
         @Parameter(description = "First event date, included, as yyyy-MM-dd") @RequestParam(name = "from", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
         @Parameter(description = "Last event date, included, as yyyy-MM-dd") @RequestParam(name = "to", required = false)
         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
      List<NurseryEventResponse> eventResponses =
            nurseryEventService.getAllEvents(batchId, farmId, eventType, from, to);
      return ResponseEntity.status(HttpStatus.OK).body(eventResponses);
   }
}
