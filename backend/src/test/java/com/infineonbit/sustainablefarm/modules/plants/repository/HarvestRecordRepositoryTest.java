package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HarvestRecord;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.support.TestBlocks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
public class HarvestRecordRepositoryTest {

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private HarvestRecordRepository harvestRecordRepository;

    /** A variety row on the block of a code, by its stable test id ({@link TestBlocks#idOf}). */
    private Variety variety(String blockCode, String name) {
        Variety variety = new Variety();
        variety.setBlockId(TestBlocks.idOf(blockCode));
        variety.setName(name);
        return varietyRepository.save(variety);
    }

    private HarvestRecord harvest(Variety variety, LocalDate date, double quantityKg) {
        return harvest(variety, date, quantityKg, null);
    }

    private HarvestRecord harvest(Variety variety, LocalDate date, double quantityKg, String lotNumber) {
        HarvestRecord harvest = new HarvestRecord();
        harvest.setLotNumber(lotNumber);
        harvest.setVariety(variety);
        harvest.setHarvestDate(date);
        harvest.setQuantityKg(quantityKg);
        harvest.setSource("user_entry");
        return harvestRecordRepository.save(harvest);
    }

    private static List<Long> blocks(String... codes) {
        return codes.length == 0 ? null : java.util.Arrays.stream(codes).map(TestBlocks::idOf).toList();
    }

    /** @param blockIds the blocks to keep, or {@code null} for every block */
    private List<Long> find(List<Long> blockIds, LocalDate from, LocalDate to) {
        return findLot(blockIds, null, from, to);
    }

    private List<Long> findLot(List<Long> blockIds, String lotNumber, LocalDate from, LocalDate to) {
        return harvestRecordRepository.findByOptionalFilters(blockIds == null,
                        blockIds == null ? List.of(-1L) : blockIds, lotNumber, from, to).stream()
                .map(HarvestRecord::getId)
                .toList();
    }

    @Test
    void findByOptionalFilters_shouldReturnEveryHarvestByDateThenId_whenNoFilterIsGiven() {
        // Arrange: two pickings on the same day, saved out of date order
        Variety keitt = variety("B", "Keitt");
        HarvestRecord late = harvest(keitt, LocalDate.of(2026, 7, 2), 300.0);
        HarvestRecord firstPicking = harvest(keitt, LocalDate.of(2026, 6, 10), 800.0);
        HarvestRecord secondPicking = harvest(keitt, LocalDate.of(2026, 6, 10), 800.0);
        // Act & Assert
        assertEquals(List.of(firstPicking.getId(), secondPicking.getId(), late.getId()),
                find(null, null, null));
    }

