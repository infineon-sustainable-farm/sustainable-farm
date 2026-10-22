package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.repository.HarvestRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * One harvest read by its identifier, through the real service and the
 * database. The variety of a harvest is a lazy association and open-in-view is
 * disabled, so the response is built after the session of the read is closed;
 * a mocked repository cannot show that the variety is loaded with the harvest.
 * The block HR1 is used by no other test.
 */
@SpringBootTest
@ActiveProfiles("test")
class HarvestByIdIntegrationTest {

    private static final LocalDate HARVESTED_ON = LocalDate.of(2026, 6, 10);

    @Autowired
    private PlantingService plantingService;

    @Autowired
    private HarvestService harvestService;

    @Autowired
    private HarvestRecordRepository harvestRecordRepository;

    @Test
    void getHarvestById_shouldReturnEveryField_withTheVarietyLoadedWithTheHarvest() {
        // Arrange: Keitt planted on block HR1 without farm, then one harvest of it
        Long varietyId = plantingService.recordPlanting(
                new PlantingRequest(null, "HR1", "Keitt", LocalDate.of(2023, 9, 15), 40)).varietyId();
        Long harvestId = harvestService.recordHarvest(
                new HarvestRequest(null, "HR1", "Keitt", HARVESTED_ON, 125.5)).id();
        // The write time as stored, at the precision of the database
        Instant lastUpdated = harvestRecordRepository.findById(harvestId).orElseThrow().getLastUpdated();
        // Act
        HarvestResponse response = harvestService.getHarvestById(harvestId);
        // Assert
        assertEquals(harvestId, response.id());
        assertNull(response.farmId());
        assertEquals("HR1", response.blockCode());
        assertEquals(varietyId, response.varietyId());
        assertEquals("Keitt", response.varietyName());
        assertEquals(HARVESTED_ON, response.harvestDate());
        assertEquals(125.5, response.quantityKg());
        assertEquals("user_entry", response.source());
        assertEquals(lastUpdated, response.lastUpdated());
    }
}
