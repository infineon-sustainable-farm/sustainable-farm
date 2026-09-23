package com.sustainablefarm.modules.producttransformation.resources.packagingrecord.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.service.DtoMapper;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.dto.request.PackagingRecordCreateRequest;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.dto.response.PackagingRecordResponse;
import com.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord.PackageType;
import com.sustainablefarm.modules.producttransformation.resources.batch.repository.BatchRepository;
import com.sustainablefarm.modules.producttransformation.resources.equipment.repository.EquipmentRepository;
import com.sustainablefarm.modules.producttransformation.resources.operator.repository.OperatorRepository;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.service.PackagingRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PackagingRecordControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PackagingRecordService packagingRecordService;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private DtoMapper dtoMapper;

    @InjectMocks
    private PackagingRecordController packagingRecordController;

    private ObjectMapper objectMapper;
    private Batch testBatch;
    private PackagingRecord testRecord;
    private PackagingRecordResponse testResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(packagingRecordController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testBatch = new Batch();
        testBatch.setBatchId("B-2026-001");

        testRecord = new PackagingRecord();
        testRecord.setRecordId("PKG-001");
        testRecord.setBatch(testBatch);
        testRecord.setPackageType(PackageType.BULK);
        testRecord.setPackageQuantityKg(new java.math.BigDecimal("500.0"));
        testRecord.setLotCode("LOT-2026-001");
        testRecord.setExportReady(false);
        testRecord.setPackagingDate(LocalDate.of(2026, 8, 17));

        testResponse = PackagingRecordResponse.builder()
                .recordId("PKG-001")
                .batchId("B-2026-001")
                .packageType(PackageType.BULK)
                .packageQuantityKg(new java.math.BigDecimal("500.0"))
                .lotCode("LOT-2026-001")
                .exportReady(false)
                .packagingDate(LocalDate.of(2026, 8, 17))
                .build();
    }

    @Test
    void createPackagingRecord_success() throws Exception {
        PackagingRecordCreateRequest request = new PackagingRecordCreateRequest(
                "PKG-001", "B-2026-001", PackageType.BULK, new java.math.BigDecimal("500.0"),
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
                "", "", null, new java.math.BigDecimal("-1.0"), "", null, null, null, null);

        mockMvc.perform(post("/api/packaging-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createPackagingRecord_batchNotFound() throws Exception {
        PackagingRecordCreateRequest request = new PackagingRecordCreateRequest(
                "PKG-001", "MISSING", PackageType.BULK, new java.math.BigDecimal("500.0"),
                "LOT-2026-001", false, LocalDate.of(2026, 8, 17), null, null);

        when(batchRepository.findById("MISSING")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/packaging-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"));
    }

    @Test
    void createPackagingRecord_businessRuleViolation() throws Exception {
        PackagingRecordCreateRequest request = new PackagingRecordCreateRequest(
                "PKG-001", "B-2026-001", PackageType.BULK, new java.math.BigDecimal("500.0"),
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
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"));
    }
}