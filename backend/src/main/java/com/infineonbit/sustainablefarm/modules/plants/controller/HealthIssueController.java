package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthIssueRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthIssueService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller of the catalogue of pests and diseases.
 * <p>
 * Available Endpoints :
 * <ul>
 * <li>{@code GET /api/plants/health-issues} — the catalogue</li>
 * <li>{@code POST /api/plants/health-issues} — add a pest or a disease</li>
 * </ul>
 * <p>
 * Outside the dev profile the catalogue starts empty: the user adds the pests
 * and diseases, and "Other" is added the first time an inspection or a
 * treatment uses it. An inspection or a treatment refers to an issue by its
 * {@code code}, which is derived from the name. Errors use the application-wide
 * {@code ApiError} body: 400 with {@code fieldErrors} for an invalid request,
 * 409 when the name or the code is already in the catalogue.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/plants/health-issues")
@AllArgsConstructor
@Tag(name = "Plants — Health issues", description = "Catalogue of the pests and diseases an inspection can report")
public class HealthIssueController {

   private final HealthIssueService healthIssueService;

   @PostMapping
   @Operation(summary = "Add a pest or a disease to the catalogue",
         description = "The code is derived from the name: accents removed, upper case, one underscore between "
               + "words, so \"Powdery mildew\" gives POWDERY_MILDEW. The name must not be in the catalogue yet, "
               + "ignoring case, accents and surrounding spaces, nor give a code already taken. kind is PEST or "
               + "DISEASE: \"Other\" is added by the service the first time an inspection or a treatment uses it. "
               + "source is optional, user_entry when omitted.")
   @ApiResponses({
         @ApiResponse(responseCode = "201", description = "The added issue"),
         @ApiResponse(responseCode = "400", description = "Invalid request; fieldErrors lists the failing fields"),
         @ApiResponse(responseCode = "409", description = "The name or the code is already in the catalogue")
   })
   public ResponseEntity<HealthIssueResponse> createIssue(@Valid @RequestBody HealthIssueRequest healthIssueRequest) {
      HealthIssueResponse issueResponse = healthIssueService.createIssue(healthIssueRequest);
      return ResponseEntity.status(HttpStatus.CREATED).body(issueResponse);
   }

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
