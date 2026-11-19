package com.infineonbit.sustainablefarm.modules.farm.api;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Read access to the site referential (farms, blocks, zones) for the other modules (ADR 0003, ADR 0004).
 *
 * <p>Implemented by the farm module; the writes stay inside it.</p>
 */
public interface FarmDirectory {

    /**
     * Tables of the referential that the other modules reference by foreign key, on {@link #KEY_COLUMN}.
     * A module stores the id in a plain column and declares the constraint as a
     * {@code core.schema.CrossModuleForeignKey} bean.
     */
    String FARM_TABLE = "farm";
    String BLOCK_TABLE = "block";
    String ZONE_TABLE = "zone";

    /** Primary key column of {@link #FARM_TABLE}, {@link #BLOCK_TABLE} and {@link #ZONE_TABLE}. */
    String KEY_COLUMN = "id";

    Optional<FarmDto> findFarm(Long farmId);

    boolean farmExists(Long farmId);

    List<FarmDto> listFarms();

    Optional<BlockDto> findBlock(Long blockId);

    /** Block carrying a code; the code is normalized ({@link #normalizeBlockCode}) before the lookup. */
    Optional<BlockDto> findBlockByCode(String code);

    /** Blocks of several ids in one lookup; unknown ids are absent from the result. */
    List<BlockDto> listBlocks(Collection<Long> blockIds);

    /** Farm of a name, ignoring case and surrounding spaces; the lowest id when several farms share it. */
    Optional<FarmDto> findFarmByName(String name);

    /** Blocks of a farm, empty for an unknown farm. */
    List<BlockDto> listBlocksOfFarm(Long farmId);

    Optional<ZoneDto> findZone(Long zoneId);

    boolean zoneExists(Long zoneId);

    List<ZoneDto> listZones();

    /** Zones of several ids in one lookup; unknown ids are absent from the result. */
    List<ZoneDto> listZones(Collection<Long> zoneIds);

    /** Zones of a block, empty for an unknown block. */
    List<ZoneDto> listZonesOfBlock(Long blockId);

    /** Zones of every block of a farm, empty for an unknown farm. */
    List<ZoneDto> listZonesOfFarm(Long farmId);

    /** Block code as stored: trimmed and in upper case; {@code null} for a blank value. */
    static String normalizeBlockCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