    @Test
    void findByOptionalFilters_shouldKeepOnlyTheGivenBlocks() {
        // Arrange: the same variety on two blocks, as the blocks of one farm
        HarvestRecord onA = harvest(variety("A", "Keitt"), LocalDate.of(2026, 6, 10), 800.0);
        HarvestRecord onB = harvest(variety("B", "Keitt"), LocalDate.of(2026, 6, 10), 500.0);
        // Act & Assert: the blocks of a farm, one block, every block, none
        assertEquals(List.of(onA.getId(), onB.getId()), find(blocks("A", "B"), null, null));
        assertEquals(List.of(onB.getId()), find(blocks("B"), null, null));
        assertEquals(List.of(onA.getId(), onB.getId()), find(null, null, null));
        assertTrue(find(List.of(-1L), null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheBlockOfTheVariety() {
        // Arrange
        HarvestRecord blockB = harvest(variety("B", "Keitt"), LocalDate.of(2026, 6, 10), 800.0);
        harvest(variety("A", "Kent"), LocalDate.of(2026, 5, 2), 400.0);
        // Act & Assert
        assertEquals(List.of(blockB.getId()), find(blocks("B"), null, null));
        assertTrue(find(blocks("C"), null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldIncludeBothBounds() {
        // Arrange
        Variety keitt = variety("B", "Keitt");
        harvest(keitt, LocalDate.of(2026, 5, 31), 100.0);
        HarvestRecord first = harvest(keitt, LocalDate.of(2026, 6, 1), 200.0);
        HarvestRecord last = harvest(keitt, LocalDate.of(2026, 6, 30), 300.0);
        harvest(keitt, LocalDate.of(2026, 7, 1), 400.0);
        // Act & Assert
        assertEquals(List.of(first.getId(), last.getId()),
                find(null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));
    }

    @Test
    void findByOptionalFilters_shouldApplyASingleBound() {
        // Arrange
        Variety keitt = variety("B", "Keitt");
        HarvestRecord may = harvest(keitt, LocalDate.of(2026, 5, 31), 100.0);
        HarvestRecord june = harvest(keitt, LocalDate.of(2026, 6, 1), 200.0);
        // Act & Assert
        assertEquals(List.of(june.getId()), find(null, LocalDate.of(2026, 6, 1), null));
        assertEquals(List.of(may.getId()), find(null, null, LocalDate.of(2026, 5, 31)));
    }

    @Test
    void findByOptionalFilters_shouldReturnNothing_whenFromIsAfterTo() {
        // Arrange
        harvest(variety("B", "Keitt"), LocalDate.of(2026, 6, 10), 800.0);
        // Act & Assert: an empty list, not an error
        assertTrue(find(null, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1)).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldMatchTheLotNumberExactly() {
        // Arrange: lot numbers that share a beginning
        Variety keitt = variety("A", "Keitt");
        HarvestRecord lot1 = harvest(keitt, LocalDate.of(2026, 6, 10), 800.0, "LOT-2026-A-001");
        harvest(keitt, LocalDate.of(2026, 6, 11), 700.0, "LOT-2026-A-0010");
        harvest(keitt, LocalDate.of(2026, 6, 12), 600.0, "LOT-2026-A-010");
        harvest(keitt, LocalDate.of(2026, 5, 2), 100.0);
        // Act & Assert: the one lot, never a lot that only starts or ends like it
        assertEquals(List.of(lot1.getId()), findLot(null, "LOT-2026-A-001", null, null));
        assertTrue(findLot(null, "LOT-2026-A-01", null, null).isEmpty());
        assertTrue(findLot(null, "LOT-1999-Z-999", null, null).isEmpty());
        // Act & Assert: combined with the other filters
        assertEquals(List.of(lot1.getId()), findLot(blocks("A"), "LOT-2026-A-001", LocalDate.of(2026, 6, 10), null));
        assertTrue(findLot(blocks("B"), "LOT-2026-A-001", null, null).isEmpty());
        assertTrue(findLot(List.of(-1L), "LOT-2026-A-001", null, null).isEmpty());
    }

    @Test
    void findLotNumbersStartingWith_shouldReturnTheLotsOfOneBlockAndYearOnly() {
        // Arrange: lots of block A in 2026, and of the blocks and years that come close
        harvest(variety("A", "Keitt"), LocalDate.of(2026, 6, 10), 800.0, "LOT-2026-A-001");
        harvest(variety("A", "Kent"), LocalDate.of(2026, 7, 2), 300.0, "LOT-2026-A-002");
        harvest(variety("A1", "Keitt"), LocalDate.of(2026, 6, 10), 100.0, "LOT-2026-A1-001");
        harvest(variety("AB", "Keitt"), LocalDate.of(2026, 6, 10), 100.0, "LOT-2026-AB-001");
        harvest(variety("BA", "Keitt"), LocalDate.of(2026, 6, 10), 100.0, "LOT-2026-BA-001");
        harvest(variety("A", "Amelie"), LocalDate.of(2025, 6, 10), 100.0, "LOT-2025-A-001");
        harvest(variety("A", "Palmer"), LocalDate.of(2026, 5, 2), 100.0);
        // Act
        List<String> lots = harvestRecordRepository.findLotNumbersStartingWith("LOT-2026-A-");
        // Assert: the two lots of block A for 2026, and no other
        assertEquals(List.of("LOT-2026-A-001", "LOT-2026-A-002"), lots.stream().sorted().toList());
    }

    @Test
    void save_shouldRefuseASecondHarvestWithTheSameLotNumber() {
        // Arrange
        harvest(variety("B", "Keitt"), LocalDate.of(2026, 6, 10), 800.0, "LOT-2026-B-001");
        Variety kent = variety("B", "Kent");
        // Act & Assert: refused by the unique constraint, whatever the variety
        assertThrows(DataIntegrityViolationException.class,
                () -> harvest(kent, LocalDate.of(2026, 6, 10), 500.0, "LOT-2026-B-001"));
    }

    @Test
    void save_shouldAcceptSeveralHarvestsWithoutLotNumber() {
        // Arrange: rows written before lot numbers existed
        Variety keitt = variety("B", "Keitt");
        // Act
        harvest(keitt, LocalDate.of(2026, 6, 10), 800.0);
        harvest(keitt, LocalDate.of(2026, 6, 11), 700.0);
        // Assert: two NULLs are not a duplicate
        assertEquals(2, find(blocks("B"), null, null).size());
    }
}
