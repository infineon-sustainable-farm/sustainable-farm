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
        assertEquals("3|keitt", Variety.keyOf(3L, "Keitt"));
    }

    @Test
    void varietyKeyOf_shouldKeepTwoBlocksApart() {
        assertEquals("1|kent", Variety.keyOf(1L, "Kent"));
        assertEquals("2|kent", Variety.keyOf(2L, "Kent"));
    }

    @Test
    void varietyKeyOf_shouldLowerTheCaseOnly_andKeepAccentsAndSpacesAsStored() {
        // The lookup compares LOWER(name) without trimming the stored name
        assertEquals("3|amélie ", Variety.keyOf(3L, "AMÉLIE "));
    }

    @Test
    void varietyKeyOf_shouldBeNull_withoutBlockOrName() {
        assertNull(Variety.keyOf(null, "Keitt"));
        assertNull(Variety.keyOf(3L, null));
    }

    @Test
    void growthCalendarKeyOf_shouldBeTheBlockId() {
        assertEquals("1", GrowthCalendar.keyOf(1L));
        assertEquals("2", GrowthCalendar.keyOf(2L));
        assertNull(GrowthCalendar.keyOf(null));
    }
}
