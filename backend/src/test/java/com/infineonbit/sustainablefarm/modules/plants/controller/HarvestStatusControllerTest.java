package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestStatusResponse.VarietyHarvestStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.service.HarvestStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
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
 * The error bodies are asserted in full, as for the alerts.
 *
 * <p>The context is closed after the class, as for the growth phase shares:
 * one more cached context would evict a {@code @SpringBootTest} context, whose
 * create-drop then empties the database that the other integration tests
 * still use.
 */
@DirtiesContext
@WebMvcTest(HarvestStatusController.class)
@Import(CoreExceptionHandler.class)
public class HarvestStatusControllerTest {

    private static final String URL = "/api/plants/harvest-status";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HarvestStatusService harvestStatusService;

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

    private static HarvestStatusResponse statuses() {
        return new HarvestStatusResponse(LocalDate.of(2027, 6, 15), 30, List.of(
                new VarietyHarvestStatus(7, "A", 1L, "Keitt", 40, MaturityStatus.RIPE,
                        LocalDate.of(2027, 5, 1), LocalDate.of(2027, 7, 31), -45,
                        LocalDate.of(2027, 6, 24), 21L, LocalDate.of(2027, 5, 20),
                        "varietal_guide_west_africa", "Bally_2002"),
                new VarietyHarvestStatus(7, "C", 3L, "Brooks", 30, null, null, null, null, null, null, null,
                        null, null)));
    }

    @Test
    void getHarvestStatus_shouldReturnTheStatusOfEveryVariety() throws Exception {
        // Arrange
        when(harvestStatusService.getHarvestStatus(7, "A", 20)).thenReturn(statuses());
        // Act
        ResultActions result = mockMvc.perform(get(URL)
                .param("farmId", "7")
                .param("blockCode", "A")
                .param("withinDays", "20"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2027-06-15"))
                .andExpect(jsonPath("$.withinDays").value(30))
                .andExpect(jsonPath("$.varieties", hasSize(2)))
                .andExpect(jsonPath("$.varieties[0].farmId").value(7))
                .andExpect(jsonPath("$.varieties[0].blockCode").value("A"))
                .andExpect(jsonPath("$.varieties[0].varietyId").value(1))
                .andExpect(jsonPath("$.varieties[0].varietyName").value("Keitt"))
                .andExpect(jsonPath("$.varieties[0].treeCount").value(40))
                .andExpect(jsonPath("$.varieties[0].maturityStatus").value("RIPE"))
                .andExpect(jsonPath("$.varieties[0].estimatedHarvestDate").value("2027-05-01"))
                .andExpect(jsonPath("$.varieties[0].seasonEnd").value("2027-07-31"))
                .andExpect(jsonPath("$.varieties[0].daysFromToday").value(-45))
                .andExpect(jsonPath("$.varieties[0].harvestAllowedFrom").value("2027-06-24"))
                .andExpect(jsonPath("$.varieties[0].treatmentId").value(21))
                .andExpect(jsonPath("$.varieties[0].firstHarvestDate").value("2027-05-20"))
                .andExpect(jsonPath("$.varieties[0].seasonSource").value("varietal_guide_west_africa"))
                .andExpect(jsonPath("$.varieties[0].phaseShareSource").value("Bally_2002"))
                .andExpect(jsonPath("$.varieties[1].varietyName").value("Brooks"))
                .andExpect(jsonPath("$.varieties[1].maturityStatus").value(nullValue()))
                .andExpect(jsonPath("$.varieties[1].estimatedHarvestDate").value(nullValue()))
                .andExpect(jsonPath("$.varieties[1].harvestAllowedFrom").value(nullValue()))
                .andExpect(jsonPath("$.varieties[1].firstHarvestDate").value(nullValue()));
    }

    @Test
    void getHarvestStatus_shouldPassNulls_whenNoParameterIsGiven() throws Exception {
        // Arrange: the service picks today and 30 days
        when(harvestStatusService.getHarvestStatus(null, null, null)).thenReturn(statuses());
        // Act & Assert
        mockMvc.perform(get(URL)).andExpect(status().isOk());
        verify(harvestStatusService).getHarvestStatus(null, null, null);
    }

    @Test
    void getHarvestStatus_shouldReturn400WithApiError_whenWithinDaysIsOutOfRange() throws Exception {
        // Act & Assert: the bounds of the alerts
        for (String withinDays : List.of("0", "91")) {
            ResultActions result = mockMvc.perform(get(URL).param("withinDays", withinDays));
            assertApiError(result, 400, "Bad Request", "Validation failed");
            result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                    .andExpect(jsonPath("$.fieldErrors.withinDays").value("withinDays must be between 1 and 90"));
        }
        verifyNoInteractions(harvestStatusService);
    }

    @Test
    void getHarvestStatus_shouldAcceptTheBoundsOfWithinDays() throws Exception {
        // Arrange
        when(harvestStatusService.getHarvestStatus(null, null, 1)).thenReturn(statuses());
        when(harvestStatusService.getHarvestStatus(null, null, 90)).thenReturn(statuses());
        // Act & Assert
        mockMvc.perform(get(URL).param("withinDays", "1")).andExpect(status().isOk());
        mockMvc.perform(get(URL).param("withinDays", "90")).andExpect(status().isOk());
    }

    @Test
    void getHarvestStatus_shouldReturn400WithApiError_whenFarmIsNotANumber() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("farmId", "abc"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'farmId'");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        verifyNoInteractions(harvestStatusService);
    }
}
