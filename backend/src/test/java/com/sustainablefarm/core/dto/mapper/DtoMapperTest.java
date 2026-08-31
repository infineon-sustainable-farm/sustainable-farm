package com.sustainablefarm.core.dto.mapper;

import com.sustainablefarm.core.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.BatchCreateRequest;
import com.sustainablefarm.dto.response.BatchResponse;
import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Batch.BatchStatus;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DtoMapperTest {

    private DtoMapper dtoMapper;

    @BeforeEach
    void setUp() {
        dtoMapper = new DtoMapper();
    }

    @Test
    void batchCreateRequestToEntity() {
        BatchCreateRequest request = new BatchCreateRequest(
                "B-2026-001",
                LocalDate.of(2026, 8, 15),
                MangoVariety.KEITT,
                new java.math.BigDecimal("1000.0"),
                "FARM-001",
                "BLOCK-001",
                BatchStatus.CREATED
        );

        Batch entity = dtoMapper.toEntity(request);

        assertEquals("B-2026-001", entity.getBatchId());
        assertEquals(MangoVariety.KEITT, entity.getMangoVariety());
        assertEquals(BatchStatus.CREATED, entity.getCurrentStatus());
    }

    @Test
    void batchEntityToResponse_doesNotExposeRelationships() {
        Batch entity = new Batch();
        entity.setBatchId("B-2026-001");
        entity.setHarvestDate(LocalDate.of(2026, 8, 15));
        entity.setMangoVariety(MangoVariety.KEITT);
        entity.setHarvestQuantityKg(new java.math.BigDecimal("1000.0"));
        entity.setFarmId("FARM-001");
        entity.setBlockId("BLOCK-001");
        entity.setCurrentStatus(BatchStatus.CREATED);

        BatchResponse response = dtoMapper.toResponse(entity);

        assertEquals("B-2026-001", response.getBatchId());
        assertNotNull(response.getMangoVariety());
        assertEquals(BatchStatus.CREATED, response.getCurrentStatus());
    }
}
