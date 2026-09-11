package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BriefingDeliverRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BriefingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BriefingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BriefingController.class)
@Import(GlobalExceptionHandler.class)
class BriefingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    private BriefingResponse buildResponse(Long id, BriefingStatus status) {
        BriefingResponse r = new BriefingResponse();
        r.setId(id);
        r.setRegistrationId(100L);
        r.setStatus(status);
        r.setStaffMember("Guard Camille");
        return r;
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(registrationService.getBriefingForRegistration(100L))
                .thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/registrations/100/briefing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_success() throws Exception {
        when(registrationService.getBriefingForRegistration(100L))
                .thenReturn(buildResponse(200L, BriefingStatus.PENDING));

        mockMvc.perform(get("/api/v1/registrations/100/briefing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.staffMember").value("Guard Camille"));
    }

    @Test
    void deliver_noBriefing_returns422() throws Exception {
        when(registrationService.deliverBriefing(eq(100L), any()))
                .thenThrow(new BusinessRuleException("No briefing"));

        mockMvc.perform(patch("/api/v1/registrations/100/briefing/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffMember\":\"Camille\",\"signature\":\"abc\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deliver_missingStaffMember_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/registrations/100/briefing/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signature\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deliver_success() throws Exception {
        when(registrationService.deliverBriefing(eq(100L), any()))
                .thenReturn(buildResponse(200L, BriefingStatus.DONE));

        mockMvc.perform(patch("/api/v1/registrations/100/briefing/deliver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffMember\":\"Camille\",\"signature\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }
}
