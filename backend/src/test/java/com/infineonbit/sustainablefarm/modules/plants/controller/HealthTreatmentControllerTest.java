package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PreventiveTreatmentRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthTreatmentResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthTreatmentService;
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
@WebMvcTest(HealthTreatmentController.class)
@Import(CoreExceptionHandler.class)
public class HealthTreatmentControllerTest {

    private static final String URL = "/api/plants/treatments";
    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthTreatmentService healthTreatmentService;

    private ResultActions postTreatment(String body) throws Exception {
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
        verify(healthTreatmentService, never()).recordPreventiveTreatment(any());
    }

    /** A valid preventive treatment of block D, with the given target, label, quantity and interval. */
    private static String treatment(String target, String label, String quantity, String interval) {
        return """
                {"blockCode":"D","targetIssueCode":"%s",%s"treatedOn":"2026-09-05","productName":"Protein bait B",
                 "activeIngredient":"spinosad","quantity":%s,"unit":"L","preHarvestIntervalDays":%s,
                 "applicator":"Issa"}
                """.formatted(target, label == null ? "" : "\"targetOtherLabel\":\"" + label + "\",", quantity,
                interval);
    }

    private static HealthTreatmentResponse bait() {
        return new HealthTreatmentResponse(21L, null, "D", "FRUIT_FLY", "Fruit flies", null, null,
                LocalDate.of(2026, 9, 5), "Protein bait B", "spinosad", 1.0, TreatmentUnit.L, 1,
                LocalDate.of(2026, 9, 6), "Issa", null, "user_entry", NOW);
    }

    @Test
    void recordPreventiveTreatment_shouldReturn201WithTheTreatment() throws Exception {
        // Arrange
        when(healthTreatmentService.recordPreventiveTreatment(any(PreventiveTreatmentRequest.class)))
                .thenReturn(bait());
        // Act
        ResultActions result = postTreatment(treatment("fruit_fly", null, "1.0", "1"));
        // Assert: no Location header, the treatment in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.blockCode").value("D"))
                .andExpect(jsonPath("$.targetIssueCode").value("FRUIT_FLY"))
                .andExpect(jsonPath("$.targetIssueName").value("Fruit flies"))
                .andExpect(jsonPath("$.targetOtherLabel").value(nullValue()))
                .andExpect(jsonPath("$.findingId").value(nullValue()))
                .andExpect(jsonPath("$.treatedOn").value("2026-09-05"))
                .andExpect(jsonPath("$.productName").value("Protein bait B"))
                .andExpect(jsonPath("$.activeIngredient").value("spinosad"))
                .andExpect(jsonPath("$.quantity").value(1.0))
                .andExpect(jsonPath("$.unit").value("L"))
                .andExpect(jsonPath("$.preHarvestIntervalDays").value(1))
                .andExpect(jsonPath("$.harvestAllowedFrom").value("2026-09-06"))
                .andExpect(jsonPath("$.applicator").value("Issa"))
                .andExpect(jsonPath("$.equipment").value(nullValue()))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-09-28T10:00:00Z"));
        // Assert: the values reach the service as sent; it normalizes them
        ArgumentCaptor<PreventiveTreatmentRequest> captor = ArgumentCaptor.forClass(PreventiveTreatmentRequest.class);
        verify(healthTreatmentService).recordPreventiveTreatment(captor.capture());
        assertEquals("fruit_fly", captor.getValue().targetIssueCode());
        assertEquals(1.0, captor.getValue().preHarvestIntervalDays());
    }

    @Test
    void recordPreventiveTreatment_shouldReturn400ListingEveryFailingField() throws Exception {
        // Act
        ResultActions result = postTreatment("""
                {"farmId":0,"blockCode":"Block D","treatedOn":"2999-01-01","productName":"",
                 "quantity":0,"unit":"tons","preHarvestIntervalDays":-1,"applicator":" ","equipment":"%s"}
                """.formatted("x".repeat(256)));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(11)))
                .andExpect(jsonPath("$.fieldErrors.farmId").value("farmId must be at least 1"))
                .andExpect(jsonPath("$.fieldErrors.blockCode")
                        .value("blockCode must be a short code such as A or B2, without prefix or space"))
                .andExpect(jsonPath("$.fieldErrors.targetIssueCode").value("targetIssueCode is required"))
                .andExpect(jsonPath("$.fieldErrors.treatedOn").value("treatedOn must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.productName").value("productName is required"))
                .andExpect(jsonPath("$.fieldErrors.activeIngredient").value("activeIngredient is required"))
                .andExpect(jsonPath("$.fieldErrors.quantity")
                        .value("quantity must be greater than 0, with at most 3 decimals"))
                .andExpect(jsonPath("$.fieldErrors.unit").value("unit must be KG or L"))
                .andExpect(jsonPath("$.fieldErrors.preHarvestIntervalDays")
                        .value("preHarvestIntervalDays must be a whole number, 0 or more"))
                .andExpect(jsonPath("$.fieldErrors.applicator").value("applicator is required"))
                .andExpect(jsonPath("$.fieldErrors.equipment").value("equipment must be at most 255 characters"));
        verify(healthTreatmentService, never()).recordPreventiveTreatment(any());
    }

    @Test
    void recordPreventiveTreatment_shouldReturn400_whenTheQuantityHasMoreThan3Decimals() throws Exception {
        assertOneFieldError(postTreatment(treatment("FRUIT_FLY", null, "1.2345", "1")),
                "quantity", "quantity must be greater than 0, with at most 3 decimals");
    }

    @Test
    void recordPreventiveTreatment_shouldReturn400_whenThePreHarvestIntervalIsNotAWholeNumber() throws Exception {
        // 1.5 must be refused, not cut to 1
        assertOneFieldError(postTreatment(treatment("FRUIT_FLY", null, "1.0", "1.5")),
                "preHarvestIntervalDays", "preHarvestIntervalDays must be a whole number, 0 or more");
    }

    @Test
    void recordPreventiveTreatment_shouldReturn400_whenOtherHasNoLabel() throws Exception {
        assertOneFieldError(postTreatment(treatment("other", null, "3", "0")),
                "targetOtherLabel", "targetOtherLabel is required when targetIssueCode is OTHER");
    }

    @Test
    void recordPreventiveTreatment_shouldReturn400_whenALabelIsGivenWithoutOther() throws Exception {
        assertOneFieldError(postTreatment(treatment("FRUIT_FLY", "Powdery mildew", "1.0", "1")),
                "targetOtherLabel", "targetOtherLabel must be left out unless targetIssueCode is OTHER");
    }

    @Test
    void recordPreventiveTreatment_shouldReturn422_whenTheTargetIsUnknown() throws Exception {
        // Arrange
        when(healthTreatmentService.recordPreventiveTreatment(any(PreventiveTreatmentRequest.class)))
                .thenThrow(new BusinessRuleException("No health issue with code POWDERY_MILDEW in the catalogue"));
        // Act
        ResultActions result = postTreatment(treatment("powdery_mildew", null, "3", "0"));
        // Assert
        assertApiError(result, 422, "Unprocessable Entity",
                "No health issue with code POWDERY_MILDEW in the catalogue");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void getAllTreatments_shouldPassEveryFilter() throws Exception {
        // Arrange
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        when(healthTreatmentService.getAllTreatments(1, "D", from, to)).thenReturn(List.of(bait()));
        // Act & Assert
        mockMvc.perform(get(URL).param("farmId", "1").param("blockCode", "D")
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetIssueCode").value("FRUIT_FLY"));
        verify(healthTreatmentService).getAllTreatments(eq(1), eq("D"), eq(from), eq(to));
    }
}
