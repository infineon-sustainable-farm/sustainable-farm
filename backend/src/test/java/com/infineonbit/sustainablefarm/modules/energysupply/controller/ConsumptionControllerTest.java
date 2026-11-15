package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.ConsumptionLogResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergySource;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyExceptionHandler;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.service.ConsumptionLogService;
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

@WebMvcTest(ConsumptionController.class)
@Import(EnergySupplyExceptionHandler.class)
class ConsumptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsumptionLogService energyConsumptionLogService;

    @Test
    void createReturnsCreatedAndLocation() throws Exception {
        when(energyConsumptionLogService.create(any())).thenReturn(log());
        mockMvc.perform(post("/api/energy/consumption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/energy/consumption/log-1")))
                .andExpect(jsonPath("$.logId").value("log-1"));
    }

    @Test
    void listAndGetReturnDtos() throws Exception {
        when(energyConsumptionLogService.findAll()).thenReturn(List.of(log()));
        when(energyConsumptionLogService.findById("log-1")).thenReturn(log());
        mockMvc.perform(get("/api/energy/consumption"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].logId").value("log-1"));
        mockMvc.perform(get("/api/energy/consumption/log-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.loadCategory").value("IRRIGATION"));
    }

    @Test
    void updateReturnsUpdatedLog() throws Exception {
        when(energyConsumptionLogService.update(any(), any())).thenReturn(log());
        mockMvc.perform(put("/api/energy/consumption/log-1")
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logId").value("log-1"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(energyConsumptionLogService).delete("log-1");
        mockMvc.perform(delete("/api/energy/consumption/log-1")).andExpect(status().isNoContent());
    }

    @Test
    void missingLogReturnsNotFound() throws Exception {
        when(energyConsumptionLogService.findById("missing"))
                .thenThrow(new EnergySupplyNotFoundException("Consumption log", "missing"));
        mockMvc.perform(get("/api/energy/consumption/missing"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidLogReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/energy/consumption")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":null,"loadCategory":null,"kwhConsumed":-1,"source":"SOLAR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("date")));
    }

    private String validRequest() {
        return """
                {"date":"%s","loadCategory":"IRRIGATION","kwhConsumed":2.5,"source":"SOLAR"}
                """.formatted(LocalDate.now());
    }

    private ConsumptionLogResponse log() {
        return new ConsumptionLogResponse(
                "log-1", LocalDate.now(), LoadCategory.IRRIGATION,
                new BigDecimal("2.50"), EnergySource.SOLAR);
    }
}
