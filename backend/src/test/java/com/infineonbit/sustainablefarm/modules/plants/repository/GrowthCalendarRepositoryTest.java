package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One calendar row per block, found by its block id (a {@code farm.block} row). */
@DataJpaTest
@ActiveProfiles("test")
public class GrowthCalendarRepositoryTest {

    private static final Long BLOCK_A = 11L;
    private static final Long BLOCK_B = 12L;

    @Autowired
    private GrowthCalendarRepository growthCalendarRepository;

    private static GrowthCalendar entry(Long blockId) {
        GrowthCalendar entry = new GrowthCalendar();
        entry.setBlockId(blockId);
        return entry;
    }

    private GrowthCalendar save(Long blockId) {
        return growthCalendarRepository.save(entry(blockId));
    }

    private static List<Long> ids(List<GrowthCalendar> entries) {
        return entries.stream().map(GrowthCalendar::getId).toList();
    }

    @Test
    void findByBlock_shouldKeepTwoBlocksApart() {
        // Arrange
        GrowthCalendar onA = save(BLOCK_A);
        GrowthCalendar onB = save(BLOCK_B);
        // Act & Assert
        assertEquals(List.of(onA.getId()), ids(growthCalendarRepository.findByBlock(BLOCK_A)));
        assertEquals(List.of(onB.getId()), ids(growthCalendarRepository.findByBlock(BLOCK_B)));
    }

    @Test
    void findByBlock_shouldReturnNothing_whenBlockDiffers() {
        // Arrange
        save(BLOCK_A);
        // Act & Assert
        assertTrue(growthCalendarRepository.findByBlock(BLOCK_B).isEmpty());
    }

    @Test
    void findByBlockForUpdate_shouldMatchTheSameRowsAsTheSharedLookup() {
        // Arrange
        GrowthCalendar onA = save(BLOCK_A);
        save(BLOCK_B);
        // Act: the locked lookup runs inside the transaction of the test
        List<GrowthCalendar> found = growthCalendarRepository.findByBlockForUpdate(BLOCK_A);
        // Assert
        assertEquals(List.of(onA.getId()), ids(found));
        assertEquals(ids(growthCalendarRepository.findByBlock(BLOCK_A)), ids(found));
        assertTrue(growthCalendarRepository.findByBlockForUpdate(13L).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldReadEveryBlock_orOnlyTheGivenBlocks() {
        // Arrange
        GrowthCalendar onA = save(BLOCK_A);
        GrowthCalendar onB = save(BLOCK_B);
        // Act & Assert
        assertEquals(List.of(onA.getId(), onB.getId()),
                ids(growthCalendarRepository.findByOptionalFilters(true, List.of(-1L))));
        assertEquals(List.of(onB.getId()), ids(growthCalendarRepository.findByOptionalFilters(false, List.of(BLOCK_B))));
        assertTrue(growthCalendarRepository.findByOptionalFilters(false, List.of(-1L)).isEmpty());
    }

    @Test
    void save_shouldRefuseASecondCalendarRowForTheSameBlock() {
        // Arrange
        growthCalendarRepository.saveAndFlush(entry(BLOCK_A));
        GrowthCalendar sameBlock = entry(BLOCK_A);
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> growthCalendarRepository.saveAndFlush(sameBlock));
    }
}
