package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.NurseryBatchRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.TransplantRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryBatchResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.NurseryEventResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.NurseryEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A transplant and the planting it creates are stored together or not at all.
 * Runs the real services, their shared transaction and the database. The
 * blocks N1 and N2 are used by no other test.
 */
@SpringBootTest
@ActiveProfiles("test")
class NurseryTransplantTransactionIntegrationTest {

    private static final LocalDate MARCH_2 = LocalDate.of(2026, 3, 2);

    @Autowired
    private NurseryBatchService nurseryBatchService;

    @Autowired
    private NurseryEventService nurseryEventService;

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private NurseryEventRepository nurseryEventRepository;

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private PopulationEventRepository populationEventRepository;

    @Autowired
    private GrowthCalendarRepository growthCalendarRepository;

    /** A batch of 150 Keitt without farm, sown on March 2. */
    private Long startBatch(String batchCode) {
        return nurseryBatchService.createBatch(new NurseryBatchRequest(null, batchCode, "Keitt", "IN_HOUSE", null,
                null, MARCH_2, 150.0, "GERMINATION", LocalDate.of(2026, 9, 20), null)).id();
    }

    @Test
    void recordTransplant_shouldPlantTheVarietyOnTheBlock_inTheSameTransaction() {
        // Arrange
        Long batchId = startBatch("IT-N1");
        LocalDate transplantedOn = LocalDate.of(2026, 9, 20);
        // Act
        NurseryEventResponse transplant = nurseryEventService.recordTransplant(batchId,
                new TransplantRequest(transplantedOn, 100.0, "n1"));
        // Assert: the variety row of the block, its PLANTING and the calendar date of the block
        List<Variety> varieties = varietyRepository.findByFarmBlockAndName(null, "N1", "Keitt");
        assertEquals(1, varieties.size());
        PopulationEvent planting = populationEventRepository.findById(transplant.populationEventId()).orElseThrow();
        assertEquals(PopulationEventType.PLANTING, planting.getEventType());
        assertEquals(varieties.get(0).getId(), planting.getVariety().getId());
        assertEquals(transplantedOn, planting.getEventDate());
        assertEquals(100, planting.getTreeCount());
        assertEquals(transplantedOn, growthCalendarRepository.findByFarmAndBlock(null, "N1").get(0).getPlantingDate());
        // Assert: the batch, computed from its events
        NurseryBatchResponse batch = nurseryBatchService.getBatchById(batchId);
        assertEquals(50, batch.currentCount());
        assertEquals(100, batch.transplantedCount());
        assertEquals(150, batch.survivedCount());
    }

    @Test
    void recordTransplant_shouldStoreNothing_whenTheVarietyIsAlreadyPlantedOnTheBlock() {
        // Arrange: Keitt is already planted on block N2
        plantingService.recordPlanting(new PlantingRequest(null, "N2", "Keitt", LocalDate.of(2026, 9, 1), 20));
        Long batchId = startBatch("IT-N2");
        long eventsBefore = nurseryEventRepository.count();
        long varietiesBefore = varietyRepository.count();
        long populationEventsBefore = populationEventRepository.count();
        long calendarsBefore = growthCalendarRepository.count();
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> nurseryEventService.recordTransplant(batchId,
                        new TransplantRequest(LocalDate.of(2026, 9, 21), 38.0, "N2")));
        // Assert: the answer of the planting service, unchanged, and nothing stored
        assertEquals("A planting of Keitt is already recorded on block N2", exception.getMessage());
        assertEquals(eventsBefore, nurseryEventRepository.count());
        assertEquals(varietiesBefore, varietyRepository.count());
        assertEquals(populationEventsBefore, populationEventRepository.count());
        assertEquals(calendarsBefore, growthCalendarRepository.count());
        assertEquals(150, nurseryBatchService.getBatchById(batchId).currentCount());
    }
}
