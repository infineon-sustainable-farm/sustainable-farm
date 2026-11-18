package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryBatch;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryOrigin;
import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryStage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class NurseryEventRepositoryTest {

    @Autowired
    private NurseryBatchRepository nurseryBatchRepository;

    @Autowired
    private NurseryEventRepository nurseryEventRepository;

    private NurseryBatch batch(Long farmId, String batchCode) {
        NurseryBatch batch = new NurseryBatch();
        batch.setFarmId(farmId);
        batch.setBatchCode(batchCode);
        batch.setVarietyName("Keitt");
        batch.setOrigin(NurseryOrigin.IN_HOUSE);
        batch.setStartedOn(LocalDate.of(2026, 3, 2));
        batch.setInitialCount(150);
        batch.setPlannedTransplantOn(LocalDate.of(2026, 9, 20));
        batch.setSource("user_entry");
        return nurseryBatchRepository.save(batch);
    }

    private NurseryEvent stageChange(NurseryBatch batch, LocalDate date, NurseryStage stage) {
        NurseryEvent event = event(batch, NurseryEventType.STAGE_CHANGE, date);
        event.setStage(stage);
        return nurseryEventRepository.save(event);
    }

    private NurseryEvent loss(NurseryBatch batch, LocalDate date, int quantity) {
        NurseryEvent event = event(batch, NurseryEventType.LOSS, date);
        event.setQuantity(quantity);
        event.setReason("Graft failure");
        return nurseryEventRepository.save(event);
    }

    private static NurseryEvent event(NurseryBatch batch, NurseryEventType type, LocalDate date) {
        NurseryEvent event = new NurseryEvent();
        event.setBatch(batch);
        event.setEventType(type);
        event.setEventDate(date);
        event.setSource("user_entry");
        return event;
    }

    private static List<Long> ids(List<NurseryEvent> events) {
        return events.stream().map(NurseryEvent::getId).toList();
    }

    @Test
    void findByBatchIds_shouldReturnTheEventsOfThoseBatches_byDateThenIdentifier() {
        // Arrange: saved out of date order, two on the same day
        NurseryBatch p1 = batch(null, "P1");
        NurseryBatch p2 = batch(null, "P2");
        NurseryBatch notAskedFor = batch(null, "P3");
        NurseryEvent p1Late = loss(p1, LocalDate.of(2026, 9, 15), 12);
        NurseryEvent p1Early = stageChange(p1, LocalDate.of(2026, 3, 2), NurseryStage.GERMINATION);
        NurseryEvent p2SameDay = stageChange(p2, LocalDate.of(2026, 3, 2), NurseryStage.HARDENING);
        stageChange(notAskedFor, LocalDate.of(2026, 3, 2), NurseryStage.GERMINATION);
        // Act
        List<NurseryEvent> events = nurseryEventRepository.findByBatchIds(List.of(p1.getId(), p2.getId()));
        // Assert
        assertEquals(List.of(p1Early.getId(), p2SameDay.getId(), p1Late.getId()), ids(events));
    }

    private List<Long> find(Long batchId, Long farmId, NurseryEventType eventType, LocalDate from, LocalDate to) {
        return ids(nurseryEventRepository.findByOptionalFilters(batchId, farmId, eventType, from, to));
    }

    @Test
    void findByOptionalFilters_shouldReturnEveryEvent_byDateThenIdentifier_withoutFilter() {
        // Arrange: saved out of date order, two on the same day
        NurseryBatch p1 = batch(null, "P1");
        NurseryBatch p2 = batch(1L, "P2");
        NurseryEvent late = loss(p1, LocalDate.of(2026, 9, 15), 12);
        NurseryEvent early = stageChange(p2, LocalDate.of(2026, 3, 2), NurseryStage.HARDENING);
        NurseryEvent sameDay = stageChange(p1, LocalDate.of(2026, 3, 2), NurseryStage.GERMINATION);
        // Act & Assert
        assertEquals(List.of(early.getId(), sameDay.getId(), late.getId()), find(null, null, null, null, null));
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheBatchAndOnTheFarmOfTheBatch() {
        // Arrange
        NurseryBatch p1 = batch(null, "P1");
        NurseryBatch p2 = batch(1L, "P2");
        NurseryEvent ofP1 = stageChange(p1, LocalDate.of(2026, 3, 2), NurseryStage.GERMINATION);
        NurseryEvent ofP2 = stageChange(p2, LocalDate.of(2026, 3, 2), NurseryStage.HARDENING);
        // Act & Assert: an unknown batch or farm matches nothing
        assertEquals(List.of(ofP1.getId()), find(p1.getId(), null, null, null, null));
        assertEquals(List.of(ofP2.getId()), find(null, 1L, null, null, null));
        assertEquals(List.of(), find(p1.getId(), 1L, null, null, null));
        assertEquals(List.of(), find(p2.getId() + 1000, null, null, null, null));
        assertEquals(List.of(), find(null, 3L, null, null, null));
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheEventType() {
        // Arrange
        NurseryBatch p1 = batch(null, "P1");
        NurseryEvent stageChange = stageChange(p1, LocalDate.of(2026, 3, 2), NurseryStage.GERMINATION);
        NurseryEvent loss = loss(p1, LocalDate.of(2026, 9, 15), 12);
        // Act & Assert
        assertEquals(List.of(stageChange.getId()), find(null, null, NurseryEventType.STAGE_CHANGE, null, null));
        assertEquals(List.of(loss.getId()), find(null, null, NurseryEventType.LOSS, null, null));
        assertEquals(List.of(), find(null, null, NurseryEventType.TRANSPLANT, null, null));
    }

    @Test
    void findByOptionalFilters_shouldIncludeBothDates() {
        // Arrange
        NurseryBatch p1 = batch(null, "P1");
        stageChange(p1, LocalDate.of(2026, 3, 2), NurseryStage.GERMINATION);
        NurseryEvent onFrom = stageChange(p1, LocalDate.of(2026, 3, 25), NurseryStage.ROOTSTOCK_GROWTH);
        NurseryEvent onTo = loss(p1, LocalDate.of(2026, 9, 15), 12);
        loss(p1, LocalDate.of(2026, 9, 16), 1);
        // Act & Assert: a from date after the to date matches nothing
        assertEquals(List.of(onFrom.getId(), onTo.getId()),
                find(null, null, null, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 9, 15)));
        assertEquals(List.of(), find(null, null, null, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 3, 25)));
    }
}
