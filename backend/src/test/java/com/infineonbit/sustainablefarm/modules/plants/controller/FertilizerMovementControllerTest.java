package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.FertilizerMovementResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerMovementType;
import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerUnit;
import com.infineonbit.sustainablefarm.modules.plants.service.FertilizerMovementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FertilizerMovementController.class)
@Import(CoreExceptionHandler.class)
public class FertilizerMovementControllerTest {

    private static final String URL = "/api/plants/fertilizer-movements";
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FertilizerMovementService fertilizerMovementService;

    /** Common part of every 4xx body: the application-wide ApiError envelope. */
    private static void assertApiError(ResultActions result, String message) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("uri=" + URL))
                .andExpect(jsonPath("$.fieldErrors").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private static FertilizerMovementResponse application() {
        return new FertilizerMovementResponse(13L, 1L, "NPK 15-15-15", FertilizerMovementType.APPLICATION,
                LocalDate.of(2026, 6, 15), 250.0, FertilizerUnit.KG, null, "B", "Team A", "around the tree base",
                null, null, null, null, null, null, "user_entry", NOW);
    }

    @Test
    void getAllMovements_shouldPassEveryFilter() throws Exception {
        // Arrange
        when(fertilizerMovementService.getAllMovements(1L, FertilizerMovementType.APPLICATION, 2, "B",
                LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 30))).thenReturn(List.of(application()));
        // Act
        ResultActions result = mockMvc.perform(get(URL)
                .param("fertilizerId", "1")
                .param("movementType", "APPLICATION")
                .param("farmId", "2")
                .param("blockCode", "B")
                .param("from", "2026-06-02")
                .param("to", "2026-06-30"));
        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(13))
                .andExpect(jsonPath("$[0].fertilizerName").value("NPK 15-15-15"))
                .andExpect(jsonPath("$[0].movementType").value("APPLICATION"))
                .andExpect(jsonPath("$[0].blockCode").value("B"))
                .andExpect(jsonPath("$[0].applicator").value("Team A"));
    }

    @Test
    void getAllMovements_shouldPassNulls_whenNoFilterIsGiven() throws Exception {
        // Arrange
        when(fertilizerMovementService.getAllMovements(null, null, null, null, null, null)).thenReturn(List.of());
        // Act & Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllMovements_shouldReturn400WithApiError_whenADateIsNotIso() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("to", "30/06/2026"));
        // Assert
        assertApiError(result, "Invalid value for parameter 'to'");
        verifyNoInteractions(fertilizerMovementService);
    }

    @Test
    void getAllMovements_shouldReturn400WithApiError_whenMovementTypeIsUnknown() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("movementType", "GIFT"));
        // Assert
        assertApiError(result, "Invalid value for parameter 'movementType'");
        verifyNoInteractions(fertilizerMovementService);
    }
}
