package com.infineonbit.sustainablefarm.modules.plants.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The natural keys follow the planting lookups: the block id (which gives the
 * farm), and the variety name, which ignores case only.
 */
class NaturalKeyTest {

    @Test
    void varietyKeyOf_shouldJoinTheBlockIdAndTheLowerCaseName() {
        assertEquals("3|keitt", BlockVariety.keyOf(3L, "Keitt"));
    }

    @Test
    void varietyKeyOf_shouldKeepTwoBlocksApart() {
        assertEquals("1|kent", BlockVariety.keyOf(1L, "Kent"));
        assertEquals("2|kent", BlockVariety.keyOf(2L, "Kent"));
    }

    @Test
    void varietyKeyOf_shouldLowerTheCaseOnly_andKeepAccentsAndSpacesAsStored() {
        // The lookup compares LOWER(name) without trimming the stored name
        assertEquals("3|amélie ", BlockVariety.keyOf(3L, "AMÉLIE "));
    }

    @Test
    void varietyKeyOf_shouldBeNull_withoutBlockOrName() {
        assertNull(BlockVariety.keyOf(null, "Keitt"));
        assertNull(BlockVariety.keyOf(3L, null));
    }

    @Test
    void growthCalendarKeyOf_shouldBeTheBlockId() {
        assertEquals("1", GrowthCalendar.keyOf(1L));
        assertEquals("2", GrowthCalendar.keyOf(2L));
        assertNull(GrowthCalendar.keyOf(null));
    }
}
