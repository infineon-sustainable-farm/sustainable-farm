package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitorType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisitorController.class)
@Import(GlobalExceptionHandler.class)
class VisitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    private VisitorResponse buildResponse(Long id) {
        VisitorResponse r = new VisitorResponse();
        r.setId(id);
        r.setFullName("Alice Dupont");
        r.setGroupSize(2);
        r.setEmail("alice@test.com");
        r.setType(VisitorType.INDIVIDUAL);
        return r;
    }

    @Test
    void list_returns200() throws Exception {
        when(registrationService.listVisitors()).thenReturn(List.of(buildResponse(1L)));

        mockMvc.perform(get("/api/v1/visitors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Alice Dupont"));
    }

    @Test
    void get_byId_returns200() throws Exception {
        when(registrationService.getVisitor(1L)).thenReturn(buildResponse(1L));

        mockMvc.perform(get("/api/v1/visitors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(registrationService.getVisitor(999L)).thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/visitors/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(registrationService.createVisitor(any(VisitorRequest.class))).thenReturn(buildResponse(1L));

        mockMvc.perform(post("/api/v1/visitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alice\",\"groupSize\":2,\"email\":\"alice@test.com\",\"type\":\"INDIVIDUAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Alice Dupont"));
    }

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        when(registrationService.createVisitor(any(VisitorRequest.class)))
                .thenThrow(new ConflictException("duplicate"));

        mockMvc.perform(post("/api/v1/visitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alice\",\"groupSize\":1,\"email\":\"alice@test.com\",\"type\":\"INDIVIDUAL\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void create_missingFullName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/visitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupSize\":1,\"type\":\"INDIVIDUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200() throws Exception {
        when(registrationService.updateVisitor(any(Long.class), any(VisitorRequest.class)))
                .thenReturn(buildResponse(1L));

        mockMvc.perform(put("/api/v1/visitors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alice\",\"groupSize\":2,\"email\":\"alice@test.com\",\"type\":\"INDIVIDUAL\"}"))
                .andExpect(status().isOk());
    }
}
