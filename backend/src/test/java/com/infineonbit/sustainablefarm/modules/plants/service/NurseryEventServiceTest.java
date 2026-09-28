package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryLossRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.StageChangeRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryEventResponse;
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
public class NurseryEventServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");
    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);
    private static final LocalDate MARCH_2 = LocalDate.of(2026, 3, 2);
    private static final LocalDate MARCH_25 = LocalDate.of(2026, 3, 25);
    private static final LocalDate SEPTEMBER_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEPTEMBER_15 = LocalDate.of(2026, 9, 15);
    private static final LocalDate SEPTEMBER_18 = LocalDate.of(2026, 9, 18);

    @Mock
    private NurseryBatchRepository nurseryBatchRepository;

    @Mock
    private NurseryEventRepository nurseryEventRepository;

    @InjectMocks
    private NurseryEventService nurseryEventService;

    /** Batch P1, locked when read: 150 plants sown on March 2. */
    private NurseryBatch p1InDatabase() {
        NurseryBatch p1 = new NurseryBatch(1L, null, "P1", "Keitt", NurseryOrigin.IN_HOUSE, null, null, MARCH_2, 150,
                LocalDate.of(2026, 9, 20), "E", "user_entry", NOW);
        when(nurseryBatchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p1));
        return p1;
    }

    private static NurseryEvent event(long id, NurseryBatch batch, NurseryEventType type, LocalDate date,
                                      NurseryStage stage, Integer quantity) {
        return new NurseryEvent(id, batch, type, date, stage, quantity, null, null, null, "user_entry", NOW);
    }

    /** P1 grafted on September 1, with 12 plants lost since: 138 left. */
    private void graftedWith138Left(NurseryBatch p1) {
        when(nurseryEventRepository.findByBatchIds(List.of(1L))).thenReturn(List.of(
                event(10, p1, NurseryEventType.STAGE_CHANGE, MARCH_2, NurseryStage.GERMINATION, null),
                event(11, p1, NurseryEventType.STAGE_CHANGE, MARCH_25, NurseryStage.ROOTSTOCK_GROWTH, null),
                event(12, p1, NurseryEventType.STAGE_CHANGE, SEPTEMBER_1, NurseryStage.GRAFTED, null),
                event(13, p1, NurseryEventType.LOSS, SEPTEMBER_15, null, 12)));
    }

    /** Saving gives the event an identifier, as the database would. */
    private void saveAssignsId() {
        when(nurseryEventRepository.save(any(NurseryEvent.class))).thenAnswer(invocation -> {
            NurseryEvent event = invocation.getArgument(0);
            event.setId(20L);
            return event;
        });
    }

    private NurseryEvent savedEvent() {
        ArgumentCaptor<NurseryEvent> captor = ArgumentCaptor.forClass(NurseryEvent.class);
        verify(nurseryEventRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordStageChange_shouldStoreTheEvent_afterLockingTheBatch() {
        // Arrange
        NurseryBatch p1 = p1InDatabase();
        graftedWith138Left(p1);
        saveAssignsId();
        // Act
        NurseryEventResponse response = nurseryEventService.recordStageChange(1L,
                new StageChangeRequest(" ready_to_transplant ", SEPTEMBER_18), NOW);
        // Assert: the stored event, of the locked batch
        NurseryEvent stageChange = savedEvent();
        assertSame(p1, stageChange.getBatch());
        assertEquals(NurseryEventType.STAGE_CHANGE, stageChange.getEventType());
        assertEquals(SEPTEMBER_18, stageChange.getEventDate());
        assertEquals(NurseryStage.READY_TO_TRANSPLANT, stageChange.getStage());
        assertNull(stageChange.getQuantity());
        assertNull(stageChange.getReason());
        assertEquals("user_entry", stageChange.getSource());
        assertEquals(NOW, stageChange.getLastUpdated());
        verify(nurseryBatchRepository, never()).findById(any());
        // Assert: the response
        assertEquals(new NurseryEventResponse(20L, 1L, "P1", null, NurseryEventType.STAGE_CHANGE, SEPTEMBER_18,
                NurseryStage.READY_TO_TRANSPLANT, null, null, null, null, "user_entry", NOW), response);
    }

    @Test
    void recordStageChange_shouldAcceptABackwardStage() {
        // Arrange: the graft did not take
        NurseryBatch p1 = p1InDatabase();
        graftedWith138Left(p1);
        saveAssignsId();
        // Act
        nurseryEventService.recordStageChange(1L, new StageChangeRequest("ROOTSTOCK_GROWTH", SEPTEMBER_15), NOW);
        // Assert
        assertEquals(NurseryStage.ROOTSTOCK_GROWTH, savedEvent().getStage());
    }

    @Test
    void recordStageChange_shouldAcceptTheDayOfTheLastChange() {
        // Arrange
        NurseryBatch p1 = p1InDatabase();
        graftedWith138Left(p1);
        saveAssignsId();
        // Act
        nurseryEventService.recordStageChange(1L, new StageChangeRequest("HARDENING", SEPTEMBER_1), NOW);
        // Assert
        assertEquals(SEPTEMBER_1, savedEvent().getEventDate());
    }

    @Test
    void recordStageChange_shouldRefuseADateBeforeTheStart() {
        // Arrange
        p1InDatabase();
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> nurseryEventService.recordStageChange(1L, new StageChangeRequest("HARDENING", MARCH_1), NOW));
        // Assert
        assertEquals("The stage change date 2026-03-01 is before the start date 2026-03-02 of batch P1",
                exception.getMessage());
        verify(nurseryEventRepository, never()).save(any());
    }

    @Test
    void recordStageChange_shouldRefuseADateBeforeTheLastChange_beforeLookingAtTheStage() {
        // Arrange: GRAFTED is also the current stage, but the date is checked first
        NurseryBatch p1 = p1InDatabase();
        graftedWith138Left(p1);
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> nurseryEventService.recordStageChange(1L,
                        new StageChangeRequest("GRAFTED", LocalDate.of(2026, 8, 1)), NOW));
        // Assert
        assertEquals("The stage change date 2026-08-01 is before the last stage change 2026-09-01 of batch P1",
                exception.getMessage());
        verify(nurseryEventRepository, never()).save(any());
    }

    @Test
    void recordStageChange_shouldRefuseTheCurrentStage() {
        // Arrange
        NurseryBatch p1 = p1InDatabase();
        graftedWith138Left(p1);
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> nurseryEventService.recordStageChange(1L,
                        new StageChangeRequest(" grafted ", LocalDate.of(2026, 9, 10)), NOW));
        // Assert
        assertEquals("Batch P1 is already at stage GRAFTED", exception.getMessage());
        verify(nurseryEventRepository, never()).save(any());
    }

    @Test
    void recordLoss_shouldStoreTheLossWithATrimmedReason() {
        // Arrange
        NurseryBatch p1 = p1InDatabase();
        graftedWith138Left(p1);
        saveAssignsId();
        // Act
        NurseryEventResponse response = nurseryEventService.recordLoss(1L,
                new NurseryLossRequest(SEPTEMBER_18, 12.0, " Graft failure "), NOW);
        // Assert
        NurseryEvent loss = savedEvent();
        assertSame(p1, loss.getBatch());
        assertEquals(NurseryEventType.LOSS, loss.getEventType());
        assertEquals(SEPTEMBER_18, loss.getEventDate());
        assertNull(loss.getStage());
        assertEquals(12, loss.getQuantity());
        assertEquals("Graft failure", loss.getReason());
        assertNull(loss.getBlockCode());
        assertEquals("user_entry", loss.getSource());
        assertEquals(NOW, loss.getLastUpdated());
        assertEquals(new NurseryEventResponse(20L, 1L, "P1", null, NurseryEventType.LOSS, SEPTEMBER_18, null, 12,
                "Graft failure", null, null, "user_entry", NOW), response);
    }

    @Test
    void recordLoss_shouldRefuseMoreThanThePlantsLeft() {
        // Arrange
        NurseryBatch p1 = p1InDatabase();
        graftedWith138Left(p1);
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> nurseryEventService.recordLoss(1L, new NurseryLossRequest(SEPTEMBER_18, 139.0, "Drought"), NOW));
        // Assert
        assertEquals("Not enough plants in batch P1: 138 left, 139 requested", exception.getMessage());
        verify(nurseryEventRepository, never()).save(any());
    }

    @Test
    void recordLoss_shouldRefuseADateBeforeTheStart_beforeCountingThePlants() {
        // Arrange: too many plants too, but the date is checked first
        p1InDatabase();
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> nurseryEventService.recordLoss(1L, new NurseryLossRequest(MARCH_1, 200.0, "Drought"), NOW));
        // Assert
        assertEquals("The loss date 2026-03-01 is before the start date 2026-03-02 of batch P1",
                exception.getMessage());
        verify(nurseryEventRepository, never()).findByBatchIds(any());
        verify(nurseryEventRepository, never()).save(any());
    }

    @Test
    void recordLoss_shouldThrowNotFound_whenTheBatchIsUnknown() {
        // Arrange
        when(nurseryBatchRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());
        // Act
        NurseryBatchNotFoundException exception = assertThrows(NurseryBatchNotFoundException.class,
                () -> nurseryEventService.recordLoss(999L, new NurseryLossRequest(SEPTEMBER_18, 1.0, "Drought"), NOW));
        // Assert
        assertEquals("Nursery batch with ID 999 not found", exception.getMessage());
        verify(nurseryEventRepository, never()).save(any());
    }
}
