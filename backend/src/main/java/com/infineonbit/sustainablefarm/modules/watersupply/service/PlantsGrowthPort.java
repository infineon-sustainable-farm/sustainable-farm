package com.infineonbit.sustainablefarm.modules.watersupply.service;

import java.util.List;

/**
 * What Water Supply reads from the Plants module: the current growth phase of a block.
 *
 * <p>Kept behind an interface so that the only class depending on Plants code is the adapter
 * ({@link PlantsGrowthCalendarAdapter}); the irrigation logic never sees Plants types.</p>
 */
public interface PlantsGrowthPort {

    /** One growth calendar entry of a block, as Plants computes it from the planting date. */
    record BlockGrowth(String blockCode, String growthPhase, Integer ageYears) {}

    /** Entries Plants holds for the block (several farms or plantings may share a code); empty if unknown. */
    List<BlockGrowth> growthForBlock(String blockCode);
}
