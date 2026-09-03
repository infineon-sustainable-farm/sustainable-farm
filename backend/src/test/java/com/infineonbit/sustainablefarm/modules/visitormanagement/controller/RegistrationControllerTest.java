package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@Import(GlobalExceptionHandler.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    private RegistrationResponse buildResponse(Long id, RegistrationStatus status) {
        RegistrationResponse r = new RegistrationResponse();
        r.setId(id);
        r.setVisitorId(1L);
        r.setVisitorName("Alice");
        r.setGroupSize(2);
        r.setTimeSlotId(10L);
        r.setSlotDate(LocalDate.of(2026, 9, 8));
        r.setSlotStart(LocalTime.of(9, 0));
        r.setStatus(status);
        return r;
    }

    @Test
    void list_bySlotId() throws Exception {
        when(registrationService.getRegistrationsBySlot(10L)).thenReturn(List.of(buildResponse(1L, RegistrationStatus.PENDING)));

        mockMvc.perform(get("/api/v1/registrations").param("timeSlotId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void list_byDate() throws Exception {
        when(registrationService.getRegistrationsByDate(LocalDate.of(2026, 9, 8)))
                .thenReturn(List.of(buildResponse(1L, RegistrationStatus.CONFIRMED)));

        mockMvc.perform(get("/api/v1/registrations").param("date", "2026-09-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void list_noFilters_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void approve_notFound_returns404() throws Exception {
        when(registrationService.approve(999L)).thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(patch("/api/v1/registrations/999/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    void approve_notPending_returns422() throws Exception {
        when(registrationService.approve(1L)).thenThrow(new BusinessRuleException("Only PENDING"));

        mockMvc.perform(patch("/api/v1/registrations/1/approve"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void approve_success() throws Exception {
        when(registrationService.approve(1L)).thenReturn(buildResponse(1L, RegistrationStatus.CONFIRMED));

        mockMvc.perform(patch("/api/v1/registrations/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void reject_success() throws Exception {
        when(registrationService.reject(1L)).thenReturn(buildResponse(1L, RegistrationStatus.REJECTED));

        mockMvc.perform(patch("/api/v1/registrations/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void checkIn_notConfirmed_returns422() throws Exception {
        when(registrationService.checkIn(1L)).thenThrow(new BusinessRuleException("Only CONFIRMED"));

        mockMvc.perform(patch("/api/v1/registrations/1/check-in"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void checkIn_success() throws Exception {
        when(registrationService.checkIn(1L)).thenReturn(buildResponse(1L, RegistrationStatus.CHECKED_IN));

        mockMvc.perform(patch("/api/v1/registrations/1/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
    }

    @Test
    void cancel_checkedIn_returns422() throws Exception {
        when(registrationService.cancel(1L)).thenThrow(new BusinessRuleException("already checked in"));

        mockMvc.perform(patch("/api/v1/registrations/1/cancel"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancel_success() throws Exception {
        when(registrationService.cancel(1L)).thenReturn(buildResponse(1L, RegistrationStatus.CANCELLED));

        mockMvc.perform(patch("/api/v1/registrations/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
