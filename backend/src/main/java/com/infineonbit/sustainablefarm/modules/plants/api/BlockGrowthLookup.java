package com.infineonbit.sustainablefarm.modules.plants.api;

import java.util.List;

/**
 * Read access to the growth of the planted blocks, for the other modules
 * (docs/adr/0003-communication-entre-modules.md).
 *
 * <p>Gives the same entries as {@code GET /api/plants/growth-calendar?blockCode=},
 * read in the same process.
 */
public interface BlockGrowthLookup {

    /**
     * One growth calendar entry of a block, as plants computes it from the planting date.
     *
     * @param blockCode   block code as stored, for example {@code "A"}
     * @param growthPhase growth phase computed from the tree age, for example {@code "full production"}
     * @param ageYears    age of the trees in whole years, or {@code null} without a planting date
     */
    record BlockGrowth(String blockCode, String growthPhase, Integer ageYears) {
    }

    /**
     * Lists the growth calendar entries of a block.
     *
     * @param blockCode block code, compared after trimming and upper-casing; {@code null} or blank for every block
     * @return the entries, possibly empty
     */
    List<BlockGrowth> growthOfBlock(String blockCode);
}
