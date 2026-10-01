package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisitorController.class)
@Import(CoreExceptionHandler.class)
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
    void create_invalidPhone_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/visitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alice\",\"groupSize\":1,"
                                + "\"phone\":\"abc\",\"type\":\"INDIVIDUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unknownVisitorType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/visitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alice\",\"groupSize\":1,\"type\":\"FAMILY\"}"))
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

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(registrationService).deleteVisitor(1L);

        mockMvc.perform(delete("/api/v1/visitors/1"))
                .andExpect(status().isNoContent());

        verify(registrationService).deleteVisitor(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Visitor 999 not found"))
                .when(registrationService).deleteVisitor(999L);

        mockMvc.perform(delete("/api/v1/visitors/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_referenced_returns422() throws Exception {
        doThrow(new BusinessRuleException("registrations still reference it"))
                .when(registrationService).deleteVisitor(1L);

        mockMvc.perform(delete("/api/v1/visitors/1"))
                .andExpect(status().isUnprocessableEntity());
    }
}
