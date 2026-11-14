package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.HarvestRecordRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The filling runs against a real database, each harvest in its own
 * transaction as at startup, so the test itself runs outside any transaction
 * and removes its rows afterwards. It is built by hand, not imported as a bean:
 * a bean would already run at context startup, before the test.
 *
 * <p>The harvests recorded before lot numbers existed are saved through the
 * repository: only the harvest service gives a lot number.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class HarvestLotNumberBackfillTest {

    private static final Instant WRITTEN = Instant.parse("2026-09-01T08:00:00Z");

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private PopulationEventRepository populationEventRepository;

    @Autowired
    private HarvestRecordRepository harvestRecordRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    private HarvestLotNumberBackfill backfill;

    @BeforeEach
    void setUp() {
        HarvestService harvestService = new HarvestService(varietyRepository, populationEventRepository,
                harvestRecordRepository);
        backfill = new HarvestLotNumberBackfill(harvestRecordRepository, harvestService, transactionManager);
    }

    @AfterEach
    void removeTheRowsOfTheTest() {
        jdbc.update("delete from harvest_record where variety_id in "
                + "(select id from varietes where bloc_parcelle like 'LF%')");
        jdbc.update("delete from varietes where bloc_parcelle like 'LF%'");
    }

    private Variety variety(Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return varietyRepository.save(variety);
    }

    /** A harvest as recorded before lot numbers existed, or with the given lot number. */
    private Long harvest(Variety variety, LocalDate harvestDate, String lotNumber) {
        HarvestRecord harvest = new HarvestRecord();
        harvest.setLotNumber(lotNumber);
        harvest.setVariety(variety);
        harvest.setHarvestDate(harvestDate);
        harvest.setQuantityKg(100.0);
        harvest.setSource("user_entry");
        harvest.setLastUpdated(WRITTEN);
        return harvestRecordRepository.save(harvest).getId();
    }

    private String lotNumber(Long id) {
        return harvestRecordRepository.findById(id).orElseThrow().getLotNumber();
    }

    @Test
    void fill_shouldNumberTheEarlierHarvestsOldestFirst_byBlockAndYear() {
        // Arrange: block LF1 on two farms and two years, and block LF2
        Variety keitt = variety(null, "LF1", "Keitt");
        Long first = harvest(keitt, LocalDate.of(2026, 6, 10), null);
        Long secondDatedEarlier = harvest(variety(1, "LF1", "Kent"), LocalDate.of(2026, 5, 1), null);
        Long lastYear = harvest(keitt, LocalDate.of(2025, 7, 1), null);
        Long otherBlock = harvest(variety(null, "LF2", "Keitt"), LocalDate.of(2026, 6, 10), null);
        // Act
        HarvestLotNumberBackfill.Result result = backfill.fill();
        // Assert: in the order the harvests were recorded, one sequence per block and year
        assertEquals("LOT-2026-LF1-001", lotNumber(first));
        assertEquals("LOT-2026-LF1-002", lotNumber(secondDatedEarlier));
        assertEquals("LOT-2025-LF1-001", lotNumber(lastYear));
        assertEquals("LOT-2026-LF2-001", lotNumber(otherBlock));
        assertEquals(new HarvestLotNumberBackfill.Result(4, List.of()), result);
    }

    @Test
    void fill_shouldGoOnAfterTheLotsAlreadyGiven_andLeaveThemAlone() {
        // Arrange: an earlier harvest without a lot, then a harvest recorded with lot 005
        Variety keitt = variety(null, "LF3", "Keitt");
        Long earlier = harvest(keitt, LocalDate.of(2026, 6, 1), null);
        Long numbered = harvest(keitt, LocalDate.of(2026, 6, 10), "LOT-2026-LF3-005");
        // Act
        HarvestLotNumberBackfill.Result result = backfill.fill();
        // Assert: the earlier harvest comes after the lots already given; the numbered one is untouched
        assertEquals("LOT-2026-LF3-006", lotNumber(earlier));
        assertEquals("LOT-2026-LF3-005", lotNumber(numbered));
        assertEquals(1, result.numbered());
    }

    @Test
    void fill_shouldChangeNoLastUpdate() {
        // Arrange
        Long earlier = harvest(variety(null, "LF4", "Keitt"), LocalDate.of(2026, 6, 1), null);
        Instant lastUpdateBefore = harvestRecordRepository.findById(earlier).orElseThrow().getLastUpdated();
        // Act
        backfill.fill();
        // Assert
        assertEquals("LOT-2026-LF4-001", lotNumber(earlier));
        assertEquals(lastUpdateBefore, harvestRecordRepository.findById(earlier).orElseThrow().getLastUpdated());
    }

    @Test
    void fill_shouldLeaveAHarvestWithoutLot_whenItsNumberIsTakenAtTheSameMoment() {
        // Arrange: a harvest recorded meanwhile took the number the filling computed
        Variety keitt = variety(null, "LF5", "Keitt");
        Long earlier = harvest(keitt, LocalDate.of(2026, 6, 1), null);
        harvest(keitt, LocalDate.of(2026, 6, 10), "LOT-2026-LF5-001");
        HarvestService staleNumbers = mock(HarvestService.class);
        when(staleNumbers.nextLotNumber("LF5", 2026)).thenReturn("LOT-2026-LF5-001");
        HarvestLotNumberBackfill racing = new HarvestLotNumberBackfill(harvestRecordRepository, staleNumbers,
                transactionManager);
        // Act
        HarvestLotNumberBackfill.Result result = racing.fill();
        // Assert: refused, reported, and left for the next startup
        assertNull(lotNumber(earlier));
        assertEquals(new HarvestLotNumberBackfill.Result(0, List.of(earlier)), result);
    }

    @Test
    void run_shouldNeverStopTheStartup_whenTheFillingFails() {
        // Arrange: the database cannot be read
        HarvestRecordRepository unavailable = mock(HarvestRecordRepository.class);
        when(unavailable.findWithoutLotNumberOldestFirst()).thenThrow(new IllegalStateException("database down"));
        HarvestLotNumberBackfill failing = new HarvestLotNumberBackfill(unavailable,
                mock(HarvestService.class), transactionManager);
        // Act & Assert: the failure is logged, not thrown
        assertDoesNotThrow(() -> failing.run());
    }
}
