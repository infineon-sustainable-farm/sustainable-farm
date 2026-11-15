package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthCalendarResponse;
import com.infineonbit.sustainablefarm.modules.plants.service.GrowthCalendarService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The only class reading Plants code: maps its public contract and never lets a Plants failure through. */
@ExtendWith(MockitoExtension.class)
class PlantsGrowthCalendarAdapterTest {
    @Mock
    private GrowthCalendarService growthCalendarService;

    @Test
    void mapsTheGrowthCalendarEntriesOfTheBlock() {
        when(growthCalendarService.getAllGrowthCalendarEntries(null, "A")).thenReturn(List.of(new GrowthCalendarResponse(
                1L, 3, "A", List.of("Kent"), LocalDate.of(2019, 6, 1), "day", 7, 4, "full production", "6+ yrs",
                null, null, null, "seed", Instant.now())));

        List<PlantsGrowthPort.BlockGrowth> growth = new PlantsGrowthCalendarAdapter(growthCalendarService).growthForBlock("A");

        assertEquals(List.of(new PlantsGrowthPort.BlockGrowth("A", "full production", 7)), growth);
    }

    @Test
    void aPlantsFailureIsReportedAsUnknownGrowth() {
        when(growthCalendarService.getAllGrowthCalendarEntries(null, "A")).thenThrow(new IllegalStateException("db down"));

        assertTrue(new PlantsGrowthCalendarAdapter(growthCalendarService).growthForBlock("A").isEmpty());
    }
}
