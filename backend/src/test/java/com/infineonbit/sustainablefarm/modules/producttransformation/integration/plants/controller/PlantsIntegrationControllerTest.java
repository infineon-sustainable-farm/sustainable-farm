package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.controller;

import com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service.DtoMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.QualityGrade;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service.PlantsHarvestImportService;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service.PlantsIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route-level contract for the Plants proxy and the import endpoint.
 */
class PlantsIntegrationControllerTest {

    private PlantsIntegrationService plantsService;
    private PlantsHarvestImportService importService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        plantsService = mock(PlantsIntegrationService.class);
        importService = mock(PlantsHarvestImportService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlantsIntegrationController(plantsService, importService, new DtoMapper()))
                .build();
    }

    private static HarvestEvent event() {
        HarvestEvent event = new HarvestEvent();
        event.setHarvestId("H-PL-1");
        event.setBatchId("PT-BATCH-1");
        event.setLotNumber("LOT-2026-A-001");
        event.setPlantsHarvestId(1L);
        event.setHarvestDate(LocalDate.of(2026, 6, 20));
        event.setMangoVariety(HarvestEvent.MangoVariety.KEITT);
        event.setQualityGrade(QualityGrade.A);
        event.setFarmId("1");
        event.setBlockId("A");
        return event;
    }

    @Test
    @DisplayName("the proxy returns the Plants lot number")
    void proxyReturnsTheLotNumber() throws Exception {
        when(plantsService.getHarvestById(1L)).thenReturn(PlantsHarvestResponse.builder()
                .id(1L)
                .lotNumber("LOT-2026-A-001")
                .blockCode("A")
                .varietyName("Keitt")
                .harvestDate("2026-06-20")
                .quantityKg(7500.0)
                .build());

        mockMvc.perform(get("/api/harvest-events/plants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotNumber").value("LOT-2026-A-001"));
    }

    @Test
    @DisplayName("the block route also returns the Plants lot number")
    void blockRouteReturnsTheLotNumber() throws Exception {
        when(plantsService.getHarvestsByBlock("A")).thenReturn(List.of(PlantsHarvestResponse.builder()
                .id(1L)
                .lotNumber("LOT-2026-A-001")
                .blockCode("A")
                .build()));

        mockMvc.perform(get("/api/harvest-events/plants/block/A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lotNumber").value("LOT-2026-A-001"));
    }

    @Test
    @DisplayName("the first import answers 201")
    void firstImportIsCreated() throws Exception {
        when(importService.importHarvest(1L, QualityGrade.A))
                .thenReturn(new PlantsHarvestImportService.ImportOutcome(event(), true));

        mockMvc.perform(post("/api/harvest-events/plants/import/1").param("qualityGrade", "A"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plantsHarvestId").value(1))
                .andExpect(jsonPath("$.lotNumber").value("LOT-2026-A-001"))
                .andExpect(jsonPath("$.batchId").value("PT-BATCH-1"));
    }

    @Test
    @DisplayName("a repeated import answers 200, not a duplicate")
    void repeatedImportIsOk() throws Exception {
        when(importService.importHarvest(1L, QualityGrade.A))
                .thenReturn(new PlantsHarvestImportService.ImportOutcome(event(), false));

        mockMvc.perform(post("/api/harvest-events/plants/import/1").param("qualityGrade", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.harvestId").value("H-PL-1"));
    }

    @Test
    @DisplayName("the grade is required, since Plants does not record one")
    void gradeIsRequired() throws Exception {
        mockMvc.perform(post("/api/harvest-events/plants/import/1"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(importService);
    }

    @Test
    @DisplayName("an unknown grade is a 400")
    void unknownGradeIsRejected() throws Exception {
        mockMvc.perform(post("/api/harvest-events/plants/import/1").param("qualityGrade", "Z"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(importService);
    }

    @Test
    @DisplayName("the controller never writes on a read route")
    void readRoutesDoNotImport() throws Exception {
        when(plantsService.getAllHarvests()).thenReturn(List.of());

        mockMvc.perform(get("/api/harvest-events/plants/all")).andExpect(status().isOk());

        org.mockito.Mockito.verifyNoInteractions(importService);
    }
}
