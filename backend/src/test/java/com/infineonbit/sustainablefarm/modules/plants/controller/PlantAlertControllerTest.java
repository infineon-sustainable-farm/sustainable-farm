package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantAlertResponse.PlantAlert;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.YieldForecastResponse.VarietyWithoutReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertSeverity;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantAlertType;
import com.infineonbit.sustainablefarm.modules.plants.service.PlantAlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
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

/** The error bodies are asserted in full, as for the yield forecast route. */
@WebMvcTest(PlantAlertController.class)
@Import(CoreExceptionHandler.class)
public class PlantAlertControllerTest {

    private static final String URL = "/api/plants/alerts";
    private static final String WITHIN_DAYS_MESSAGE = "withinDays must be between 1 and 90";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlantAlertService plantAlertService;

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

    private static PlantAlertResponse alerts(int withinDays) {
        return new PlantAlertResponse(LocalDate.of(2027, 5, 15), withinDays,
                List.of(new PlantAlert(PlantAlertType.NURSERY_READY, PlantAlertSeverity.WARNING, 5, "D", "Keitt",
                        "Batch P1 (Keitt, 120 plants): ready to transplant since 20 April 2027; "
                                + "transplant planned on 15 June 2027 to block D, in 31 days",
                        LocalDate.of(2027, 6, 15), null, 31, null, null, null, null, 21L, null)),
                List.of(new VarietyWithoutReference(null, "A", "Palmer", 10)));
    }

    @Test
    void getAlerts_shouldReturnTheAlerts_withEveryKeyOfAnAlert() throws Exception {
        // Arrange
        when(plantAlertService.getAlerts(5, "D", 45)).thenReturn(alerts(45));
        // Act
        ResultActions result = mockMvc.perform(get(URL)
                .param("farmId", "5")
                .param("blockCode", "D")
                .param("withinDays", "45"));
        // Assert: the envelope, then every key of the alert, the null ones included
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.today").value("2027-05-15"))
                .andExpect(jsonPath("$.withinDays").value(45))
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0]", aMapWithSize(15)))
                .andExpect(jsonPath("$.alerts[0].type").value("NURSERY_READY"))
                .andExpect(jsonPath("$.alerts[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.alerts[0].farmId").value(5))
                .andExpect(jsonPath("$.alerts[0].blockCode").value("D"))
                .andExpect(jsonPath("$.alerts[0].varietyName").value("Keitt"))
                .andExpect(jsonPath("$.alerts[0].message").value(
                        "Batch P1 (Keitt, 120 plants): ready to transplant since 20 April 2027; "
                                + "transplant planned on 15 June 2027 to block D, in 31 days"))
                .andExpect(jsonPath("$.alerts[0].date").value("2027-06-15"))
                .andExpect(jsonPath("$.alerts[0].endDate").value(nullValue()))
                .andExpect(jsonPath("$.alerts[0].daysFromToday").value(31))
                .andExpect(jsonPath("$.alerts[0].varietyId").value(nullValue()))
                .andExpect(jsonPath("$.alerts[0].treatmentId").value(nullValue()))
                .andExpect(jsonPath("$.alerts[0].findingId").value(nullValue()))
                .andExpect(jsonPath("$.alerts[0].fertilizerId").value(nullValue()))
                .andExpect(jsonPath("$.alerts[0].batchId").value(21))
                .andExpect(jsonPath("$.alerts[0].source").value(nullValue()))
                .andExpect(jsonPath("$.varietiesWithoutReference[0].farmId").value(nullValue()))
                .andExpect(jsonPath("$.varietiesWithoutReference[0].blockCode").value("A"))
                .andExpect(jsonPath("$.varietiesWithoutReference[0].varietyName").value("Palmer"))
                .andExpect(jsonPath("$.varietiesWithoutReference[0].treeCount").value(10));
    }

    @Test
    void getAlerts_shouldPassNulls_whenNoParameterIsGiven() throws Exception {
        // Arrange: the service picks today and 30 days
        when(plantAlertService.getAlerts(null, null, null)).thenReturn(alerts(30));
        // Act & Assert
        mockMvc.perform(get(URL)).andExpect(status().isOk());
        verify(plantAlertService).getAlerts(null, null, null);
    }

    @Test
    void getAlerts_shouldReturn400WithApiError_whenWithinDaysIsZero() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("withinDays", "0"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.withinDays").value(WITHIN_DAYS_MESSAGE));
        verifyNoInteractions(plantAlertService);
    }

    @Test
    void getAlerts_shouldReturn400WithApiError_whenWithinDaysIsAbove90() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("withinDays", "91"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.withinDays").value(WITHIN_DAYS_MESSAGE));
        verifyNoInteractions(plantAlertService);
    }

    @Test
    void getAlerts_shouldAcceptTheBoundsOfWithinDays() throws Exception {
        // Arrange
        when(plantAlertService.getAlerts(null, null, 1)).thenReturn(alerts(1));
        when(plantAlertService.getAlerts(null, null, 90)).thenReturn(alerts(90));
        // Act & Assert
        mockMvc.perform(get(URL).param("withinDays", "1")).andExpect(status().isOk());
        mockMvc.perform(get(URL).param("withinDays", "90")).andExpect(status().isOk());
    }

    @Test
    void getAlerts_shouldReturn400_whenWithinDaysIsNotANumber() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("withinDays", "abc"));
        // Assert: a 400 on withinDays, whose message is the conversion text of Spring (a known limitation)
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors.withinDays").value(startsWith("Failed to convert")));
        verifyNoInteractions(plantAlertService);
    }

    @Test
    void getAlerts_shouldReturn400WithApiError_whenFarmIdIsNotANumber() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("farmId", "abc"));
        // Assert
        assertApiError(result, 400, "Bad Request", "Invalid value for parameter 'farmId'");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
        verifyNoInteractions(plantAlertService);
    }
}
