package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Translation between Water Supply zones ({@code zoneId}) and Plants blocks ({@code blockCode}),
 * read from the farm referential: a zone belongs to one block, whose code is the Plants block code.
 *
 * <p>The lookups keep returning lists (the former mapping table allowed several codes per zone):
 * a zone now has zero or one code. Block codes are normalized to upper case.</p>
 */
@Component
public class ZoneBlockResolver {
    private final FarmDirectory farmDirectory;

    public ZoneBlockResolver(FarmDirectory farmDirectory) {
        this.farmDirectory = farmDirectory;
    }

    /** Block code of a zone as a list; empty for an unlinked, unknown or {@code null} zone. */
    public List<String> blockCodesForZone(Long zoneId) {
        return farmDirectory.findZone(zoneId).map(ZoneDto::blockCode).filter(Objects::nonNull)
                .map(List::of).orElse(List.of());
    }

    /** Block codes of several zones in one lookup; unlinked zones are absent from the map. */
    public Map<Long, List<String>> blockCodesForZones(Collection<Long> zoneIds) {
        return farmDirectory.listZones(zoneIds).stream()
                .filter(zone -> zone.blockCode() != null)
                .collect(Collectors.toMap(ZoneDto::id, zone -> List.of(zone.blockCode())));
    }

    /** Zones of a block, optionally restricted to one farm ({@code null} = every farm). */
    public List<Long> zoneIdsForBlock(Long farmId, String blockCode) {
        return zonesOfBlock(farmId, blockCode).stream().map(ZoneDto::id).toList();
    }

    /** Farm owning a block, as a list (empty when no block of the farm carries the code). */
    public List<Long> farmIdsForBlock(Long farmId, String blockCode) {
        String code = requireBlockCode(blockCode);
        return farmDirectory.findBlockByCode(code)
                .filter(block -> farmId == null || farmId.equals(block.farmId()))
                .map(block -> List.of(block.farmId()))
                .orElse(List.of());
    }

    /** Upper-case block code, the form stored on the block; {@code null} for a blank value. */
    public static String normalizeBlockCode(String blockCode) {
        return FarmDirectory.normalizeBlockCode(blockCode);
    }

    private List<ZoneDto> zonesOfBlock(Long farmId, String blockCode) {
        String code = requireBlockCode(blockCode);
        return farmDirectory.findBlockByCode(code)
                .filter(block -> farmId == null || farmId.equals(block.farmId()))
                .map(block -> farmDirectory.listZonesOfBlock(block.id()))
                .orElse(List.of());
    }

    private static String requireBlockCode(String blockCode) {
        String normalized = normalizeBlockCode(blockCode);
        if (normalized == null) {
            throw new IllegalArgumentException("blockCode must not be blank");
        }
        return normalized;
    }
}
