package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HarvestRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HarvestResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.repository.HarvestRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One harvest read by its identifier, through the real service and the
 * database. The variety of a harvest is a lazy association and open-in-view is
 * disabled, so the response is built after the session of the read is closed;
 * a mocked repository cannot show that the variety is loaded with the harvest.
 * The blocks HR1 and HR2 are used by no other test.
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

    @Autowired
    private PlantsBlocks plantsBlocks;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
        assertEquals("LOT-2026-HR1-001", response.lotNumber());
        // A block always belongs to a farm: the default farm when the planting named none
        assertEquals(plantsBlocks.find(null, "HR1").orElseThrow().farmId(), response.farmId());
        assertEquals("HR1", response.blockCode());
        assertEquals(varietyId, response.varietyId());
        assertEquals("Keitt", response.varietyName());
        assertEquals(HARVESTED_ON, response.harvestDate());
        assertEquals(125.5, response.quantityKg());
        assertEquals("user_entry", response.source());
        assertEquals(lastUpdated, response.lastUpdated());
    }

    @Test
    void getHarvestById_shouldSendTheQuantityAsEntered_whileTheDatabaseKeepsItToTheGram() {
        // Arrange: Kent planted on block HR2, then one harvest of 12.5 kg
        plantingService.recordPlanting(new PlantingRequest(null, "HR2", "Kent", LocalDate.of(2023, 9, 15), 40));
        Long harvestId = harvestService.recordHarvest(
                new HarvestRequest(null, "HR2", "Kent", HARVESTED_ON, 12.5)).id();
        // Act
        HarvestRecord stored = transactionTemplate.execute(status ->
                harvestRecordRepository.findById(harvestId).orElseThrow());
        String json = jsonMapper.writeValueAsString(harvestService.getHarvestById(harvestId));
        // Assert: numeric(12,3) in the database, the number as entered in the JSON, never 12.500
        assertEquals(new BigDecimal("12.500"), stored.getQuantityKg());
        assertTrue(json.contains("\"quantityKg\":12.5,"), json);
        // The audit timestamps of the shared base entity are written with the row
        assertNotNull(stored.getCreatedAt());
        assertNotNull(stored.getUpdatedAt());
    }
}
