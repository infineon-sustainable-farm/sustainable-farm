package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.GeneratorEventResponseDto;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyExceptionHandler;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.GeneratorEventNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.service.GeneratorEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeneratorController.class)
@Import(EnergySupplyExceptionHandler.class)
class GeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeneratorEventService energyGeneratorEventService;

    @Test
    void createReturnsCreatedResponseAndLocation() throws Exception {
        when(energyGeneratorEventService.create(any())).thenReturn(eventResponse("event-1"));

        mockMvc.perform(post("/api/energy/generator-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/energy/generator-events/event-1")))
                .andExpect(jsonPath("$.eventId").value("event-1"))
                .andExpect(jsonPath("$.date").value(LocalDate.now().toString()));
    }

    @Test
    void updateReturnsUpdatedResponse() throws Exception {
        when(energyGeneratorEventService.update(eq("event-1"), any())).thenReturn(eventResponse("event-1"));

        mockMvc.perform(put("/api/energy/generator-events/event-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("event-1"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(energyGeneratorEventService).delete("event-1");

        mockMvc.perform(delete("/api/energy/generator-events/event-1"))
                .andExpect(status().isNoContent());

        verify(energyGeneratorEventService).delete("event-1");
    }

    @Test
    void missingEventReturnsNotFoundJson() throws Exception {
        when(energyGeneratorEventService.findById("missing"))
                .thenThrow(new GeneratorEventNotFoundException("missing"));

        mockMvc.perform(get("/api/energy/generator-events/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Generator event not found: missing"));
    }

    @Test
    void invalidRequestReturnsFieldLevelValidationMessage() throws Exception {
        String request = """
                {
                  "date": "%s",
                  "durationHours": -1,
                  "fuelLiters": null,
                  "costEur": 0,
                  "triggerReason": "Test"
                }
                """.formatted(LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/energy/generator-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("date")))
                .andExpect(jsonPath("$.message", containsString("durationHours")));
    }

    @Test
    void listReturnsResponseDtos() throws Exception {
        when(energyGeneratorEventService.findAll()).thenReturn(List.of(eventResponse("event-1")));

        mockMvc.perform(get("/api/energy/generator-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("event-1"));
    }

    @Test
    void deleteMissingEventReturnsNotFound() throws Exception {
        doThrow(new GeneratorEventNotFoundException("missing"))
                .when(energyGeneratorEventService).delete("missing");

        mockMvc.perform(delete("/api/energy/generator-events/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String validRequest() {
        return """
                {
                  "date": "%s",
                  "triggerReason": "Grid outage",
                  "durationHours": 2.5,
                  "fuelLiters": null,
                  "costEur": 30
                }
                """.formatted(LocalDate.now());
    }

    private GeneratorEventResponseDto eventResponse(String eventId) {
        return new GeneratorEventResponseDto(
                eventId, LocalDate.now(), "Grid outage",
                new BigDecimal("2.50"), null, new BigDecimal("30.00"));
    }
}
