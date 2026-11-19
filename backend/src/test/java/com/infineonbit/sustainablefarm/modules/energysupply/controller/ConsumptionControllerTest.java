package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.ConsumptionLogResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergySource;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.service.ConsumptionLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@WebMvcTest(ConsumptionController.class)
class ConsumptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsumptionLogService energyConsumptionLogService;

    @Test
    void createReturnsCreatedAndLocation() throws Exception {
        when(energyConsumptionLogService.create(any())).thenReturn(log());
        mockMvc.perform(post("/api/energy-supply/consumption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/energy-supply/consumption/1")))
                .andExpect(jsonPath("$.logId").value(1));
    }

    @Test
    void listAndGetReturnDtos() throws Exception {
        when(energyConsumptionLogService.findAll()).thenReturn(List.of(log()));
        when(energyConsumptionLogService.findById(1L)).thenReturn(log());
        mockMvc.perform(get("/api/energy-supply/consumption"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].logId").value(1));
        mockMvc.perform(get("/api/energy-supply/consumption/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.loadCategory").value("IRRIGATION"));
    }

    @Test
    void updateReturnsUpdatedLog() throws Exception {
        when(energyConsumptionLogService.update(any(), any())).thenReturn(log());
        mockMvc.perform(put("/api/energy-supply/consumption/1")
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logId").value(1));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(energyConsumptionLogService).delete(1L);
        mockMvc.perform(delete("/api/energy-supply/consumption/1")).andExpect(status().isNoContent());
    }

    @Test
    void missingLogReturnsNotFound() throws Exception {
        when(energyConsumptionLogService.findById(99L))
                .thenThrow(new EnergySupplyNotFoundException("Consumption log", "99"));
        mockMvc.perform(get("/api/energy-supply/consumption/99"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidLogReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/energy-supply/consumption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":null,"loadCategory":null,"kwhConsumed":-1,"source":"SOLAR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.date").exists());
    }

    private String validRequest() {
        return """
                {"date":"%s","loadCategory":"IRRIGATION","kwhConsumed":2.5,"source":"SOLAR"}
                """.formatted(LocalDate.now());
    }

    private ConsumptionLogResponse log() {
        return new ConsumptionLogResponse(
                1L, LocalDate.now(), LoadCategory.IRRIGATION,
                new BigDecimal("2.50"), EnergySource.SOLAR);
    }
}
