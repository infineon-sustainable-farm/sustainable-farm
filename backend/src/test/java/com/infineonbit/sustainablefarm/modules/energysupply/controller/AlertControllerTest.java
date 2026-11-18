package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyAlertResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService energyAlertService;

    @Test
    void resolveChangesAlertStatus() throws Exception {
        when(energyAlertService.updateStatus(1L, AlertStatus.RESOLVED)).thenReturn(resolvedAlert());
        mockMvc.perform(patch("/api/energy-supply/alerts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.alertId").value(1));
    }

    @Test
    void missingAlertReturnsNotFound() throws Exception {
        when(energyAlertService.updateStatus(99L, AlertStatus.RESOLVED))
                .thenThrow(new EnergySupplyNotFoundException("Energy alert", "99"));
        mockMvc.perform(patch("/api/energy-supply/alerts/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/energy-supply/alerts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest());
    }

    private EnergyAlertResponse resolvedAlert() {
        return new EnergyAlertResponse(
                1L, "ENG-002", "Battery", AlertSeverity.WARNING,
                "Battery charge is low", LocalDateTime.now(), AlertStatus.RESOLVED);
    }
}
