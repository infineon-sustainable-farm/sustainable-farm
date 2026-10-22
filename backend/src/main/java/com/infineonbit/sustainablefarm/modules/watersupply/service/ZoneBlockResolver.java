package com.infineonbit.sustainablefarm.modules.watersupply.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Translation between Water Supply zones ({@code zoneId}) and Plants blocks ({@code blockCode}).
 *
 * <p>The cardinality is not settled with Plants: a zone may carry several block codes, so the
 * lookups return lists. Block codes are normalized to upper case.</p>
 */
public interface ZoneBlockResolver {
    /** Block codes of a zone, sorted; empty for an unmapped or {@code null} zone. */
    List<String> blockCodesForZone(UUID zoneId);

    /** Block codes of several zones in one lookup; unmapped zones are absent from the map. */
    Map<UUID, List<String>> blockCodesForZones(Collection<UUID> zoneIds);

    /** Zones mapped to a block, optionally restricted to one farm ({@code null} = every farm). */
    List<UUID> zoneIdsForBlock(UUID farmId, String blockCode);
}
