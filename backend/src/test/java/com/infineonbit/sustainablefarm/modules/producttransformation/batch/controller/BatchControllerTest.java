package com.infineonbit.sustainablefarm.modules.producttransformation.batch.controller;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.service.BatchService;
import com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service.DtoMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.repository.HarvestEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route-level contract for the batch endpoints: the Plants identity
 * (lot number, plants harvest id) is exposed on batch responses,
 * resolved from the linked harvest_event joined by batch_id.
 */
class BatchControllerTest {

    private BatchService batchService;
    private HarvestEventRepository harvestEventRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        batchService = mock(BatchService.class);
        harvestEventRepository = mock(HarvestEventRepository.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BatchController(batchService, new DtoMapper(), harvestEventRepository))
                .build();
    }

    private static Batch batch(String batchId) {
        Batch batch = new Batch();
        batch.setBatchId(batchId);
        batch.setHarvestDate(LocalDate.of(2026, 11, 12));
        batch.setMangoVariety(HarvestEvent.MangoVariety.KEITT);
        batch.setHarvestQuantityKg(new BigDecimal("750.00"));
        batch.setCurrentStatus(Batch.BatchStatus.CREATED);
        batch.setFarmId("FARM-1");
        batch.setBlockId("A");
        return batch;
    }

    private static HarvestEvent event(String batchId, String lotNumber, Long plantsHarvestId) {
        HarvestEvent event = new HarvestEvent();
        event.setHarvestId("H-" + batchId);
        event.setBatchId(batchId);
        event.setLotNumber(lotNumber);
        event.setPlantsHarvestId(plantsHarvestId);
        event.setHarvestDate(LocalDate.of(2026, 11, 12));
        event.setMangoVariety(HarvestEvent.MangoVariety.KEITT);
        event.setFarmId("FARM-1");
        event.setBlockId("A");
        return event;
    }

    @Test
    @DisplayName("GET /api/batches/{batchId} exposes the Plants identity when a harvest event is linked")
    void getByIdExposesPlantsIdentity() throws Exception {
        Batch batch = batch("PT-BATCH-1");
        when(batchService.getBatchById("PT-BATCH-1")).thenReturn(batch);
        when(harvestEventRepository.findByBatchId("PT-BATCH-1"))
                .thenReturn(java.util.Optional.of(event("PT-BATCH-1", "LOT-GP-20261112-221158", 42L)));

        mockMvc.perform(get("/api/batches/PT-BATCH-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value("PT-BATCH-1"))
                .andExpect(jsonPath("$.lotNumber").value("LOT-GP-20261112-221158"))
                .andExpect(jsonPath("$.plantsHarvestId").value(42));
    }

    @Test
    @DisplayName("GET /api/batches/{batchId} returns nulls for a batch without a harvest event")
    void getByIdWithoutHarvestEvent() throws Exception {
        Batch batch = batch("PT-BATCH-MANUAL");
        when(batchService.getBatchById("PT-BATCH-MANUAL")).thenReturn(batch);
        when(harvestEventRepository.findByBatchId("PT-BATCH-MANUAL")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/batches/PT-BATCH-MANUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value("PT-BATCH-MANUAL"))
                .andExpect(jsonPath("$.lotNumber").value(nullValue()))
                .andExpect(jsonPath("$.plantsHarvestId").value(nullValue()));
    }

    @Test
    @DisplayName("GET /api/batches resolves identities in one batched query and exposes them")
    void listBatchesResolveIdentitiesInBatch() throws Exception {
        Batch linked = batch("PT-BATCH-LINKED");
        Batch manual = batch("PT-BATCH-MANUAL");
        when(batchService.getAllBatches()).thenReturn(List.of(linked, manual));
        when(harvestEventRepository.findByBatchIdIn(anyCollection()))
                .thenReturn(List.of(event("PT-BATCH-LINKED", "LOT-GP-20261112-221158", 42L)));

        mockMvc.perform(get("/api/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchId").value("PT-BATCH-LINKED"))
                .andExpect(jsonPath("$[0].lotNumber").value("LOT-GP-20261112-221158"))
                .andExpect(jsonPath("$[0].plantsHarvestId").value(42))
                .andExpect(jsonPath("$[1].batchId").value("PT-BATCH-MANUAL"))
                .andExpect(jsonPath("$[1].lotNumber").value(nullValue()))
                .andExpect(jsonPath("$[1].plantsHarvestId").value(nullValue()));

        verify(harvestEventRepository, times(1)).findByBatchIdIn(eq(List.of("PT-BATCH-LINKED", "PT-BATCH-MANUAL")));
    }
}
