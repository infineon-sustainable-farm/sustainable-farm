package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthInspectionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthInspectionResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFindingStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.InspectionMethod;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthInspectionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
@WebMvcTest(HealthInspectionController.class)
@Import(CoreExceptionHandler.class)
public class HealthInspectionControllerTest {

    private static final String URL = "/api/plants/health-inspections";
    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final LocalDate SEPTEMBER_1 = LocalDate.of(2026, 9, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthInspectionService healthInspectionService;

    private ResultActions postInspection(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Common part of every 4xx body: the application-wide ApiError envelope. */
    private static void assertApiError(ResultActions result, int status, String error, String message)
            throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("uri=" + URL))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /** A 400 of the validation, with exactly one field error. */
    private void assertOneFieldError(ResultActions result, String field, String message) throws Exception {
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors['" + field + "']").value(message));
        verify(healthInspectionService, never()).recordInspection(any());
    }

    private static HealthInspectionResponse blockC() {
        return new HealthInspectionResponse(1L, null, "C", SEPTEMBER_1, 48, HealthCategory.MODERATE, "Awa",
                InspectionMethod.VISUAL, "user_entry", NOW, List.of(
                new HealthFindingResponse(10L, 1L, null, "C", SEPTEMBER_1, "ANTHRACNOSE", "Anthracnose",
                        HealthIssueKind.DISEASE, null, "42", HealthFindingStatus.UNTREATED, null, null)));
    }

    @Test
    void recordInspection_shouldReturn201WithTheInspectionAndItsFindings() throws Exception {
        // Arrange
        when(healthInspectionService.recordInspection(any(HealthInspectionRequest.class))).thenReturn(blockC());
        // Act
        ResultActions result = postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48,"observer":"Awa","method":"visual",
                 "findings":[{"issueCode":"anthracnose","treeLabel":"42"}]}
                """);
        // Assert: no Location header, the inspection and its findings in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.blockCode").value("C"))
                .andExpect(jsonPath("$.inspectedOn").value("2026-09-01"))
                .andExpect(jsonPath("$.healthScorePct").value(48))
                .andExpect(jsonPath("$.healthCategory").value("MODERATE"))
                .andExpect(jsonPath("$.observer").value("Awa"))
                .andExpect(jsonPath("$.method").value("VISUAL"))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-28T10:00:00Z"))
                .andExpect(jsonPath("$.findings", hasSize(1)))
                .andExpect(jsonPath("$.findings[0].id").value(10))
                .andExpect(jsonPath("$.findings[0].inspectionId").value(1))
                .andExpect(jsonPath("$.findings[0].issueCode").value("ANTHRACNOSE"))
                .andExpect(jsonPath("$.findings[0].issueKind").value("DISEASE"))
                .andExpect(jsonPath("$.findings[0].treeLabel").value("42"))
                .andExpect(jsonPath("$.findings[0].status").value("UNTREATED"))
                .andExpect(jsonPath("$.findings[0].resolvedOn").value(nullValue()));
        // Assert: the values reach the service as sent; it normalizes them
        ArgumentCaptor<HealthInspectionRequest> captor = ArgumentCaptor.forClass(HealthInspectionRequest.class);
        verify(healthInspectionService).recordInspection(captor.capture());
        assertEquals(48.0, captor.getValue().healthScorePct());
        assertEquals("visual", captor.getValue().method());
        assertEquals("anthracnose", captor.getValue().findings().getFirst().issueCode());
    }

    @Test
    void recordInspection_shouldReturn400_whenTheScoreIsAbove100() throws Exception {
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":101,"findings":[]}
                """), "healthScorePct", "healthScorePct must be a whole number between 0 and 100");
    }

    @Test
    void recordInspection_shouldReturn400_whenTheScoreIsNotAWholeNumber() throws Exception {
        // 48.5 must be refused, not cut to 48
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48.5,"findings":[]}
                """), "healthScorePct", "healthScorePct must be a whole number between 0 and 100");
    }

    @Test
    void recordInspection_shouldReturn400_whenOtherHasNoLabel() throws Exception {
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48,
                 "findings":[{"issueCode":" other ","otherLabel":"  "}]}
                """), "findings[0].otherLabel", "otherLabel is required when issueCode is OTHER");
    }

    @Test
    void recordInspection_shouldReturn400_whenALabelIsGivenWithoutOther() throws Exception {
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48,
                 "findings":[{"issueCode":"ANTHRACNOSE","otherLabel":"Leaf curl"}]}
                """), "findings[0].otherLabel", "otherLabel must be left out unless issueCode is OTHER");
    }

    @Test
    void recordInspection_shouldReturn400WithOneMessage_whenTheLabelIsTooLong() throws Exception {
        String label = "x".repeat(61);
        // With OTHER: the length is the only fault
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48,
                 "findings":[{"issueCode":"OTHER","otherLabel":"%s"}]}
                """.formatted(label)), "findings[0].otherLabel", "otherLabel must be at most 60 characters");
        // Without OTHER: two faults on one field, always the same single message
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48,
                 "findings":[{"issueCode":"TERMITES","otherLabel":"%s"}]}
                """.formatted(label)), "findings[0].otherLabel",
                "otherLabel must be left out unless issueCode is OTHER");
    }

    @Test
    void recordInspection_shouldReturn400_withTheIndexOfTheFailingFinding() throws Exception {
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48,
                 "findings":[{"issueCode":"ANTHRACNOSE"},{"issueCode":"anthrac nose"}]}
                """), "findings[1].issueCode",
                "issueCode must be a code of letters, digits and underscores, such as ANTHRACNOSE");
    }

    @Test
    void recordInspection_shouldReturn400_whenFindingsIsMissingOrHoldsNull() throws Exception {
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48}
                """), "findings", "findings is required");
        assertOneFieldError(postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48,"findings":[null]}
                """), "findings[0]", "findings must not contain null");
    }

    @Test
    void recordInspection_shouldReturn400ListingEveryFailingField() throws Exception {
        // Act
        ResultActions result = postInspection("""
                {"farmId":0,"blockCode":"Block C","inspectedOn":"2999-01-01","healthScorePct":-1,
                 "observer":"%s","method":"drone",
                 "findings":[{"treeLabel":"tree 42"}]}
                """.formatted("x".repeat(256)));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(8)))
                .andExpect(jsonPath("$.fieldErrors.farmId").value("farmId must be at least 1"))
                .andExpect(jsonPath("$.fieldErrors.blockCode")
                        .value("blockCode must be a short code such as A or B2, without prefix or space"))
                .andExpect(jsonPath("$.fieldErrors.inspectedOn").value("inspectedOn must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.healthScorePct")
                        .value("healthScorePct must be a whole number between 0 and 100"))
                .andExpect(jsonPath("$.fieldErrors.observer").value("observer must be at most 255 characters"))
                .andExpect(jsonPath("$.fieldErrors.method").value("method must be VISUAL or TRAP"))
                .andExpect(jsonPath("$.fieldErrors['findings[0].issueCode']").value("issueCode is required"))
                .andExpect(jsonPath("$.fieldErrors['findings[0].treeLabel']")
                        .value("treeLabel must be up to 20 letters, digits or hyphens, such as 42 or R3-12"));
        verify(healthInspectionService, never()).recordInspection(any());
    }

    @Test
    void recordInspection_shouldReturn422_whenAnIssueCodeIsUnknown() throws Exception {
        // Arrange
        when(healthInspectionService.recordInspection(any(HealthInspectionRequest.class)))
                .thenThrow(new BusinessRuleException("No health issue with code POWDERY_MILDEW in the catalogue"));
        // Act
        ResultActions result = postInspection("""
                {"blockCode":"C","inspectedOn":"2026-09-01","healthScorePct":48,
                 "findings":[{"issueCode":"ANTHRACNOSE"},{"issueCode":"powdery_mildew"}]}
                """);
        // Assert
        assertApiError(result, 422, "Unprocessable Entity",
                "No health issue with code POWDERY_MILDEW in the catalogue");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void getAllInspections_shouldPassEveryFilter() throws Exception {
        // Arrange
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        when(healthInspectionService.getAllInspections(1, "C", from, to)).thenReturn(List.of(blockC()));
        // Act & Assert
        mockMvc.perform(get(URL).param("farmId", "1").param("blockCode", "C")
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].findings[0].issueCode").value("ANTHRACNOSE"));
        verify(healthInspectionService).getAllInspections(eq(1), eq("C"), eq(from), eq(to));
    }

    @Test
    void getAllInspections_shouldReturn400_whenADateIsNotIso() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("from", "01/09/2026"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'from'");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }
}
