package com.sustainablefarm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.PackagingRecordCreateRequest;
import com.sustainablefarm.dto.response.PackagingRecordResponse;
import com.sustainablefarm.exception.GlobalExceptionHandler;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.PackagingRecord;
import com.sustainablefarm.model.PackagingRecord.PackageType;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.repository.EquipmentRepository;
import com.sustainablefarm.repository.OperatorRepository;
import com.sustainablefarm.service.PackagingRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PackagingRecordController.class)
@Import(GlobalExceptionHandler.class)
class PackagingRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PackagingRecordService packagingRecordService;

    @MockBean
    private BatchRepository batchRepository;

    @MockBean
    private EquipmentRepository equipmentRepository;

    @MockBean
    private OperatorRepository operatorRepository;

    @MockBean
    private DtoMapper dtoMapper;

    private Batch testBatch;
    private PackagingRecord testRecord;
    private PackagingRecordResponse testResponse;

    @BeforeEach
    void setUp() {
        testBatch = new Batch();
        testBatch.setBatchId("B-2026-001");

        testRecord = new PackagingRecord();
        testRecord.setRecordId("PKG-001");
        testRecord.setBatch(testBatch);
        testRecord.setPackageType(PackageType.BULK);
        testRecord.setPackageQuantityKg(500.0);
        testRecord.setLotCode("LOT-2026-001");
        testRecord.setExportReady(false);
        testRecord.setPackagingDate(LocalDate.of(2026, 8, 17));

        testResponse = PackagingRecordResponse.builder()
                .recordId("PKG-001")
                .batchId("B-2026-001")
                .packageType(PackageType.BULK)
                .packageQuantityKg(500.0)
                .lotCode("LOT-2026-001")
                .exportReady(false)
                .packagingDate(LocalDate.of(2026, 8, 17))
                .build();
    }

    @Test
    void createPackagingRecord_success() throws Exception {
        PackagingRecordCreateRequest request = new PackagingRecordCreateRequest(
                "PKG-001", "B-2026-001", PackageType.BULK, 500.0,
                "LOT-2026-001", false, LocalDate.of(2026, 8, 17), null, null);

        when(batchRepository.findById("B-2026-001")).thenReturn(Optional.of(testBatch));
        when(dtoMapper.toEntity(any(PackagingRecordCreateRequest.class), any(Batch.class), any(), any()))
                .thenReturn(testRecord);
        when(packagingRecordService.createPackagingRecord(any(PackagingRecord.class))).thenReturn(testRecord);
        when(dtoMapper.toResponse(testRecord)).thenReturn(testResponse);

        mockMvc.perform(post("/api/packaging-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordId").value("PKG-001"))
                .andExpect(jsonPath("$.batchId").value("B-2026-001"))
                .andExpect(jsonPath("$.lotCode").value("LOT-2026-001"));
    }

    @Test
    void createPackagingRecord_validationFailure() throws Exception {
        PackagingRecordCreateRequest request = new PackagingRecordCreateRequest(
                "", "", null, -1.0, "", null, null, null, null);

        mockMvc.perform(post("/api/packaging-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createPackagingRecord_batchNotFound() throws Exception {
        PackagingRecordCreateRequest request = new PackagingRecordCreateRequest(
                "PKG-001", "MISSING", PackageType.BULK, 500.0,
                "LOT-2026-001", false, LocalDate.of(2026, 8, 17), null, null);

        when(batchRepository.findById("MISSING")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/packaging-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createPackagingRecord_businessRuleViolation() throws Exception {
        PackagingRecordCreateRequest request = new PackagingRecordCreateRequest(
                "PKG-001", "B-2026-001", PackageType.BULK, 500.0,
                "LOT-2026-001", false, LocalDate.of(2026, 8, 17), null, null);

        when(batchRepository.findById("B-2026-001")).thenReturn(Optional.of(testBatch));
        when(dtoMapper.toEntity(any(PackagingRecordCreateRequest.class), any(Batch.class), any(), any()))
                .thenReturn(testRecord);
        when(packagingRecordService.createPackagingRecord(any(PackagingRecord.class)))
                .thenThrow(new IllegalArgumentException("Lot code already exists: LOT-2026-001"));

        mockMvc.perform(post("/api/packaging-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
    }
}
