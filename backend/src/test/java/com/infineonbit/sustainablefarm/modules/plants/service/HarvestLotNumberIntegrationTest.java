package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The lot numbers of the harvests, through the real service and the database:
 * the next number is read from the lots already stored. The blocks starting
 * with LN are used by no other test.
 */
@SpringBootTest
@ActiveProfiles("test")
class HarvestLotNumberIntegrationTest {

    private static final LocalDate PLANTED_ON = LocalDate.of(2023, 9, 15);
    private static final LocalDate HARVESTED_ON = LocalDate.of(2026, 6, 10);

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private HarvestService harvestService;

    private HarvestResponse harvest(String blockCode, String varietyName, LocalDate harvestDate) {
        return harvestService.recordHarvest(new HarvestRequest(null, blockCode, varietyName, harvestDate, 50.0));
    }

    @Test
    void recordHarvest_shouldNumberTheLotsOfABlockAndYearInTheOrderTheyAreRecorded() {
        // Arrange: Keitt and Kent planted on block LN1
        plantingService.recordPlanting(new PlantingRequest(null, "LN1", "Keitt", PLANTED_ON, 40));
        plantingService.recordPlanting(new PlantingRequest(null, "LN1", "Kent", PLANTED_ON, 20));
        // Act: two pickings of 2026 on two varieties, the second one dated earlier, then one of 2025 recorded late
        HarvestResponse first = harvest("LN1", "Keitt", HARVESTED_ON);
        HarvestResponse second = harvest(" ln1 ", "Kent", HARVESTED_ON.minusDays(5));
        HarvestResponse late = harvest("LN1", "Keitt", LocalDate.of(2025, 12, 30));
        // Assert: one sequence per block and year, whatever the variety, in the order of recording
        assertEquals("LOT-2026-LN1-001", first.lotNumber());
        assertEquals("LOT-2026-LN1-002", second.lotNumber());
        assertEquals("LOT-2025-LN1-001", late.lotNumber());
        // Assert: the number is stored, not made up when the harvest is read
        assertEquals("LOT-2026-LN1-002", harvestService.getHarvestById(second.id()).lotNumber());
    }

    @Test
    void recordHarvest_shouldShareTheSequenceOfABlockBetweenFarms() {
        // Arrange: block LN2 on no farm and on farm 1
        plantingService.recordPlanting(new PlantingRequest(null, "LN2", "Keitt", PLANTED_ON, 40));
        plantingService.recordPlanting(new PlantingRequest(1, "LN2", "Keitt", PLANTED_ON, 30));
        // Act
        HarvestResponse noFarm = harvest("LN2", "Keitt", HARVESTED_ON);
        HarvestResponse farm1 = harvestService.recordHarvest(new HarvestRequest(1, "LN2", "Keitt", HARVESTED_ON, 20.0));
        // Assert: one lot number is never given twice, even on two farms
        assertEquals("LOT-2026-LN2-001", noFarm.lotNumber());
        assertEquals("LOT-2026-LN2-002", farm1.lotNumber());
        assertEquals(1, farm1.farmId());
    }
}
