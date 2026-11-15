package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyAlertResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.AlertStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyExceptionHandler;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@Import(EnergySupplyExceptionHandler.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService energyAlertService;

    @Test
    void listReturnsAlertsForTheFrontend() throws Exception {
        when(energyAlertService.findAll()).thenReturn(List.of(openAlert()));

        mockMvc.perform(get("/api/energy/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertId").value("alert-1"))
                .andExpect(jsonPath("$[0].componentName").value("Battery"))
                .andExpect(jsonPath("$[0].severity").value("WARNING"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void listOpenReturnsOnlyOpenAlerts() throws Exception {
        when(energyAlertService.findOpen()).thenReturn(List.of(openAlert()));

        mockMvc.perform(get("/api/energy/alerts/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertId").value("alert-1"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void resolveChangesAlertStatus() throws Exception {
        when(energyAlertService.updateStatus("alert-1", AlertStatus.RESOLVED)).thenReturn(resolvedAlert());
        mockMvc.perform(patch("/api/energy/alerts/alert-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void missingAlertReturnsNotFound() throws Exception {
        when(energyAlertService.updateStatus("missing", AlertStatus.RESOLVED))
                .thenThrow(new EnergySupplyNotFoundException("Energy alert", "missing"));
        mockMvc.perform(patch("/api/energy/alerts/missing/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/energy/alerts/alert-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest());
    }

    private EnergyAlertResponse resolvedAlert() {
        return new EnergyAlertResponse(
                "alert-1", "ENG-002", "Battery", AlertSeverity.WARNING,
                "Battery charge is low", LocalDateTime.now(), AlertStatus.RESOLVED);
    }

    private EnergyAlertResponse openAlert() {
        return new EnergyAlertResponse(
                "alert-1", "ENG-002", "Battery", AlertSeverity.WARNING,
                "Battery charge is low", LocalDateTime.now(), AlertStatus.OPEN);
    }
}
