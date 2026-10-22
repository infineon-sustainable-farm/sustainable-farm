package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.BlockComparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.Comparison;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.NotCompared;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.PhaseShare;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldComparisonResponse.VarietyComparison;
import com.infineonbit.sustainablefarm.modules.plants.service.YieldComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error bodies are asserted in full, as for the yield forecast.
 *
 * <p>The context is closed after the class, as for the growth phase shares:
 * one more cached context would evict a {@code @SpringBootTest} context, whose
 * create-drop then empties the database that the other integration tests
 * still use.
 */
@DirtiesContext
@WebMvcTest(YieldComparisonController.class)
@Import(CoreExceptionHandler.class)
public class YieldComparisonControllerTest {

    private static final String URL = "/api/plants/yield-comparison";
    private static final YearMonth JANUARY_2027 = YearMonth.of(2027, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private YieldComparisonService yieldComparisonService;

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

    private static YieldComparisonResponse comparison() {
        return new YieldComparisonResponse(JANUARY_2027, 12, "recorded_plantings",
                List.of(new VarietyComparison(7, "A", 1L, "Keitt", 40, 2199.9, 550.5, 2, 25.0,
                        220.0, "Zalka_2025", "varietal_guide_west_africa",
                        List.of(new PhaseShare("gradual production", 0.25, "Bally_2002")))),
                List.of(new BlockComparison(7, "A", 2199.9, 550.5, 25.0)),
                new Comparison(2199.9, 550.5, 25.0),
                List.of(new NotCompared(7, "C", 3L, "Brooks", 30, 120.0, 1, "NO_REFERENCE")));
    }

    @Test
    void getYieldComparison_shouldReturnTheComparison_withMonthsAsYearMonthStrings() throws Exception {
        // Arrange
        when(yieldComparisonService.getYieldComparison(7, "A", JANUARY_2027, 12)).thenReturn(comparison());
        // Act
        ResultActions result = mockMvc.perform(get(URL)
                .param("farmId", "7")
                .param("blockCode", "A")
                .param("from", "2027-01")
                .param("months", "12"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2027-01"))
                .andExpect(jsonPath("$.months").value(12))
                .andExpect(jsonPath("$.basis").value("recorded_plantings"))
                .andExpect(jsonPath("$.varieties", hasSize(1)))
                .andExpect(jsonPath("$.varieties[0].farmId").value(7))
                .andExpect(jsonPath("$.varieties[0].blockCode").value("A"))
                .andExpect(jsonPath("$.varieties[0].varietyId").value(1))
                .andExpect(jsonPath("$.varieties[0].varietyName").value("Keitt"))
                .andExpect(jsonPath("$.varieties[0].treeCount").value(40))
                .andExpect(jsonPath("$.varieties[0].expectedKg").value(2199.9))
                .andExpect(jsonPath("$.varieties[0].actualKg").value(550.5))
                .andExpect(jsonPath("$.varieties[0].harvestCount").value(2))
                .andExpect(jsonPath("$.varieties[0].achievedPct").value(25.0))
                .andExpect(jsonPath("$.varieties[0].yieldPerTreeKg").value(220.0))
                .andExpect(jsonPath("$.varieties[0].yieldSource").value("Zalka_2025"))
                .andExpect(jsonPath("$.varieties[0].seasonSource").value("varietal_guide_west_africa"))
                .andExpect(jsonPath("$.varieties[0].phases[0].growthPhase").value("gradual production"))
                .andExpect(jsonPath("$.varieties[0].phases[0].yieldShare").value(0.25))
                .andExpect(jsonPath("$.varieties[0].phases[0].source").value("Bally_2002"))
                .andExpect(jsonPath("$.blocks[0].farmId").value(7))
                .andExpect(jsonPath("$.blocks[0].blockCode").value("A"))
                .andExpect(jsonPath("$.blocks[0].expectedKg").value(2199.9))
                .andExpect(jsonPath("$.blocks[0].actualKg").value(550.5))
                .andExpect(jsonPath("$.blocks[0].achievedPct").value(25.0))
                .andExpect(jsonPath("$.total.expectedKg").value(2199.9))
                .andExpect(jsonPath("$.total.actualKg").value(550.5))
                .andExpect(jsonPath("$.total.achievedPct").value(25.0))
                .andExpect(jsonPath("$.notCompared[0].blockCode").value("C"))
                .andExpect(jsonPath("$.notCompared[0].varietyId").value(3))
                .andExpect(jsonPath("$.notCompared[0].varietyName").value("Brooks"))
                .andExpect(jsonPath("$.notCompared[0].treeCount").value(30))
                .andExpect(jsonPath("$.notCompared[0].actualKg").value(120.0))
                .andExpect(jsonPath("$.notCompared[0].harvestCount").value(1))
                .andExpect(jsonPath("$.notCompared[0].reason").value("NO_REFERENCE"));
    }

    @Test
    void getYieldComparison_shouldPassNulls_whenNoParameterIsGiven() throws Exception {
        // Arrange: the service picks the calendar year of today
        when(yieldComparisonService.getYieldComparison(null, null, null, null)).thenReturn(comparison());
        // Act & Assert
        mockMvc.perform(get(URL)).andExpect(status().isOk());
        verify(yieldComparisonService).getYieldComparison(null, null, null, null);
    }

    @Test
    void getYieldComparison_shouldSendNullYields_whenThereIsNoData() throws Exception {
        // Arrange: a young block, nothing expected and nothing picked
        when(yieldComparisonService.getYieldComparison(null, null, null, null)).thenReturn(
                new YieldComparisonResponse(JANUARY_2027, 12, "recorded_plantings",
                        List.of(new VarietyComparison(null, "B", 2L, "Keitt", 100, 0.0, null, 0, null,
                                220.0, "Zalka_2025", "varietal_guide_west_africa",
                                List.of(new PhaseShare("establishment", 0.0, "orchard_literature")))),
                        List.of(new BlockComparison(null, "B", 0.0, null, null)),
                        new Comparison(0.0, null, null),
                        List.of()));
        // Act & Assert: no value is sent as zero
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.varieties[0].farmId").value(nullValue()))
                .andExpect(jsonPath("$.varieties[0].expectedKg").value(0.0))
                .andExpect(jsonPath("$.varieties[0].actualKg").value(nullValue()))
                .andExpect(jsonPath("$.varieties[0].harvestCount").value(0))
                .andExpect(jsonPath("$.varieties[0].achievedPct").value(nullValue()))
                .andExpect(jsonPath("$.total.actualKg").value(nullValue()))
                .andExpect(jsonPath("$.total.achievedPct").value(nullValue()))
                .andExpect(jsonPath("$.notCompared", hasSize(0)));
    }

    @Test
    void getYieldComparison_shouldReturn400WithApiError_whenMonthsIsOutOfRange() throws Exception {
        // Act & Assert: the bounds of the forecast
        for (String months : List.of("0", "25")) {
            ResultActions result = mockMvc.perform(get(URL).param("months", months));
            assertApiError(result, 400, "Bad Request", "Validation failed");
            result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                    .andExpect(jsonPath("$.fieldErrors.months").value("months must be between 1 and 24"));
        }
        verifyNoInteractions(yieldComparisonService);
    }

    @Test
    void getYieldComparison_shouldAcceptTheBoundsOfMonths() throws Exception {
        // Arrange
        when(yieldComparisonService.getYieldComparison(null, null, null, 1)).thenReturn(comparison());
        when(yieldComparisonService.getYieldComparison(null, null, null, 24)).thenReturn(comparison());
        // Act & Assert
        mockMvc.perform(get(URL).param("months", "1")).andExpect(status().isOk());
        mockMvc.perform(get(URL).param("months", "24")).andExpect(status().isOk());
    }

    @Test
    void getYieldComparison_shouldReturn400WithApiError_whenFromIsNotAMonth() throws Exception {
        // Act & Assert: "abc", a month without its leading zero, and a full date are all refused
        for (String from : List.of("abc", "2027-1", "2027-01-01")) {
            ResultActions result = mockMvc.perform(get(URL).param("from", from));
            assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'from'");
            result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        }
        verifyNoInteractions(yieldComparisonService);
    }
}
