package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.api.BlockGrowthLookup;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The {@link BlockGrowthLookup} of plants: the entries of {@link GrowthCalendarService},
 * as {@code GET /api/plants/growth-calendar?blockCode=} gives them.
 */
@Service
public class BlockGrowthLookupService implements BlockGrowthLookup {

    private final GrowthCalendarService growthCalendarService;

    public BlockGrowthLookupService(GrowthCalendarService growthCalendarService) {
        this.growthCalendarService = growthCalendarService;
    }

    @Override
    public List<BlockGrowth> growthOfBlock(String blockCode) {
        return growthCalendarService.getAllGrowthCalendarEntries(null, blockCode).stream()
                .map(entry -> new BlockGrowth(entry.blockCode(), entry.growthPhase(), entry.ageYears()))
                .toList();
    }
}
