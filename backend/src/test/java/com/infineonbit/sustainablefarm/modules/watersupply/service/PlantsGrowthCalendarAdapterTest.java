package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.plants.api.BlockGrowthLookup;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The only class reading Plants: maps its public interface and never lets a Plants failure through. */
@ExtendWith(MockitoExtension.class)
class PlantsGrowthCalendarAdapterTest {
    @Mock
    private BlockGrowthLookup blockGrowthLookup;

    @Test
    void mapsTheGrowthCalendarEntriesOfTheBlock() {
        when(blockGrowthLookup.growthOfBlock("A"))
                .thenReturn(List.of(new BlockGrowthLookup.BlockGrowth("A", "full production", 7)));

        List<PlantsGrowthPort.BlockGrowth> growth = new PlantsGrowthCalendarAdapter(blockGrowthLookup).growthForBlock("A");

        assertEquals(List.of(new PlantsGrowthPort.BlockGrowth("A", "full production", 7)), growth);
    }

    @Test
    void aPlantsFailureIsReportedAsUnknownGrowth() {
        when(blockGrowthLookup.growthOfBlock("A")).thenThrow(new IllegalStateException("db down"));

        assertTrue(new PlantsGrowthCalendarAdapter(blockGrowthLookup).growthForBlock("A").isEmpty());
    }
}
