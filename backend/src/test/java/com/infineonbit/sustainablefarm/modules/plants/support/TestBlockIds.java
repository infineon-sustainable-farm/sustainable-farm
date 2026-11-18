package com.infineonbit.sustainablefarm.modules.plants.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Block ids for the repository tests, which run without the farm referential: one id per
 * (farm, code) pair used by a test, so the filters of a list route can be written as the ids of
 * the blocks of a farm, of a code, or both, as {@code PlantsBlocks.filter} computes them.
 * A {@code null} farm is a farm of its own here.
 */
public final class TestBlockIds {

    private record Pair(Long farmId, String code) {
    }

    private final Map<Pair, Long> ids = new LinkedHashMap<>();

    /** Id of the block of a code on a farm, the same for the same pair. */
    public Long of(Long farmId, String code) {
        return ids.computeIfAbsent(new Pair(farmId, code),
                pair -> TestBlocks.idOf(code) + (farmId == null ? 0 : 1000 * farmId));
    }

    /** Whether the filter keeps every block: neither farm nor code. */
    public boolean all(Long farmId, String code) {
        return farmId == null && code == null;
    }

    /** Ids of the blocks used so far that match the farm and the code ({@code null} for any); never empty. */
    public List<Long> ids(Long farmId, String code) {
        List<Long> matching = ids.entrySet().stream()
                .filter(entry -> farmId == null || Objects.equals(farmId, entry.getKey().farmId()))
                .filter(entry -> code == null || code.equals(entry.getKey().code()))
                .map(Map.Entry::getValue)
                .toList();
        return matching.isEmpty() ? List.of(-1L) : matching;
    }
}
