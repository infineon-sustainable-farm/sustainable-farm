package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryBatchResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryBatch;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import com.infineonbit.sustainablefarm.modules.plants.exception.NurseryBatchNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.NurseryBatchRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.NurseryEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NurseryBatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final LocalDate MARCH_2 = LocalDate.of(2026, 3, 2);
    private static final LocalDate JULY_1 = LocalDate.of(2026, 7, 1);
    private static final LocalDate SEPTEMBER_15 = LocalDate.of(2026, 9, 15);
    private static final LocalDate SEPTEMBER_20 = LocalDate.of(2026, 9, 20);
    private static final LocalDate OCTOBER_10 = LocalDate.of(2026, 10, 10);

    @Mock
    private NurseryBatchRepository nurseryBatchRepository;

    @Mock
    private NurseryEventRepository nurseryEventRepository;

    @InjectMocks
    private NurseryBatchService nurseryBatchService;

    private static NurseryBatchRequest inHouseP1() {
        return new NurseryBatchRequest(null, " p1 ", " Keitt ", " in_house ", null, null, MARCH_2, 150.0,
                " germination ", SEPTEMBER_20, " e ");
    }

    private static NurseryBatchRequest purchasedP2(String supplierLotNumber, String plannedBlockCode,
                                                   LocalDate plannedTransplantOn) {
        return new NurseryBatchRequest(2, "p2", "Keitt", "PURCHASED", " Test nursery ", supplierLotNumber, JULY_1,
                200.0, "hardening", plannedTransplantOn, plannedBlockCode);
    }

    private static NurseryBatch p1() {
        return new NurseryBatch(1L, null, "P1", "Keitt", NurseryOrigin.IN_HOUSE, null, null, MARCH_2, 150,
                SEPTEMBER_20, "E", "user_entry", NOW);
    }

    private static NurseryBatch p2() {
        return new NurseryBatch(2L, null, "P2", "Keitt", NurseryOrigin.PURCHASED, "Test nursery", "L-2026-07",
                JULY_1, 200, OCTOBER_10, "F", "user_entry", NOW);
    }

    private static NurseryEvent event(long id, NurseryBatch batch, NurseryEventType type, LocalDate date,
                                      NurseryStage stage, Integer quantity) {
        return new NurseryEvent(id, batch, type, date, stage, quantity, null, null, null, "user_entry", NOW);
    }

    /** P1 at germination with 12 plants lost, and P2 at hardening. */
    private static List<NurseryEvent> eventsOf(NurseryBatch p1, NurseryBatch p2) {
        return List.of(
                event(10, p1, NurseryEventType.STAGE_CHANGE, MARCH_2, NurseryStage.GERMINATION, null),
                event(11, p2, NurseryEventType.STAGE_CHANGE, JULY_1, NurseryStage.HARDENING, null),
                event(12, p1, NurseryEventType.LOSS, SEPTEMBER_15, null, 12));
    }

    /** Saving gives the batch and its first event an identifier, as the database would. */
    private void saveAssignsIds() {
        when(nurseryBatchRepository.save(any(NurseryBatch.class))).thenAnswer(invocation -> {
            NurseryBatch batch = invocation.getArgument(0);
            batch.setId(1L);
            return batch;
        });
        when(nurseryEventRepository.save(any(NurseryEvent.class))).thenAnswer(invocation -> {
            NurseryEvent event = invocation.getArgument(0);
            event.setId(10L);
            return event;
        });
    }

    private NurseryBatch savedBatch() {
        ArgumentCaptor<NurseryBatch> captor = ArgumentCaptor.forClass(NurseryBatch.class);
        verify(nurseryBatchRepository).save(captor.capture());
        return captor.getValue();
    }

    private NurseryEvent savedEvent() {
        ArgumentCaptor<NurseryEvent> captor = ArgumentCaptor.forClass(NurseryEvent.class);
        verify(nurseryEventRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void createBatch_shouldStoreANormalizedInHouseBatch_withItsInitialStageAsTheFirstEvent() {
        // Arrange
        when(nurseryBatchRepository.findByFarmAndCode(null, "P1")).thenReturn(List.of());
        saveAssignsIds();
        // Act
        NurseryBatchResponse response = nurseryBatchService.createBatch(inHouseP1(), NOW);
        // Assert: the stored batch
        NurseryBatch batch = savedBatch();
        assertNull(batch.getFarmId());
        assertEquals("P1", batch.getBatchCode());
        assertEquals("Keitt", batch.getVarietyName());
        assertEquals(NurseryOrigin.IN_HOUSE, batch.getOrigin());
        assertNull(batch.getSupplier());
        assertNull(batch.getSupplierLotNumber());
        assertEquals(MARCH_2, batch.getStartedOn());
        assertEquals(150, batch.getInitialCount());
        assertEquals(SEPTEMBER_20, batch.getPlannedTransplantOn());
        assertEquals("E", batch.getPlannedBlockCode());
        assertEquals("user_entry", batch.getSource());
        assertEquals(NOW, batch.getLastUpdated());
        // Assert: the initial stage, stored as the first stage change, on the start date
        NurseryEvent initialStage = savedEvent();
        assertSame(batch, initialStage.getBatch());
        assertEquals(NurseryEventType.STAGE_CHANGE, initialStage.getEventType());
        assertEquals(MARCH_2, initialStage.getEventDate());
        assertEquals(NurseryStage.GERMINATION, initialStage.getStage());
        assertNull(initialStage.getQuantity());
        assertEquals("user_entry", initialStage.getSource());
        assertEquals(NOW, initialStage.getLastUpdated());
        // Assert: the response, with its computed values
        assertEquals(new NurseryBatchResponse(1L, null, "P1", "Keitt", NurseryOrigin.IN_HOUSE, null, null, MARCH_2,
                150, SEPTEMBER_20, "E", NurseryStage.GERMINATION, MARCH_2, 0, 0, 150, 150, 100.0, "user_entry",
                NOW), response);
    }

    @Test
    void createBatch_shouldStoreTheSupplierAndLotNumber_ofAPurchasedBatch() {
        // Arrange
        when(nurseryBatchRepository.findByFarmAndCode(2, "P2")).thenReturn(List.of());
        saveAssignsIds();
        // Act
        NurseryBatchResponse response = nurseryBatchService.createBatch(
                purchasedP2(" L-2026-07 ", "F", OCTOBER_10), NOW);
        // Assert
        NurseryBatch batch = savedBatch();
        assertEquals(2, batch.getFarmId());
        assertEquals(NurseryOrigin.PURCHASED, batch.getOrigin());
        assertEquals("Test nursery", batch.getSupplier());
        assertEquals("L-2026-07", batch.getSupplierLotNumber());
        assertEquals(NurseryStage.HARDENING, savedEvent().getStage());
        assertEquals(NurseryStage.HARDENING, response.currentStage());
        assertEquals(JULY_1, response.currentStageSince());
    }

    @Test
    void createBatch_shouldTreatABlankLotNumberAndPlannedBlockAsNone_andAcceptAPlannedDateOnTheStartDate() {
        // Arrange
        when(nurseryBatchRepository.findByFarmAndCode(2, "P2")).thenReturn(List.of());
        saveAssignsIds();
        // Act
        nurseryBatchService.createBatch(purchasedP2("  ", " ", JULY_1), NOW);
        // Assert
        NurseryBatch batch = savedBatch();
        assertNull(batch.getSupplierLotNumber());
        assertNull(batch.getPlannedBlockCode());
        assertEquals(JULY_1, batch.getPlannedTransplantOn());
    }

    @Test
    void createBatch_shouldRefuseACodeAlreadyUsedInTheFarm() {
        // Arrange: "p1" is looked for as stored, in the farm of the request
        when(nurseryBatchRepository.findByFarmAndCode(null, "P1")).thenReturn(List.of(p1()));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> nurseryBatchService.createBatch(inHouseP1(), NOW));
        // Assert
        assertEquals("A nursery batch with code P1 already exists", exception.getMessage());
        verify(nurseryBatchRepository, never()).save(any());
        verify(nurseryEventRepository, never()).save(any());
    }

    @Test
    void createBatch_shouldRefuseAPlannedDateBeforeTheStart() {
        // Arrange
        NurseryBatchRequest request = new NurseryBatchRequest(null, "P3", "Keitt", "IN_HOUSE", null, null, MARCH_2,
                150.0, "GERMINATION", LocalDate.of(2026, 3, 1), null);
        when(nurseryBatchRepository.findByFarmAndCode(null, "P3")).thenReturn(List.of());
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> nurseryBatchService.createBatch(request, NOW));
        // Assert: nothing is written
        assertEquals("The planned transplant date 2026-03-01 is before the start date 2026-03-02 of batch P3",
                exception.getMessage());
        verify(nurseryBatchRepository, never()).save(any());
        verify(nurseryEventRepository, never()).save(any());
    }

    @Test
    void getAllBatches_shouldComputeEachBatchFromOneEventQuery() {
        // Arrange
        NurseryBatch p1 = p1();
        NurseryBatch p2 = p2();
        when(nurseryBatchRepository.findByOptionalFarm(null)).thenReturn(List.of(p1, p2));
        when(nurseryEventRepository.findByBatchIds(List.of(1L, 2L))).thenReturn(eventsOf(p1, p2));
        // Act
        List<NurseryBatchResponse> responses = nurseryBatchService.getAllBatches(null, null);
        // Assert: in the order of the batches, each with its own events
        assertEquals(2, responses.size());
        assertEquals(new NurseryBatchResponse(1L, null, "P1", "Keitt", NurseryOrigin.IN_HOUSE, null, null, MARCH_2,
                150, SEPTEMBER_20, "E", NurseryStage.GERMINATION, MARCH_2, 12, 0, 138, 138, 92.0, "user_entry",
                NOW), responses.get(0));
        assertEquals(new NurseryBatchResponse(2L, null, "P2", "Keitt", NurseryOrigin.PURCHASED, "Test nursery",
                "L-2026-07", JULY_1, 200, OCTOBER_10, "F", NurseryStage.HARDENING, JULY_1, 0, 0, 200, 200, 100.0,
                "user_entry", NOW), responses.get(1));
    }

    @Test
    void getAllBatches_shouldKeepOnlyTheBatchesAtTheGivenStage() {
        // Arrange
        NurseryBatch p1 = p1();
        NurseryBatch p2 = p2();
        when(nurseryBatchRepository.findByOptionalFarm(null)).thenReturn(List.of(p1, p2));
        when(nurseryEventRepository.findByBatchIds(List.of(1L, 2L))).thenReturn(eventsOf(p1, p2));
        // Act
        List<NurseryBatchResponse> responses = nurseryBatchService.getAllBatches(null, NurseryStage.HARDENING);
        // Assert
        assertEquals(List.of("P2"), responses.stream().map(NurseryBatchResponse::batchCode).toList());
    }

    @Test
    void getAllBatches_shouldNotQueryTheEvents_whenThereIsNoBatch() {
        // Arrange
        when(nurseryBatchRepository.findByOptionalFarm(3)).thenReturn(List.of());
        // Act
        List<NurseryBatchResponse> responses = nurseryBatchService.getAllBatches(3, null);
        // Assert
        assertEquals(List.of(), responses);
        verify(nurseryEventRepository, never()).findByBatchIds(any());
    }

    @Test
    void getBatchById_shouldComputeTheBatchFromItsEvents() {
        // Arrange
        NurseryBatch p1 = p1();
        when(nurseryBatchRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(nurseryEventRepository.findByBatchIds(List.of(1L))).thenReturn(List.of(
                event(10, p1, NurseryEventType.STAGE_CHANGE, MARCH_2, NurseryStage.GERMINATION, null),
                event(12, p1, NurseryEventType.LOSS, SEPTEMBER_15, null, 12)));
        // Act
        NurseryBatchResponse response = nurseryBatchService.getBatchById(1L);
        // Assert
        assertEquals(138, response.currentCount());
        assertEquals(92.0, response.survivalRatePct());
        assertEquals(NurseryStage.GERMINATION, response.currentStage());
    }

    @Test
    void getBatchById_shouldThrowNotFound_whenTheBatchIsUnknown() {
        // Arrange
        when(nurseryBatchRepository.findById(999L)).thenReturn(Optional.empty());
        // Act
        NurseryBatchNotFoundException exception = assertThrows(NurseryBatchNotFoundException.class,
                () -> nurseryBatchService.getBatchById(999L));
        // Assert
        assertEquals("Nursery batch with ID 999 not found", exception.getMessage());
    }
}
