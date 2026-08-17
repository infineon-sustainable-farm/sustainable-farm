package com.sustainablefarm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.BatchCreateRequest;
import com.sustainablefarm.dto.response.BatchResponse;
import com.sustainablefarm.exception.GlobalExceptionHandler;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Batch.BatchStatus;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.service.BatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BatchController.class)
@Import(GlobalExceptionHandler.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchService batchService;

    @MockBean
    private DtoMapper dtoMapper;

    private Batch testBatch;
    private BatchResponse testResponse;

    @BeforeEach
    void setUp() {
        testBatch = new Batch();
        testBatch.setBatchId("B-2026-001");
        testBatch.setHarvestDate(LocalDate.of(2026, 8, 15));
        testBatch.setMangoVariety(MangoVariety.KEITT);
        testBatch.setHarvestQuantityKg(1000.0);
        testBatch.setFarmId("FARM-001");
        testBatch.setBlockId("BLOCK-001");
        testBatch.setCurrentStatus(BatchStatus.CREATED);

        testResponse = BatchResponse.builder()
                .batchId("B-2026-001")
                .harvestDate(LocalDate.of(2026, 8, 15))
                .mangoVariety(MangoVariety.KEITT)
                .harvestQuantityKg(1000.0)
                .farmId("FARM-001")
                .blockId("BLOCK-001")
                .currentStatus(BatchStatus.CREATED)
                .build();
    }

    @Test
    void createBatch_success() throws Exception {
        BatchCreateRequest request = new BatchCreateRequest(
                "B-2026-001",
                LocalDate.of(2026, 8, 15),
                MangoVariety.KEITT,
                1000.0,
                "FARM-001",
                "BLOCK-001",
                null
        );

        when(dtoMapper.toEntity(any(BatchCreateRequest.class))).thenReturn(testBatch);
        when(batchService.createBatch(any(Batch.class))).thenReturn(testBatch);
        when(dtoMapper.toResponse(testBatch)).thenReturn(testResponse);

        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId").value("B-2026-001"))
                .andExpect(jsonPath("$.currentStatus").value("CREATED"))
                .andExpect(jsonPath("$.mangoVariety").value("KEITT"));

        verify(batchService).createBatch(any(Batch.class));
    }

    @Test
    void createBatch_validationFailure() throws Exception {
        BatchCreateRequest request = new BatchCreateRequest(
                "",
                null,
                null,
                -1.0,
                "",
                "",
                null
        );

        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    void getBatchById_success() throws Exception {
        when(batchService.getBatchById("B-2026-001")).thenReturn(testBatch);
        when(dtoMapper.toResponse(testBatch)).thenReturn(testResponse);

        mockMvc.perform(get("/api/batches/B-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value("B-2026-001"));
    }

    @Test
    void getBatchById_notFound() throws Exception {
        when(batchService.getBatchById("MISSING"))
                .thenThrow(new IllegalArgumentException("Batch not found with ID: MISSING"));

        mockMvc.perform(get("/api/batches/MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Batch not found with ID: MISSING"));
    }

    @Test
    void advanceBatchStatus_success() throws Exception {
        testBatch.setCurrentStatus(BatchStatus.INTAKE);
        testResponse = BatchResponse.builder()
                .batchId("B-2026-001")
                .currentStatus(BatchStatus.INTAKE)
                .build();

        when(batchService.advanceBatchStatus("B-2026-001")).thenReturn(testBatch);
        when(dtoMapper.toResponse(testBatch)).thenReturn(testResponse);

        mockMvc.perform(post("/api/batches/B-2026-001/advance-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("INTAKE"));
    }

    @Test
    void advanceBatchStatus_invalidTransition() throws Exception {
        when(batchService.advanceBatchStatus("B-2026-001"))
                .thenThrow(new IllegalArgumentException("Cannot advance batch status: Cannot advance status from SHIPPED"));

        mockMvc.perform(post("/api/batches/B-2026-001/advance-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void getAllBatches_success() throws Exception {
        when(batchService.getAllBatches()).thenReturn(List.of(testBatch));
        when(dtoMapper.toResponse(testBatch)).thenReturn(testResponse);

        mockMvc.perform(get("/api/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchId").value("B-2026-001"));
    }

    @Test
    void deleteBatch_success() throws Exception {
        mockMvc.perform(delete("/api/batches/B-2026-001"))
                .andExpect(status().isNoContent());

        verify(batchService).deleteBatch("B-2026-001");
    }

    @Test
    void setBatchStatus_success() throws Exception {
        testBatch.setCurrentStatus(BatchStatus.REJECTED);
        testResponse = BatchResponse.builder()
                .batchId("B-2026-001")
                .currentStatus(BatchStatus.REJECTED)
                .build();

        when(batchService.setBatchStatus(eq("B-2026-001"), eq(BatchStatus.REJECTED))).thenReturn(testBatch);
        when(dtoMapper.toResponse(testBatch)).thenReturn(testResponse);

        mockMvc.perform(put("/api/batches/B-2026-001/status")
                        .param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("REJECTED"));
    }
}
