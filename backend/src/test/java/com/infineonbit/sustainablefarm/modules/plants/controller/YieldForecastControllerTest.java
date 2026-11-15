package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.Entry;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.MonthlyTotal;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.VarietyWithoutReference;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldForecastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The error bodies are asserted in full, as for the planting route. */
@WebMvcTest(YieldForecastController.class)
@Import(CoreExceptionHandler.class)
public class YieldForecastControllerTest {

    private static final String URL = "/api/plants/yield-forecast";
    private static final YearMonth JANUARY_2027 = YearMonth.of(2027, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private YieldForecastService yieldForecastService;

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

    private static YieldForecastResponse forecast() {
        return new YieldForecastResponse(JANUARY_2027, 1, "recorded_plantings",
                List.of(new MonthlyTotal(JANUARY_2027, 0.0)),
                List.of(new Entry(YearMonth.of(2027, 5), null, "B", 1L, "Keitt", 150, 3, "gradual production",
                        220.0, 0.5, 0.3333, 5500.0, "Zalka_2025", "varietal_guide_west_africa",
                        "assumption_to_validate")),
                List.of(new VarietyWithoutReference(null, "A", "Palmer", 10)));
    }

    @Test
    void getYieldForecast_shouldReturnTheForecast_withMonthsAsYearMonthStrings() throws Exception {
        // Arrange
        when(yieldForecastService.getYieldForecast(1, "B", JANUARY_2027, 12)).thenReturn(forecast());
        // Act
        ResultActions result = mockMvc.perform(get(URL)
                .param("farmId", "1")
                .param("blockCode", "B")
                .param("from", "2027-01")
                .param("months", "12"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2027-01"))
                .andExpect(jsonPath("$.months").value(1))
                .andExpect(jsonPath("$.basis").value("recorded_plantings"))
                .andExpect(jsonPath("$.monthlyTotals", hasSize(1)))
                .andExpect(jsonPath("$.monthlyTotals[0].month").value("2027-01"))
                .andExpect(jsonPath("$.monthlyTotals[0].expectedKg").value(0.0))
                .andExpect(jsonPath("$.entries[0].month").value("2027-05"))
                .andExpect(jsonPath("$.entries[0].farmId").value(nullValue()))
                .andExpect(jsonPath("$.entries[0].blockCode").value("B"))
                .andExpect(jsonPath("$.entries[0].varietyId").value(1))
                .andExpect(jsonPath("$.entries[0].varietyName").value("Keitt"))
                .andExpect(jsonPath("$.entries[0].treeCount").value(150))
                .andExpect(jsonPath("$.entries[0].ageYears").value(3))
                .andExpect(jsonPath("$.entries[0].growthPhase").value("gradual production"))
                .andExpect(jsonPath("$.entries[0].yieldPerTreeKg").value(220.0))
                .andExpect(jsonPath("$.entries[0].phaseShare").value(0.5))
                .andExpect(jsonPath("$.entries[0].monthShare").value(0.3333))
                .andExpect(jsonPath("$.entries[0].expectedKg").value(5500.0))
                .andExpect(jsonPath("$.entries[0].yieldSource").value("Zalka_2025"))
                .andExpect(jsonPath("$.entries[0].seasonSource").value("varietal_guide_west_africa"))
                .andExpect(jsonPath("$.entries[0].phaseShareSource").value("assumption_to_validate"))
                .andExpect(jsonPath("$.varietiesWithoutReference[0].farmId").value(nullValue()))
                .andExpect(jsonPath("$.varietiesWithoutReference[0].blockCode").value("A"))
                .andExpect(jsonPath("$.varietiesWithoutReference[0].varietyName").value("Palmer"))
                .andExpect(jsonPath("$.varietiesWithoutReference[0].treeCount").value(10));
    }

    @Test
    void getYieldForecast_shouldPassNulls_whenNoParameterIsGiven() throws Exception {
        // Arrange: the service picks the current month and 6 months
        when(yieldForecastService.getYieldForecast(null, null, null, null)).thenReturn(forecast());
        // Act & Assert
        mockMvc.perform(get(URL)).andExpect(status().isOk());
        verify(yieldForecastService).getYieldForecast(null, null, null, null);
    }

    @Test
    void getYieldForecast_shouldReturn400WithApiError_whenMonthsIsZero() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("months", "0"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.months").value("months must be between 1 and 24"));
        verifyNoInteractions(yieldForecastService);
    }

    @Test
    void getYieldForecast_shouldReturn400WithApiError_whenMonthsIsAbove24() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("months", "25"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.months").value("months must be between 1 and 24"));
        verifyNoInteractions(yieldForecastService);
    }

    @Test
    void getYieldForecast_shouldAcceptTheBoundsOfMonths() throws Exception {
        // Arrange
        when(yieldForecastService.getYieldForecast(null, null, null, 1)).thenReturn(forecast());
        when(yieldForecastService.getYieldForecast(null, null, null, 24)).thenReturn(forecast());
        // Act & Assert
        mockMvc.perform(get(URL).param("months", "1")).andExpect(status().isOk());
        mockMvc.perform(get(URL).param("months", "24")).andExpect(status().isOk());
    }

    @Test
    void getYieldForecast_shouldReturn400_whenMonthsIsNotANumber() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("months", "abc"));
        // Assert: a 400 on months, whose message is the conversion text of Spring (a known limitation)
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.months").value(startsWith("Failed to convert")));
        verifyNoInteractions(yieldForecastService);
    }

    @Test
    void getYieldForecast_shouldReturn400WithApiError_whenFromIsNotAMonth() throws Exception {
        // Act & Assert: "abc", a month without its leading zero, and a full date are all refused
        for (String from : List.of("abc", "2027-1", "2027-01-01")) {
            ResultActions result = mockMvc.perform(get(URL).param("from", from));
            assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'from'");
            result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        }
        verifyNoInteractions(yieldForecastService);
    }
}
