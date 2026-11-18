package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Variety rows are found by their block id (a {@code farm.block} row) and their name. */
@DataJpaTest
@ActiveProfiles("test")
public class VarietyRepositoryTest {

    private static final Long BLOCK_A = 11L;
    private static final Long BLOCK_B = 12L;

    @Autowired
    private VarietyRepository varietyRepository;

    private static Variety variety(Long blockId, String name) {
        Variety variety = new Variety();
        variety.setBlockId(blockId);
        variety.setName(name);
        return variety;
    }

    private Variety save(Long blockId, String name) {
        return varietyRepository.save(variety(blockId, name));
    }

    private static List<Long> ids(List<Variety> varieties) {
        return varieties.stream().map(Variety::getId).toList();
    }

    @Test
    void findByBlockAndName_shouldMatchOnlyThatBlock() {
        // Arrange
        Variety onA = save(BLOCK_A, "Keitt");
        save(BLOCK_B, "Keitt");
        // Act
        List<Variety> found = varietyRepository.findByBlockAndName(BLOCK_A, "Keitt");
        // Assert
        assertEquals(List.of(onA.getId()), ids(found));
    }

    @Test
    void findByBlockAndName_shouldIgnoreNameCase() {
        // Arrange
        Variety keitt = save(BLOCK_A, "Keitt");
        // Act
        List<Variety> found = varietyRepository.findByBlockAndName(BLOCK_A, "kEITT");
        // Assert
        assertEquals(List.of(keitt.getId()), ids(found));
    }

    @Test
    void findByBlockAndName_shouldReturnNothing_whenBlockDiffers() {
        // Arrange
        save(BLOCK_A, "Keitt");
        // Act & Assert
        assertTrue(varietyRepository.findByBlockAndName(BLOCK_B, "Keitt").isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldReadEveryBlock_orOnlyTheGivenBlocks() {
        // Arrange
        Variety onA = save(BLOCK_A, "Keitt");
        Variety onB = save(BLOCK_B, "Kent");
        // Act
        List<Variety> all = varietyRepository.findByOptionalFilters(true, List.of(-1L));
        List<Variety> onlyB = varietyRepository.findByOptionalFilters(false, List.of(BLOCK_B));
        List<Variety> none = varietyRepository.findByOptionalFilters(false, List.of(-1L));
        // Assert: ordered by block id, then name
        assertEquals(List.of(onA.getId(), onB.getId()), ids(all));
        assertEquals(List.of(onB.getId()), ids(onlyB));
        assertTrue(none.isEmpty());
    }

    @Test
    void findByBlockAndNameForUpdate_shouldMatchTheSameRowsAsTheSharedLookup() {
        // Arrange
        Variety onA = save(BLOCK_A, "Keitt");
        save(BLOCK_B, "Keitt");
        // Act: the locked lookup runs inside the transaction of the test
        List<Variety> found = varietyRepository.findByBlockAndNameForUpdate(BLOCK_A, "kEITT");
        // Assert
        assertEquals(List.of(onA.getId()), ids(found));
        assertEquals(ids(varietyRepository.findByBlockAndName(BLOCK_A, "kEITT")), ids(found));
    }

    @Test
    void save_shouldRefuseASecondRowOfTheSamePair() {
        // Arrange
        varietyRepository.saveAndFlush(variety(BLOCK_A, "Keitt"));
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> varietyRepository.saveAndFlush(variety(BLOCK_A, "Keitt")));
    }

    @Test
    void save_shouldRefuseASecondRowOfTheSamePair_whenOnlyTheCaseOfTheNameDiffers() {
        // Arrange
        varietyRepository.saveAndFlush(variety(BLOCK_A, "Keitt"));
        // Act & Assert: the planting lookup already reads "keitt" as "Keitt"
        assertThrows(DataIntegrityViolationException.class,
                () -> varietyRepository.saveAndFlush(variety(BLOCK_A, "keitt")));
    }

    @Test
    void save_shouldAcceptTheSameNameOnAnotherBlock() {
        // Arrange
        varietyRepository.saveAndFlush(variety(BLOCK_A, "Keitt"));
        // Act
        varietyRepository.saveAndFlush(variety(BLOCK_B, "Keitt"));
        // Assert
        assertEquals(2, varietyRepository.count());
    }
}
