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

    private NurseryBatch batch(Integer farmId, String batchCode) {
        return nurseryBatchRepository.save(new NurseryBatch(null, farmId, batchCode, "Keitt", NurseryOrigin.IN_HOUSE,
                null, null, LocalDate.of(2026, 3, 2), 150, LocalDate.of(2026, 9, 20), null, "user_entry", null));
    }

    private NurseryEvent stageChange(NurseryBatch batch, LocalDate date, NurseryStage stage) {
        return nurseryEventRepository.save(new NurseryEvent(null, batch, NurseryEventType.STAGE_CHANGE, date, stage,
                null, null, null, null, "user_entry", null));
    }

    private NurseryEvent loss(NurseryBatch batch, LocalDate date, int quantity) {
        return nurseryEventRepository.save(new NurseryEvent(null, batch, NurseryEventType.LOSS, date, null, quantity,
                "Graft failure", null, null, "user_entry", null));
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
}
