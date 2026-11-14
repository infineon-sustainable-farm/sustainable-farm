package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.QualityGrade;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.repository.HarvestEventRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Plants harvest import: identity (option B) and idempotence.
 */
class PlantsHarvestImportServiceTest {

    private HarvestEventRepository repository;
    private PlantsIntegrationService plantsService;
    private PlantsHarvestImportService importService;

    @BeforeEach
    void setUp() {
        repository = mock(HarvestEventRepository.class);
        // Real mapper, network call stubbed: the mapping rules are part of what we test.
        plantsService = spy(new PlantsIntegrationService(null));
        importService = new PlantsHarvestImportService(plantsService, repository);
    }

    private PlantsHarvestResponse harvest(long id) {
        return PlantsHarvestResponse.builder()
                .id(id)
                .lotNumber("LOT-2026-A-001")
                .farmId(null)
                .blockCode("A")
                .varietyId(1L)
                .varietyName("Keitt")
                .harvestDate("2026-06-20")
                .quantityKg(7500.0)
                .source("user_entry")
                .build();
    }

    @Test
    @DisplayName("first import creates the event with a PT batch id and the Plants lot")
    void createsOnFirstImport() {
        when(repository.findByPlantsHarvestId(1L)).thenReturn(Optional.empty());
        doReturn(harvest(1L)).when(plantsService).getHarvestById(1L);
        when(repository.save(any(HarvestEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlantsHarvestImportService.ImportOutcome outcome = importService.importHarvest(1L, QualityGrade.A);

        assertTrue(outcome.created());
        HarvestEvent saved = outcome.event();
        assertEquals("H-PL-1", saved.getHarvestId());
        assertEquals("PT-BATCH-1", saved.getBatchId(), "option B: PT owns the batch id");
        assertEquals("LOT-2026-A-001", saved.getLotNumber(), "option B: the Plants lot is kept");
        assertEquals(1L, saved.getPlantsHarvestId());
        assertEquals(QualityGrade.A, saved.getQualityGrade());
        assertEquals("KEITT", saved.getMangoVariety().name(), "Plants spelling is accepted");
        assertEquals("A", saved.getBlockId());
        assertNull(saved.getFarmId(), "Plants has no farm id, so none may be invented");
        assertEquals(0, saved.getHarvestQuantityKg().compareTo(java.math.BigDecimal.valueOf(7500.0)));
    }

    @Test
    @DisplayName("re-importing the same harvest returns the existing event, writes nothing")
    void isIdempotent() {
        HarvestEvent existing = new HarvestEvent();
        existing.setHarvestId("H-PL-1");
        existing.setBatchId("PT-BATCH-1");
        existing.setPlantsHarvestId(1L);
        when(repository.findByPlantsHarvestId(1L)).thenReturn(Optional.of(existing));

        PlantsHarvestImportService.ImportOutcome outcome = importService.importHarvest(1L, QualityGrade.B);

        assertFalse(outcome.created());
        assertSame(existing, outcome.event());
        verify(plantsService, never()).getHarvestById(anyLong());
        verify(repository, never()).save(any(HarvestEvent.class));
    }

    @Test
    @DisplayName("a concurrent duplicate is recovered, not surfaced as a 500")
    void recoversFromConcurrentDuplicate() {
        HarvestEvent winner = new HarvestEvent();
        winner.setHarvestId("H-PL-1");
        winner.setBatchId("PT-BATCH-1");
        winner.setPlantsHarvestId(1L);

        when(repository.findByPlantsHarvestId(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        doReturn(harvest(1L)).when(plantsService).getHarvestById(1L);
        when(repository.save(any(HarvestEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        PlantsHarvestImportService.ImportOutcome outcome = importService.importHarvest(1L, QualityGrade.A);

        assertFalse(outcome.created());
        assertSame(winner, outcome.event());
    }

    @Test
    @DisplayName("an unrelated constraint violation surfaces as itself, not as a duplicate")
    void rethrowsOtherIntegrityViolations() {
        when(repository.findByPlantsHarvestId(1L)).thenReturn(Optional.empty());
        doReturn(harvest(1L)).when(plantsService).getHarvestById(1L);

        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("null value in column farm_id violates not-null constraint");
        when(repository.save(any(HarvestEvent.class))).thenThrow(failure);

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> importService.importHarvest(1L, QualityGrade.A));

        assertSame(failure, thrown);
    }

    @Test
    @DisplayName("the Plants id and lot number are the values actually persisted")
    void persistsThePlantsIdentity() {
        when(repository.findByPlantsHarvestId(7L)).thenReturn(Optional.empty());
        doReturn(harvest(7L)).when(plantsService).getHarvestById(7L);
        when(repository.save(any(HarvestEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        importService.importHarvest(7L, QualityGrade.C);

        ArgumentCaptor<HarvestEvent> captor = ArgumentCaptor.forClass(HarvestEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("LOT-2026-A-001", captor.getValue().getLotNumber());
        assertEquals(7L, captor.getValue().getPlantsHarvestId());
        assertEquals("H-PL-7", captor.getValue().getHarvestId());
        assertEquals("PT-BATCH-7", captor.getValue().getBatchId());
    }

    @Test
    void refusesAnUnknownPlantsHarvest() {
        when(repository.findByPlantsHarvestId(99L)).thenReturn(Optional.empty());
        doReturn(null).when(plantsService).getHarvestById(99L);

        assertThrows(ResourceNotFoundException.class,
                () -> importService.importHarvest(99L, QualityGrade.A));
    }

    @Test
    void requiresAQualityGrade() {
        assertThrows(IllegalArgumentException.class,
                () -> importService.importHarvest(1L, null));
    }
}
