package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanCompletionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.FertilizerPlanRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerPlanStockCheckResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerPlanStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.exception.FertilizerPlanNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerPlanService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error bodies are asserted in full, as for the fertilizer routes.
 *
 * <p>{@code @DirtiesContext}: this slice is not kept in the test context
 * cache, so it does not push a shared database context out of it.
 */
@WebMvcTest(FertilizerPlanController.class)
@Import(CoreExceptionHandler.class)
@DirtiesContext
public class FertilizerPlanControllerTest {

    private static final String URL = "/api/plants/fertilizer-plans";
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FertilizerPlanService fertilizerPlanService;

    private ResultActions postPlan(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions postComplete(long id, String body) throws Exception {
        return mockMvc.perform(post(URL + "/" + id + "/complete").contentType(MediaType.APPLICATION_JSON).content(body));
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

    private static FertilizerPlanResponse plannedNpk() {
        return new FertilizerPlanResponse(5L, 1L, "NPK 15-15-15", FertilizerUnit.KG, null, "B",
                LocalDate.of(2026, 11, 24), 30.0, FertilizerPlanStatus.PLANNED, false, 10, 500L, 0.76,
                LocalDate.of(2026, 11, 3), 15000L, 22.87, null, null, null, "before the rains", "user_entry", NOW);
    }

    private static FertilizerPlanResponse doneNpk() {
        return new FertilizerPlanResponse(5L, 1L, "NPK 15-15-15", FertilizerUnit.KG, null, "B",
                LocalDate.of(2026, 11, 24), 30.0, FertilizerPlanStatus.DONE, false, null, 500L, 0.76,
                LocalDate.of(2026, 11, 3), 15000L, 22.87, 77L, LocalDate.of(2026, 11, 14), 25.5, null,
                "user_entry", NOW);
    }

    @Test
    void createPlan_shouldReturn201WithThePlan_whenRequestIsValid() throws Exception {
        // Arrange
        when(fertilizerPlanService.createPlan(any(FertilizerPlanRequest.class))).thenReturn(plannedNpk());
        // Act
        ResultActions result = postPlan("""
                {"fertilizerId":1,"blockCode":" b ","plannedDate":"%s","plannedQuantity":30,
                 "note":"before the rains"}
                """.formatted(TODAY.plusDays(10)));
        // Assert: no Location header, the plan in the body
        result.andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.fertilizerId").value(1))
                .andExpect(jsonPath("$.fertilizerName").value("NPK 15-15-15"))
                .andExpect(jsonPath("$.unit").value("KG"))
                .andExpect(jsonPath("$.farmId").value(nullValue()))
                .andExpect(jsonPath("$.blockCode").value("B"))
                .andExpect(jsonPath("$.plannedDate").value("2026-11-24"))
                .andExpect(jsonPath("$.plannedQuantity").value(30.0))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.daysFromToday").value(10))
                .andExpect(jsonPath("$.unitPriceXof").value(500))
                .andExpect(jsonPath("$.unitPriceEur").value(0.76))
                .andExpect(jsonPath("$.priceDate").value("2026-11-03"))
                .andExpect(jsonPath("$.plannedCostXof").value(15000))
                .andExpect(jsonPath("$.plannedCostEur").value(22.87))
                .andExpect(jsonPath("$.movementId").value(nullValue()))
                .andExpect(jsonPath("$.actualDate").value(nullValue()))
                .andExpect(jsonPath("$.actualQuantity").value(nullValue()))
                .andExpect(jsonPath("$.note").value("before the rains"))
                .andExpect(jsonPath("$.source").value("user_entry"));
        ArgumentCaptor<FertilizerPlanRequest> captor = ArgumentCaptor.forClass(FertilizerPlanRequest.class);
        verify(fertilizerPlanService).createPlan(captor.capture());
        assertEquals(new FertilizerPlanRequest(1L, null, " b ", TODAY.plusDays(10), 30.0, "before the rains"),
                captor.getValue());
    }

    @Test
    void createPlan_shouldAcceptToday_asThePlannedDate() throws Exception {
        // Arrange
        when(fertilizerPlanService.createPlan(any(FertilizerPlanRequest.class))).thenReturn(plannedNpk());
        // Act + Assert
        postPlan("""
                {"fertilizerId":1,"blockCode":"B","plannedDate":"%s","plannedQuantity":0.001}
                """.formatted(TODAY)).andExpect(status().isCreated());
    }

    @Test
    void createPlan_shouldReturn400WithRequiredFields_whenBodyIsEmpty() throws Exception {
        // Act
        ResultActions result = postPlan("{}");
        // Assert: the farm and the note are optional
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(4)))
                .andExpect(jsonPath("$.fieldErrors.fertilizerId").value("fertilizerId is required"))
                .andExpect(jsonPath("$.fieldErrors.blockCode").value("blockCode is required"))
                .andExpect(jsonPath("$.fieldErrors.plannedDate").value("plannedDate is required"))
                .andExpect(jsonPath("$.fieldErrors.plannedQuantity").value("plannedQuantity is required"));
        verify(fertilizerPlanService, never()).createPlan(any());
    }

