package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthIssueService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing the catalogue of pests and diseases.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/health-issues} — the catalogue</li>
 * </ul>
 * <p>
 * Read-only. The catalogue is loaded at startup and changed in the database;
 * an inspection or a treatment refers to an issue by its {@code code}.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/health-issues")
@AllArgsConstructor
@Tag(name = "Plants — Health issues", description = "Catalogue of the pests and diseases an inspection can report")
public class HealthIssueController {

   private final HealthIssueService healthIssueService;

   @GetMapping
   @Operation(summary = "List the health issues",
         description = "Pests, then diseases, then \"Other\", each by name. Each issue has the code an inspection "
               + "or a treatment sends, and its source.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "The catalogue")
   })
   public ResponseEntity<List<HealthIssueResponse>> getAllIssues() {
      List<HealthIssueResponse> issueResponses = healthIssueService.getAllIssues();
      return ResponseEntity.status(HttpStatus.OK).body(issueResponses);
   }
}
