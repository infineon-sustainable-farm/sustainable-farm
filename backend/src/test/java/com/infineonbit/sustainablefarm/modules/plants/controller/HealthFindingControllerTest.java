package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FindingResolutionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FindingTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.HealthFindingNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthFindingService;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthTreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The error bodies are asserted in full, as for the planting and harvest routes. */
@WebMvcTest(HealthFindingController.class)
@Import(CoreExceptionHandler.class)
public class HealthFindingControllerTest {

    private static final String URL = "/api/plants/health-findings";
    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final String COPPER = """
            {"treatedOn":"2026-09-04","productName":"Copper fungicide A","activeIngredient":"copper hydroxide",
             "quantity":2.5,"unit":"KG","preHarvestIntervalDays":14,"applicator":"Awa","equipment":"knapsack sprayer"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthFindingService healthFindingService;

    @MockitoBean
    private HealthTreatmentService healthTreatmentService;

    private ResultActions postTreatment(long findingId, String body) throws Exception {
        return mockMvc.perform(post(URL + "/" + findingId + "/treatments")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Common part of every 4xx body: the application-wide ApiError envelope. */
    private static void assertApiError(ResultActions result, int status, String error, String message, String path)
            throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("uri=" + path))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private static HealthTreatmentResponse copper() {
        return new HealthTreatmentResponse(20L, null, "C", "ANTHRACNOSE", "Anthracnose", null, 1L,
                LocalDate.of(2026, 9, 4), "Copper fungicide A", "copper hydroxide", 2.5, TreatmentUnit.KG, 14,
                LocalDate.of(2026, 9, 18), "Awa", "knapsack sprayer", "user_entry", NOW);
    }

    @Test
    void recordTreatment_shouldReturn201WithTheTreatment() throws Exception {
        // Arrange
        when(healthTreatmentService.recordFindingTreatment(eq(1L), any(FindingTreatmentRequest.class)))
                .thenReturn(copper());
        // Act
        ResultActions result = postTreatment(1L, COPPER);
        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.findingId").value(1))
                .andExpect(jsonPath("$.blockCode").value("C"))
                .andExpect(jsonPath("$.targetIssueCode").value("ANTHRACNOSE"))
                .andExpect(jsonPath("$.quantity").value(2.5))
                .andExpect(jsonPath("$.unit").value("KG"))
                .andExpect(jsonPath("$.preHarvestIntervalDays").value(14))
                .andExpect(jsonPath("$.harvestAllowedFrom").value("2026-09-18"))
                .andExpect(jsonPath("$.equipment").value("knapsack sprayer"));
    }

    @Test
    void recordTreatment_shouldReturn400ListingEveryFailingField() throws Exception {
        // Act
        ResultActions result = postTreatment(1L, """
                {"activeIngredient":"%s","quantity":2.5555,"unit":"g","preHarvestIntervalDays":1.5,
                 "equipment":"%s"}
                """.formatted("x".repeat(256), "x".repeat(256)));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/1/treatments");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(8)))
                .andExpect(jsonPath("$.fieldErrors.treatedOn").value("treatedOn is required"))
                .andExpect(jsonPath("$.fieldErrors.productName").value("productName is required"))
                .andExpect(jsonPath("$.fieldErrors.activeIngredient")
                        .value("activeIngredient must be at most 255 characters"))
                .andExpect(jsonPath("$.fieldErrors.quantity")
                        .value("quantity must be greater than 0, with at most 3 decimals"))
                .andExpect(jsonPath("$.fieldErrors.unit").value("unit must be KG or L"))
                .andExpect(jsonPath("$.fieldErrors.preHarvestIntervalDays")
                        .value("preHarvestIntervalDays must be a whole number, 0 or more"))
                .andExpect(jsonPath("$.fieldErrors.applicator").value("applicator is required"))
                .andExpect(jsonPath("$.fieldErrors.equipment").value("equipment must be at most 255 characters"));
        verify(healthTreatmentService, never()).recordFindingTreatment(any(), any());
    }

    @Test
    void recordTreatment_shouldReturn404_whenTheFindingIsUnknown() throws Exception {
        // Arrange
        when(healthTreatmentService.recordFindingTreatment(eq(999L), any(FindingTreatmentRequest.class)))
                .thenThrow(new HealthFindingNotFoundException(999L));
        // Act
        ResultActions result = postTreatment(999L, COPPER);
        // Assert
        assertApiError(result, 404, "Not Found", "Health finding with ID 999 not found", URL + "/999/treatments");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void recordTreatment_shouldReturn422_whenDatedBeforeTheInspectionOrTheFindingIsResolved() throws Exception {
        // Before the inspection
        when(healthTreatmentService.recordFindingTreatment(eq(1L), any(FindingTreatmentRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "The treatment date 2026-08-30 is before the inspection date 2026-09-01 of finding 1"));
        assertApiError(postTreatment(1L, COPPER), 422, "Unprocessable Entity",
                "The treatment date 2026-08-30 is before the inspection date 2026-09-01 of finding 1",
                URL + "/1/treatments");
        // On a resolved finding
        when(healthTreatmentService.recordFindingTreatment(eq(2L), any(FindingTreatmentRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "Finding 2 was resolved on 2026-09-20 and cannot be treated any more"));
        assertApiError(postTreatment(2L, COPPER), 422, "Unprocessable Entity",
                "Finding 2 was resolved on 2026-09-20 and cannot be treated any more", URL + "/2/treatments");
    }

    @Test
    void getAllFindings_shouldPassEveryFilter() throws Exception {
        // Arrange
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        when(healthFindingService.getAllFindings(1, "C", HealthFindingStatus.IN_PROGRESS, from, to))
                .thenReturn(List.of(new HealthFindingResponse(1L, 1L, 1, "C", from, "ANTHRACNOSE", "Anthracnose",
                        HealthIssueKind.DISEASE, null, "42", HealthFindingStatus.IN_PROGRESS, 1,
                        LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 18), null, null)));
        // Act & Assert
        mockMvc.perform(get(URL).param("farmId", "1").param("blockCode", "C").param("status", "IN_PROGRESS")
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].treatmentCount").value(1))
                .andExpect(jsonPath("$[0].lastTreatedOn").value("2026-09-04"))
                .andExpect(jsonPath("$[0].harvestAllowedFrom").value("2026-09-18"))
                .andExpect(jsonPath("$[0].resolvedOn").value(nullValue()));
        verify(healthFindingService).getAllFindings(eq(1), eq("C"), eq(HealthFindingStatus.IN_PROGRESS),
                eq(from), eq(to));
    }

    @Test
    void getAllFindings_shouldReturn400_whenTheStatusIsUnknown() throws Exception {
        // Act: the status is an API code, in upper case
        ResultActions result = mockMvc.perform(get(URL).param("status", "untreated"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'status'", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    private ResultActions postResolution(long findingId, String body) throws Exception {
        return mockMvc.perform(post(URL + "/" + findingId + "/resolution")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void resolveFinding_shouldReturn201WithTheFinding() throws Exception {
        // Arrange
        LocalDate september20 = LocalDate.of(2026, 9, 20);
        when(healthFindingService.resolveFinding(eq(1L), any(FindingResolutionRequest.class)))
                .thenReturn(new HealthFindingResponse(1L, 1L, null, "C", LocalDate.of(2026, 9, 1), "ANTHRACNOSE",
                        "Anthracnose", HealthIssueKind.DISEASE, null, "42", HealthFindingStatus.TREATED, 1,
                        LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 18), september20, "No new lesions"));
        // Act
        ResultActions result = postResolution(1L, """
                {"resolvedOn":"2026-09-20","note":"No new lesions"}
                """);
        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("TREATED"))
                .andExpect(jsonPath("$.resolvedOn").value("2026-09-20"))
                .andExpect(jsonPath("$.resolutionNote").value("No new lesions"));
    }

    @Test
    void resolveFinding_shouldReturn409_whenAlreadyResolved() throws Exception {
        // Arrange
        when(healthFindingService.resolveFinding(eq(1L), any(FindingResolutionRequest.class)))
                .thenThrow(new ConflictException("Finding 1 was already resolved on 2026-09-20"));
        // Act
        ResultActions result = postResolution(1L, """
                {"resolvedOn":"2026-09-20"}
                """);
        // Assert
        assertApiError(result, 409, "Conflict", "Finding 1 was already resolved on 2026-09-20",
                URL + "/1/resolution");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void resolveFinding_shouldReturn422_whenDatedBeforeTheLastTreatment() throws Exception {
        // Arrange
        when(healthFindingService.resolveFinding(eq(1L), any(FindingResolutionRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "The resolution date 2026-09-03 is before the last treatment date 2026-09-04 of finding 1"));
        // Act
        ResultActions result = postResolution(1L, """
                {"resolvedOn":"2026-09-03"}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity",
                "The resolution date 2026-09-03 is before the last treatment date 2026-09-04 of finding 1",
                URL + "/1/resolution");
    }

    @Test
    void resolveFinding_shouldReturn404_whenTheFindingIsUnknown() throws Exception {
        // Arrange
        when(healthFindingService.resolveFinding(eq(999L), any(FindingResolutionRequest.class)))
                .thenThrow(new HealthFindingNotFoundException(999L));
        // Act
        ResultActions result = postResolution(999L, """
                {"resolvedOn":"2026-09-20"}
                """);
        // Assert
        assertApiError(result, 404, "Not Found", "Health finding with ID 999 not found", URL + "/999/resolution");
    }

    @Test
    void resolveFinding_shouldReturn400_whenTheDateIsMissingOrInTheFuture() throws Exception {
        // Missing date, note too long
        ResultActions missing = postResolution(1L, """
                {"note":"%s"}
                """.formatted("x".repeat(256)));
        assertApiError(missing, 400, "Bad Request", "Validation failed", URL + "/1/resolution");
        missing.andExpect(jsonPath("$.fieldErrors", aMapWithSize(2)))
                .andExpect(jsonPath("$.fieldErrors.resolvedOn").value("resolvedOn is required"))
                .andExpect(jsonPath("$.fieldErrors.note").value("note must be at most 255 characters"));
        // Date in the future
        ResultActions future = postResolution(1L, """
                {"resolvedOn":"2999-01-01"}
                """);
        assertApiError(future, 400, "Bad Request", "Validation failed", URL + "/1/resolution");
        future.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.resolvedOn").value("resolvedOn must be today or in the past"));
        verify(healthFindingService, never()).resolveFinding(any(), any());
    }
}
