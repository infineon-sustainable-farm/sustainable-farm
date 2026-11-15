package com.infineonbit.sustainablefarm.modules.plants.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The natural keys follow the planting lookups: a NULL farm is a value of its
 * own, the variety name ignores case only, and the block is taken as stored.
 */
class NaturalKeyTest {

    @Test
    void varietyKeyOf_shouldWriteZero_whenTheRowHasNoFarm() {
        assertEquals("0|A|keitt", Variety.keyOf(null, "A", "Keitt"));
    }

    @Test
    void varietyKeyOf_shouldKeepTheFarm_whenTheRowHasOne() {
        assertEquals("1|C|kent", Variety.keyOf(1, "C", "Kent"));
    }

    @Test
    void varietyKeyOf_shouldLowerTheCaseOnly_andKeepAccentsAndSpacesAsStored() {
        // The lookup compares LOWER(name) without trimming the stored name
        assertEquals("0|C|amélie ", Variety.keyOf(null, "C", "AMÉLIE "));
    }

    @Test
    void growthCalendarKeyOf_shouldKeepFarmNullAndFarm1Apart() {
        assertEquals("0|A", GrowthCalendar.keyOf(null, "A"));
        assertEquals("1|A", GrowthCalendar.keyOf(1, "A"));
    }
}
