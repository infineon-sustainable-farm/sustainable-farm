package com.sustainablefarm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.OperatorCreateRequest;
import com.sustainablefarm.dto.response.OperatorResponse;
import com.sustainablefarm.exception.GlobalExceptionHandler;
import com.sustainablefarm.model.Operator;
import com.sustainablefarm.model.Operator.ActiveStatus;
import com.sustainablefarm.model.Operator.Role;
import com.sustainablefarm.service.OperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperatorController.class)
@Import(GlobalExceptionHandler.class)
class OperatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OperatorService operatorService;

    @MockBean
    private DtoMapper dtoMapper;

    private Operator testOperator;
    private OperatorResponse testResponse;

    @BeforeEach
    void setUp() {
        testOperator = new Operator();
        testOperator.setOperatorId("OP-001");
        testOperator.setOperatorName("Jane Doe");
        testOperator.setRole(Role.SUPERVISOR);
        testOperator.setActiveStatus(ActiveStatus.ACTIVE);
        testOperator.setHireDate(LocalDate.of(2024, 1, 15));

        testResponse = OperatorResponse.builder()
                .operatorId("OP-001")
                .operatorName("Jane Doe")
                .role(Role.SUPERVISOR)
                .activeStatus(ActiveStatus.ACTIVE)
                .hireDate(LocalDate.of(2024, 1, 15))
                .build();
    }

    @Test
    void createOperator_success() throws Exception {
        OperatorCreateRequest request = new OperatorCreateRequest(
                "OP-001", "Jane Doe", Role.SUPERVISOR, "HACCP", ActiveStatus.ACTIVE, LocalDate.of(2024, 1, 15));

        when(dtoMapper.toEntity(any(OperatorCreateRequest.class))).thenReturn(testOperator);
        when(operatorService.createOperator(any(Operator.class))).thenReturn(testOperator);
        when(dtoMapper.toResponse(testOperator)).thenReturn(testResponse);

        mockMvc.perform(post("/api/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operatorId").value("OP-001"))
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    @Test
    void createOperator_validationFailure() throws Exception {
        OperatorCreateRequest request = new OperatorCreateRequest(
                "", null, null, null, null, null);

        mockMvc.perform(post("/api/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getOperatorById_notFound() throws Exception {
        when(operatorService.getOperatorById("MISSING"))
                .thenThrow(new com.sustainablefarm.exception.ResourceNotFoundException("Operator not found with ID: MISSING"));

        mockMvc.perform(get("/api/operators/MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }
}
