package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantsOverviewResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantsOverviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error body is asserted in full, as for the alerts.
 *
 * <p>The context is closed after the class, as for the growth phase shares:
 * one more cached context would evict a {@code @SpringBootTest} context, whose
 * create-drop then empties the database that the other integration tests
 * still use.
 */
@DirtiesContext
@WebMvcTest(PlantsOverviewController.class)
@Import(CoreExceptionHandler.class)
public class PlantsOverviewControllerTest {

    private static final String URL = "/api/plants/overview";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlantsOverviewService plantsOverviewService;

    @Test
    void getOverview_shouldReturnTheFiguresOfTheDay() throws Exception {
        // Arrange
        when(plantsOverviewService.getOverview(7, "A"))
                .thenReturn(new PlantsOverviewResponse(LocalDate.of(2027, 6, 15), 170, 100, 40, 3, 1));
        // Act & Assert
        mockMvc.perform(get(URL).param("farmId", "7").param("blockCode", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2027-06-15"))
                .andExpect(jsonPath("$.totalPlants").value(170))
                .andExpect(jsonPath("$.inGrowthStage").value(100))
                .andExpect(jsonPath("$.readyForHarvest").value(40))
                .andExpect(jsonPath("$.activeAlerts").value(3))
                .andExpect(jsonPath("$.criticalAlerts").value(1));
    }

    @Test
    void getOverview_shouldPassNulls_whenNoParameterIsGiven() throws Exception {
        // Arrange
        when(plantsOverviewService.getOverview(null, null))
                .thenReturn(new PlantsOverviewResponse(LocalDate.of(2027, 6, 15), 0, 0, 0, 0, 0));
        // Act & Assert
        mockMvc.perform(get(URL)).andExpect(status().isOk());
        verify(plantsOverviewService).getOverview(null, null);
    }

    @Test
    void getOverview_shouldReturn400WithApiError_whenFarmIsNotANumber() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("farmId", "abc"));
        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'farmId'"))
                .andExpect(jsonPath("$.path").value("uri=" + URL))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        verifyNoInteractions(plantsOverviewService);
    }
}
