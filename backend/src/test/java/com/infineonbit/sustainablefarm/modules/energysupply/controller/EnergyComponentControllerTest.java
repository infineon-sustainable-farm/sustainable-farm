package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyExceptionHandler;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.service.EnergyComponentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnergyComponentController.class)
@Import(EnergySupplyExceptionHandler.class)
class EnergyComponentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnergyComponentService energyComponentService;

    @Test
    void createReturnsCreatedAndLocation() throws Exception {
        when(energyComponentService.create(any())).thenReturn(component());
        mockMvc.perform(post("/api/energy/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/energy/components/ENG-001")))
                .andExpect(jsonPath("$.componentId").value("ENG-001"));
    }

    @Test
    void createAcceptsIdsBeyondTheInitialRegistry() throws Exception {
        when(energyComponentService.create(any())).thenReturn(new EnergyComponentResponse(
                "ENG-006", "Additional component", ComponentCategory.BATTERY,
                BigDecimal.ONE, ComponentStatus.OPERATIONAL));
        mockMvc.perform(post("/api/energy/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"componentId":"ENG-006","name":"Additional component","category":"BATTERY","capacityValue":1,"status":"OPERATIONAL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.componentId").value("ENG-006"));
    }

    @Test
    void listAndGetReturnResponseDtos() throws Exception {
        when(energyComponentService.findAll()).thenReturn(List.of(component()));
        when(energyComponentService.findById("ENG-001")).thenReturn(component());
        mockMvc.perform(get("/api/energy/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Solar array"));
        mockMvc.perform(get("/api/energy/components/ENG-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPERATIONAL"));
    }

    @Test
    void updateReturnsUpdatedComponent() throws Exception {
        when(energyComponentService.update(any(), any())).thenReturn(component());
        mockMvc.perform(put("/api/energy/components/ENG-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.componentId").value("ENG-001"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(energyComponentService).delete("ENG-001");
        mockMvc.perform(delete("/api/energy/components/ENG-001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingComponentReturnsNotFound() throws Exception {
        when(energyComponentService.findById("missing"))
                .thenThrow(new EnergySupplyNotFoundException("Energy component", "missing"));
        mockMvc.perform(get("/api/energy/components/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidComponentReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/energy/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"componentId":"bad","name":"","category":null,"capacityValue":-1,"status":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("componentId")));
    }

    private String validRequest() {
        return """
                {"componentId":"ENG-001","name":"Solar array","category":"PV_ARRAY","capacityValue":10.5,"status":"OPERATIONAL"}
                """;
    }

    private EnergyComponentResponse component() {
        return new EnergyComponentResponse(
                "ENG-001", "Solar array", ComponentCategory.PV_ARRAY,
                new BigDecimal("10.50"), ComponentStatus.OPERATIONAL);
    }
}
