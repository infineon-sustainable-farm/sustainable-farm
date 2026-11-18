package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Zone to Plants block lookups, read from the farm referential (Zone -> Block -> code). */
@ExtendWith(MockitoExtension.class)
class ZoneBlockResolverTest {
    @Mock
    private FarmDirectory farmDirectory;

    @Test
    void zoneIdsForBlockReturnsTheZonesOfTheBlock() {
        when(farmDirectory.findBlockByCode("A")).thenReturn(Optional.of(block(20L, 1L, "A")));
        when(farmDirectory.listZonesOfBlock(20L)).thenReturn(List.of(zone(10L, 20L, 1L, "A"), zone(11L, 20L, 1L, "A")));

        assertEquals(List.of(10L, 11L), resolver().zoneIdsForBlock(null, "a"));
        assertEquals(List.of(10L, 11L), resolver().zoneIdsForBlock(1L, "a"));
    }

    @Test
    void zoneIdsForBlockOfAnotherFarmIsEmpty() {
        when(farmDirectory.findBlockByCode("A")).thenReturn(Optional.of(block(20L, 1L, "A")));

        assertTrue(resolver().zoneIdsForBlock(2L, "A").isEmpty());
        assertTrue(resolver().farmIdsForBlock(2L, "A").isEmpty());
    }

    @Test
    void farmIdsForBlockReturnsTheOwningFarm() {
        when(farmDirectory.findBlockByCode("B2")).thenReturn(Optional.of(block(21L, 3L, "B2")));

        assertEquals(List.of(3L), resolver().farmIdsForBlock(null, "b2"));
    }

    @Test
    void zoneIdsForBlockRejectsABlankBlockCode() {
        assertThrows(IllegalArgumentException.class, () -> resolver().zoneIdsForBlock(null, "  "));
    }

    @Test
    void blockCodesForZonesSkipsZonesWithoutCode() {
        when(farmDirectory.listZones(List.of(10L, 11L)))
                .thenReturn(List.of(zone(10L, 20L, 1L, "A"), zone(11L, 21L, 1L, null)));

        Map<Long, List<String>> blocks = resolver().blockCodesForZones(List.of(10L, 11L));

        assertEquals(Map.of(10L, List.of("A")), blocks);
    }

    @Test
    void blockCodesForZoneIsEmptyForAnUnknownZone() {
        when(farmDirectory.findZone(99L)).thenReturn(Optional.empty());

        assertTrue(resolver().blockCodesForZone(99L).isEmpty());
    }

    static ZoneDto zone(Long id, Long blockId, Long farmId, String blockCode) {
        return new ZoneDto(id, blockId, farmId, blockCode, "Zone " + id, 1.0, "drip", null, null, null, null);
    }

    private static BlockDto block(Long id, Long farmId, String code) {
        return new BlockDto(id, farmId, code, "Field " + id, 1.0, null, null, null, null);
    }

    private ZoneBlockResolver resolver() {
        return new ZoneBlockResolver(farmDirectory);
    }
}
