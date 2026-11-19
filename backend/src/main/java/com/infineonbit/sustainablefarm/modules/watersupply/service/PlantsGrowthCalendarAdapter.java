package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.plants.api.BlockGrowthLookup;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@link PlantsGrowthPort} on the public interface of Plants, {@code plants.api.BlockGrowthLookup}
 * (docs/adr/0003): the growth calendar entries of a block, read in the same process.
 *
 * <p>A Plants failure never breaks irrigation: the adapter answers "unknown" and logs.</p>
 */
@Component
public class PlantsGrowthCalendarAdapter implements PlantsGrowthPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlantsGrowthCalendarAdapter.class);

    private final BlockGrowthLookup blockGrowthLookup;

    public PlantsGrowthCalendarAdapter(BlockGrowthLookup blockGrowthLookup) {
        this.blockGrowthLookup = blockGrowthLookup;
    }

    @Override
    public List<BlockGrowth> growthForBlock(String blockCode) {
        try {
            return blockGrowthLookup.growthOfBlock(blockCode).stream()
                    .map(entry -> new BlockGrowth(entry.blockCode(), entry.growthPhase(), entry.ageYears()))
                    .toList();
        } catch (RuntimeException ex) {
            LOGGER.warn("Plants growth calendar unavailable for block {}", blockCode, ex);
            return List.of();
        }
    }
}
