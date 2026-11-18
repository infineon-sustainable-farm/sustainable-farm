package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.BlockVariety;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BlockVariety rows are found by their block id (a {@code farm.block} row) and their name. */
@DataJpaTest
@ActiveProfiles("test")
public class BlockVarietyRepositoryTest {

    private static final Long BLOCK_A = 11L;
    private static final Long BLOCK_B = 12L;

    @Autowired
    private BlockVarietyRepository blockVarietyRepository;

    private static BlockVariety variety(Long blockId, String name) {
        BlockVariety variety = new BlockVariety();
        variety.setBlockId(blockId);
        variety.setName(name);
        return variety;
    }

    private BlockVariety save(Long blockId, String name) {
        return blockVarietyRepository.save(variety(blockId, name));
    }

    private static List<Long> ids(List<BlockVariety> varieties) {
        return varieties.stream().map(BlockVariety::getId).toList();
    }

    @Test
    void findByBlockAndName_shouldMatchOnlyThatBlock() {
        // Arrange
        BlockVariety onA = save(BLOCK_A, "Keitt");
        save(BLOCK_B, "Keitt");
        // Act
        List<BlockVariety> found = blockVarietyRepository.findByBlockAndName(BLOCK_A, "Keitt");
        // Assert
        assertEquals(List.of(onA.getId()), ids(found));
    }

    @Test
    void findByBlockAndName_shouldIgnoreNameCase() {
        // Arrange
        BlockVariety keitt = save(BLOCK_A, "Keitt");
        // Act
        List<BlockVariety> found = blockVarietyRepository.findByBlockAndName(BLOCK_A, "kEITT");
        // Assert
        assertEquals(List.of(keitt.getId()), ids(found));
    }

    @Test
    void findByBlockAndName_shouldReturnNothing_whenBlockDiffers() {
        // Arrange
        save(BLOCK_A, "Keitt");
        // Act & Assert
        assertTrue(blockVarietyRepository.findByBlockAndName(BLOCK_B, "Keitt").isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldReadEveryBlock_orOnlyTheGivenBlocks() {
        // Arrange
        BlockVariety onA = save(BLOCK_A, "Keitt");
        BlockVariety onB = save(BLOCK_B, "Kent");
        // Act
        List<BlockVariety> all = blockVarietyRepository.findByOptionalFilters(true, List.of(-1L));
        List<BlockVariety> onlyB = blockVarietyRepository.findByOptionalFilters(false, List.of(BLOCK_B));
        List<BlockVariety> none = blockVarietyRepository.findByOptionalFilters(false, List.of(-1L));
        // Assert: ordered by block id, then name
        assertEquals(List.of(onA.getId(), onB.getId()), ids(all));
        assertEquals(List.of(onB.getId()), ids(onlyB));
        assertTrue(none.isEmpty());
    }

    @Test
    void findByBlockAndNameForUpdate_shouldMatchTheSameRowsAsTheSharedLookup() {
        // Arrange
        BlockVariety onA = save(BLOCK_A, "Keitt");
        save(BLOCK_B, "Keitt");
        // Act: the locked lookup runs inside the transaction of the test
        List<BlockVariety> found = blockVarietyRepository.findByBlockAndNameForUpdate(BLOCK_A, "kEITT");
        // Assert
        assertEquals(List.of(onA.getId()), ids(found));
        assertEquals(ids(blockVarietyRepository.findByBlockAndName(BLOCK_A, "kEITT")), ids(found));
    }

    @Test
    void save_shouldRefuseASecondRowOfTheSamePair() {
        // Arrange
        blockVarietyRepository.saveAndFlush(variety(BLOCK_A, "Keitt"));
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> blockVarietyRepository.saveAndFlush(variety(BLOCK_A, "Keitt")));
    }

    @Test
    void save_shouldRefuseASecondRowOfTheSamePair_whenOnlyTheCaseOfTheNameDiffers() {
        // Arrange
        blockVarietyRepository.saveAndFlush(variety(BLOCK_A, "Keitt"));
        // Act & Assert: the planting lookup already reads "keitt" as "Keitt"
        assertThrows(DataIntegrityViolationException.class,
                () -> blockVarietyRepository.saveAndFlush(variety(BLOCK_A, "keitt")));
    }

    @Test
    void save_shouldAcceptTheSameNameOnAnotherBlock() {
        // Arrange
        blockVarietyRepository.saveAndFlush(variety(BLOCK_A, "Keitt"));
        // Act
        blockVarietyRepository.saveAndFlush(variety(BLOCK_B, "Keitt"));
        // Assert
        assertEquals(2, blockVarietyRepository.count());
    }
}
