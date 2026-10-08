package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryEventResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.service.NurseryEventService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NurseryEventController.class)
@Import(CoreExceptionHandler.class)
public class NurseryEventControllerTest {

    private static final String URL = "/api/plants/nursery-events";
    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NurseryEventService nurseryEventService;

    /** A 400 raised while reading a parameter, without fieldErrors. */
    private static void assertBadParameter(ResultActions result, String parameter) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter '" + parameter + "'"))
                .andExpect(jsonPath("$.path").value("uri=" + URL))
                .andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void getAllEvents_shouldPassEveryFilter() throws Exception {
        // Arrange
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        when(nurseryEventService.getAllEvents(1L, 2, NurseryEventType.TRANSPLANT, from, to)).thenReturn(List.of(
                new NurseryEventResponse(14L, 1L, "P1", 2, NurseryEventType.TRANSPLANT, LocalDate.of(2026, 9, 20),
                        null, 100, null, "E", 7L, "user_entry", NOW)));
        // Act & Assert
        mockMvc.perform(get(URL).param("batchId", "1").param("farmId", "2").param("eventType", "TRANSPLANT")
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(14))
                .andExpect(jsonPath("$[0].batchCode").value("P1"))
                .andExpect(jsonPath("$[0].eventType").value("TRANSPLANT"))
                .andExpect(jsonPath("$[0].populationEventId").value(7));
        verify(nurseryEventService).getAllEvents(1L, 2, NurseryEventType.TRANSPLANT, from, to);
    }

    @Test
    void getAllEvents_shouldReturnAnEmptyList_whenNothingMatches() throws Exception {
        // Arrange
        when(nurseryEventService.getAllEvents(null, null, null, null, null)).thenReturn(List.of());
        // Act & Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllEvents_shouldReturn400_whenTheEventTypeIsUnknown() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("eventType", "DEATH"));
        // Assert
        assertBadParameter(result, "eventType");
        verify(nurseryEventService, never()).getAllEvents(any(), any(), any(), any(), any());
    }

    @Test
    void getAllEvents_shouldReturn400_whenADateIsNotInTheIsoFormat() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get(URL).param("from", "01/09/2026"));
        // Assert
        assertBadParameter(result, "from");
        verify(nurseryEventService, never()).getAllEvents(any(), any(), any(), any(), any());
    }
}
