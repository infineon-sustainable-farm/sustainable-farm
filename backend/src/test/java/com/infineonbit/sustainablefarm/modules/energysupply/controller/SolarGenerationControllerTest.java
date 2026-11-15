package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.SolarGenerationLogResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyExceptionHandler;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.service.SolarGenerationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolarGenerationController.class)
@Import(EnergySupplyExceptionHandler.class)
class SolarGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolarGenerationLogService energySolarGenerationLogService;

    @Test
    void createReturnsCreatedAndLocation() throws Exception {
        when(energySolarGenerationLogService.create(any())).thenReturn(log());
        mockMvc.perform(post("/api/energy/solar")
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/energy/solar/solar-1")))
                .andExpect(jsonPath("$.logId").value("solar-1"));
    }

    @Test
    void listAndGetReturnDtos() throws Exception {
        when(energySolarGenerationLogService.findAll()).thenReturn(List.of(log()));
        when(energySolarGenerationLogService.findById("solar-1")).thenReturn(log());
        mockMvc.perform(get("/api/energy/solar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].componentId").value("ENG-001"));
        mockMvc.perform(get("/api/energy/solar/solar-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.kwhProduced").value(22.5));
    }

    @Test
    void updateReturnsUpdatedLog() throws Exception {
        when(energySolarGenerationLogService.update(any(), any())).thenReturn(log());
        mockMvc.perform(put("/api/energy/solar/solar-1")
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logId").value("solar-1"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(energySolarGenerationLogService).delete("solar-1");
        mockMvc.perform(delete("/api/energy/solar/solar-1")).andExpect(status().isNoContent());
    }

    @Test
    void missingLogReturnsNotFound() throws Exception {
        when(energySolarGenerationLogService.findById("missing"))
                .thenThrow(new EnergySupplyNotFoundException("Solar generation log", "missing"));
        mockMvc.perform(get("/api/energy/solar/missing"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidLogReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/energy/solar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"componentId":"","date":null,"kwhProduced":-1,"efficiencyPct":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("componentId")));
    }

    private String validRequest() {
        return """
                {"componentId":"ENG-001","date":"%s","kwhProduced":22.5,"efficiencyPct":80}
                """.formatted(LocalDate.now());
    }

    private SolarGenerationLogResponse log() {
        return new SolarGenerationLogResponse(
                "solar-1", "ENG-001", "PV array", LocalDate.now(),
                new BigDecimal("22.50"), new BigDecimal("80.00"));
    }
}