    @Test
    void createPlan_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Act: a past date, a quantity of 0, a block with a prefix, a farm of 0 and a long note
        ResultActions result = postPlan("""
                {"fertilizerId":1,"farmId":0,"blockCode":"Block B","plannedDate":"%s","plannedQuantity":0,
                 "note":"%s"}
                """.formatted(TODAY.minusDays(1), "n".repeat(256)));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(5)))
                .andExpect(jsonPath("$.fieldErrors.farmId").value("farmId must be at least 1"))
                .andExpect(jsonPath("$.fieldErrors.blockCode")
                        .value("blockCode must be a short code such as A or B2, without prefix or space"))
                .andExpect(jsonPath("$.fieldErrors.plannedDate").value("plannedDate must be today or in the future"))
                .andExpect(jsonPath("$.fieldErrors.plannedQuantity")
                        .value("plannedQuantity must be greater than 0, with at most 3 decimals"))
                .andExpect(jsonPath("$.fieldErrors.note").value("note must be at most 255 characters"));
        verify(fertilizerPlanService, never()).createPlan(any());
    }

    @Test
    void createPlan_shouldReturn400_whenQuantityHasMoreThanThreeDecimals() throws Exception {
        // Act
        ResultActions result = postPlan("""
                {"fertilizerId":1,"blockCode":"B","plannedDate":"%s","plannedQuantity":1.2345}
                """.formatted(TODAY));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed", URL);
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.plannedQuantity")
                        .value("plannedQuantity must be greater than 0, with at most 3 decimals"));
    }

    @Test
    void createPlan_shouldReturn404_whenTheFertilizerIsUnknown() throws Exception {
        // Arrange
        when(fertilizerPlanService.createPlan(any(FertilizerPlanRequest.class)))
                .thenThrow(new FertilizerNotFoundException(9L));
        // Act
        ResultActions result = postPlan("""
                {"fertilizerId":9,"blockCode":"B","plannedDate":"%s","plannedQuantity":30}
                """.formatted(TODAY));
        // Assert
        assertApiError(result, 404, "Not Found", "Fertilizer with ID 9 not found", URL);
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void getAllPlans_shouldReturn200WithThePlans_andPassEveryFilter() throws Exception {
        // Arrange
        when(fertilizerPlanService.getAllPlans(1L, FertilizerPlanStatus.PLANNED, 2, "B",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(List.of(plannedNpk()));
        // Act
        ResultActions result = mockMvc.perform(get(URL)
                .param("fertilizerId", "1").param("status", "PLANNED").param("farmId", "2")
                .param("blockCode", "B").param("from", "2026-11-01").param("to", "2026-11-30"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    void getAllPlans_shouldReturn200WithAnEmptyList_whenNoFilterIsGiven() throws Exception {
        // Arrange
        when(fertilizerPlanService.getAllPlans(null, null, null, null, null, null)).thenReturn(List.of());
        // Act + Assert
        mockMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllPlans_shouldReturn400_whenTheStatusIsUnknown() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("status", "LATE"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'status'", URL);
        verifyNoInteractions(fertilizerPlanService);
    }

    @Test
    void getAllPlans_shouldReturn400_whenADateIsMalformed() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("from", "24/11/2026"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'from'", URL);
        verifyNoInteractions(fertilizerPlanService);
    }

    @Test
    void completePlan_shouldReturn200WithTheDonePlan_whenRequestIsValid() throws Exception {
        // Arrange
        when(fertilizerPlanService.completePlan(eq(5L), any(FertilizerPlanCompletionRequest.class)))
                .thenReturn(doneNpk());
        // Act
        ResultActions result = postComplete(5L, """
                {"applicationDate":"%s","quantity":25.5,"applicator":"Team A","method":"around the tree base"}
                """.formatted(TODAY));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.daysFromToday").value(nullValue()))
                .andExpect(jsonPath("$.movementId").value(77))
                .andExpect(jsonPath("$.actualDate").value("2026-11-14"))
                .andExpect(jsonPath("$.actualQuantity").value(25.5));
        ArgumentCaptor<FertilizerPlanCompletionRequest> captor =
                ArgumentCaptor.forClass(FertilizerPlanCompletionRequest.class);
        verify(fertilizerPlanService).completePlan(eq(5L), captor.capture());
        assertEquals(new FertilizerPlanCompletionRequest(TODAY, 25.5, "Team A", "around the tree base"),
                captor.getValue());
    }

    @Test
    void completePlan_shouldReturn400WithRequiredFields_whenBodyIsEmpty() throws Exception {
        // Act
        ResultActions result = postComplete(5L, "{}");
        // Assert: the method is optional
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/5/complete");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(3)))
                .andExpect(jsonPath("$.fieldErrors.applicationDate").value("applicationDate is required"))
                .andExpect(jsonPath("$.fieldErrors.quantity").value("quantity is required"))
                .andExpect(jsonPath("$.fieldErrors.applicator").value("applicator is required"));
        verify(fertilizerPlanService, never()).completePlan(any(), any());
    }

    @Test
    void completePlan_shouldReturn400WithEveryFailingField_whenValuesAreInvalid() throws Exception {
        // Act: a date in the future, a negative quantity, a blank applicator and a long method
        ResultActions result = postComplete(5L, """
                {"applicationDate":"%s","quantity":-1,"applicator":"  ","method":"%s"}
                """.formatted(TODAY.plusDays(1), "m".repeat(256)));
        // Assert: the same messages as an application
        assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/5/complete");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(4)))
                .andExpect(jsonPath("$.fieldErrors.applicationDate")
                        .value("applicationDate must be today or in the past"))
                .andExpect(jsonPath("$.fieldErrors.quantity")
                        .value("quantity must be greater than 0, with at most 3 decimals"))
                .andExpect(jsonPath("$.fieldErrors.applicator").value("applicator is required"))
                .andExpect(jsonPath("$.fieldErrors.method").value("method must be at most 255 characters"));
    }

    @Test
    void completePlan_shouldReturn404_whenThePlanIsUnknown() throws Exception {
        // Arrange
        when(fertilizerPlanService.completePlan(eq(9L), any(FertilizerPlanCompletionRequest.class)))
                .thenThrow(new FertilizerPlanNotFoundException(9L));
        // Act
        ResultActions result = postComplete(9L, """
                {"applicationDate":"%s","quantity":25.5,"applicator":"Team A"}
                """.formatted(TODAY));
        // Assert
        assertApiError(result, 404, "Not Found", "Planned application with ID 9 not found", URL + "/9/complete");
    }

    @Test
    void completePlan_shouldReturn409_whenThePlanIsAlreadyDone() throws Exception {
        // Arrange
        String message = "Planned application with ID 5 is already done: "
                + "only a planned application can be marked as done or cancelled";
        when(fertilizerPlanService.completePlan(eq(5L), any(FertilizerPlanCompletionRequest.class)))
                .thenThrow(new ConflictException(message));
        // Act
        ResultActions result = postComplete(5L, """
                {"applicationDate":"%s","quantity":25.5,"applicator":"Team A"}
                """.formatted(TODAY));
        // Assert
        assertApiError(result, 409, "Conflict", message, URL + "/5/complete");
    }

    @Test
    void completePlan_shouldReturn422_whenTheQuantityExceedsTheStock() throws Exception {
        // Arrange
        String message = "Not enough stock of NPK 15-15-15: 20 kg left, 30 kg requested";
        when(fertilizerPlanService.completePlan(eq(5L), any(FertilizerPlanCompletionRequest.class)))
                .thenThrow(new BusinessRuleException(message));
        // Act
        ResultActions result = postComplete(5L, """
                {"applicationDate":"%s","quantity":30,"applicator":"Team A"}
                """.formatted(TODAY));
        // Assert
        assertApiError(result, 422, "Unprocessable Entity", message, URL + "/5/complete");
    }

    @Test
    void cancelPlan_shouldReturn200WithTheCancelledPlan() throws Exception {
        // Arrange
        when(fertilizerPlanService.cancelPlan(5L)).thenReturn(new FertilizerPlanResponse(5L, 1L, "NPK 15-15-15",
                FertilizerUnit.KG, null, "B", LocalDate.of(2026, 11, 24), 30.0, FertilizerPlanStatus.CANCELLED,
                false, null, null, null, null, null, null, null, null, null, null, "user_entry", NOW));
        // Act: no body is needed
        ResultActions result = mockMvc.perform(post(URL + "/5/cancel"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.plannedCostXof").value(nullValue()));
    }

    @Test
    void cancelPlan_shouldReturn409_whenThePlanIsAlreadyCancelled() throws Exception {
        // Arrange
        String message = "Planned application with ID 5 is already cancelled: "
                + "only a planned application can be marked as done or cancelled";
        when(fertilizerPlanService.cancelPlan(5L)).thenThrow(new ConflictException(message));
        // Act
        ResultActions result = mockMvc.perform(post(URL + "/5/cancel"));
        // Assert
        assertApiError(result, 409, "Conflict", message, URL + "/5/cancel");
    }

    @Test
    void cancelPlan_shouldReturn404_whenThePlanIsUnknown() throws Exception {
        // Arrange
        when(fertilizerPlanService.cancelPlan(9L)).thenThrow(new FertilizerPlanNotFoundException(9L));
        // Act
        ResultActions result = mockMvc.perform(post(URL + "/9/cancel"));
        // Assert
        assertApiError(result, 404, "Not Found", "Planned application with ID 9 not found", URL + "/9/cancel");
    }

    @Test
    void getComparison_shouldReturn200WithThePeriodAndItsLines() throws Exception {
        // Arrange
        when(fertilizerPlanService.getComparison(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).thenReturn(
                new FertilizerPlanComparisonResponse(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), List.of(
                        new FertilizerPlanComparisonResponse.Row("A", 1L, "NPK 15-15-15", FertilizerUnit.KG,
                                60.0, 20.0, 40.0, 5.0, -20.0, -33.3, 30000L, 45.73, 20000L, 30.49,
                                LocalDate.of(2026, 11, 3)))));
        // Act
        ResultActions result = mockMvc.perform(get(URL + "/comparison")
                .param("from", "2026-01-01").param("to", "2026-12-31"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2026-12-31"))
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].blockCode").value("A"))
                .andExpect(jsonPath("$.rows[0].fertilizerId").value(1))
                .andExpect(jsonPath("$.rows[0].fertilizerName").value("NPK 15-15-15"))
                .andExpect(jsonPath("$.rows[0].unit").value("KG"))
                .andExpect(jsonPath("$.rows[0].plannedQuantity").value(60.0))
                .andExpect(jsonPath("$.rows[0].remainingQuantity").value(20.0))
                .andExpect(jsonPath("$.rows[0].appliedQuantity").value(40.0))
                .andExpect(jsonPath("$.rows[0].unplannedQuantity").value(5.0))
                .andExpect(jsonPath("$.rows[0].gapQuantity").value(-20.0))
                .andExpect(jsonPath("$.rows[0].gapPercent").value(-33.3))
                .andExpect(jsonPath("$.rows[0].plannedCostXof").value(30000))
                .andExpect(jsonPath("$.rows[0].plannedCostEur").value(45.73))
                .andExpect(jsonPath("$.rows[0].appliedCostXof").value(20000))
                .andExpect(jsonPath("$.rows[0].appliedCostEur").value(30.49))
                .andExpect(jsonPath("$.rows[0].priceDate").value("2026-11-03"));
    }

    @Test
    void getComparison_shouldReturn400_whenADateIsMalformed() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL + "/comparison").param("to", "2026-13-01"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'to'", URL + "/comparison");
        verifyNoInteractions(fertilizerPlanService);
    }

    @Test
    void getStockCheck_shouldReturn200WithTheWarnings_andPassNoDays_whenOmitted() throws Exception {
        // Arrange
        when(fertilizerPlanService.getStockCheck(null)).thenReturn(new FertilizerPlanStockCheckResponse(30,
                LocalDate.of(2026, 11, 14), LocalDate.of(2026, 12, 14), List.of(
                new FertilizerPlanStockCheckResponse.Warning(1L, "NPK 15-15-15", FertilizerUnit.KG,
                        70.0, 30.0, 50.0, 20.0))));
        // Act
        ResultActions result = mockMvc.perform(get(URL + "/stock-check"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(30))
                .andExpect(jsonPath("$.today").value("2026-11-14"))
                .andExpect(jsonPath("$.until").value("2026-12-14"))
                .andExpect(jsonPath("$.warnings", hasSize(1)))
                .andExpect(jsonPath("$.warnings[0].fertilizerId").value(1))
                .andExpect(jsonPath("$.warnings[0].fertilizerName").value("NPK 15-15-15"))
                .andExpect(jsonPath("$.warnings[0].unit").value("KG"))
                .andExpect(jsonPath("$.warnings[0].plannedQuantity").value(70.0))
                .andExpect(jsonPath("$.warnings[0].overdueQuantity").value(30.0))
                .andExpect(jsonPath("$.warnings[0].currentStock").value(50.0))
                .andExpect(jsonPath("$.warnings[0].shortfall").value(20.0));
    }

    @Test
    void getStockCheck_shouldPassTheDays_atBothBounds() throws Exception {
        // Arrange
        when(fertilizerPlanService.getStockCheck(any())).thenReturn(new FertilizerPlanStockCheckResponse(1,
                LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15), List.of()));
        // Act + Assert
        mockMvc.perform(get(URL + "/stock-check").param("days", "1")).andExpect(status().isOk());
        mockMvc.perform(get(URL + "/stock-check").param("days", "365")).andExpect(status().isOk());
        verify(fertilizerPlanService).getStockCheck(1);
        verify(fertilizerPlanService).getStockCheck(365);
    }

    @Test
    void getStockCheck_shouldReturn400_whenDaysIsOutOfRange() throws Exception {
        // Act
        ResultActions tooFew = mockMvc.perform(get(URL + "/stock-check").param("days", "0"));
        ResultActions tooMany = mockMvc.perform(get(URL + "/stock-check").param("days", "366"));
        // Assert: a field error, not a 500
        for (ResultActions result : List.of(tooFew, tooMany)) {
            assertApiError(result, 400, "Bad Request", "Validation failed", URL + "/stock-check");
            result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                    .andExpect(jsonPath("$.fieldErrors.days").value("days must be between 1 and 365"));
        }
        verifyNoInteractions(fertilizerPlanService);
    }
}
