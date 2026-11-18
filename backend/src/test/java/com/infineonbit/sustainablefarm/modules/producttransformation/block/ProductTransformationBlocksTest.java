package com.infineonbit.sustainablefarm.modules.producttransformation.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockRegistry;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The farm and block texts of Product Transformation resolved through the site referential. */
class ProductTransformationBlocksTest {

    private final FarmDirectory farmDirectory = mock(FarmDirectory.class);
    private final BlockRegistry blockRegistry = mock(BlockRegistry.class);
    private final ProductTransformationBlocks blocks = new ProductTransformationBlocks(farmDirectory, blockRegistry);

    @Test
    void anExistingCodeIsUsedWhateverTheFarmText() {
        when(farmDirectory.findBlockByCode("A")).thenReturn(Optional.of(block(1L, 1L, "A")));

        assertEquals(1L, blocks.resolve("Another farm", " a "));

        verify(blockRegistry, never()).findOrCreateBlock(any(), anyString());
    }

    @Test
    void aNewCodeGoesToTheFarmOfANumericFarmText() {
        when(farmDirectory.findBlockByCode("Q1")).thenReturn(Optional.empty());
        when(farmDirectory.findFarm(2L)).thenReturn(Optional.of(farm(2L, "North")));
        when(blockRegistry.findOrCreateBlock(2L, "Q1")).thenReturn(block(9L, 2L, "Q1"));

        assertEquals(9L, blocks.resolve("2", "q1"));
    }

    @Test
    void aNewCodeGoesToTheFarmOfThatName() {
        when(farmDirectory.findBlockByCode("Q1")).thenReturn(Optional.empty());
        when(farmDirectory.findFarmByName("ferme demo nord")).thenReturn(Optional.of(farm(1L, "Ferme Demo Nord")));
        when(blockRegistry.findOrCreateBlock(1L, "Q1")).thenReturn(block(9L, 1L, "Q1"));

        assertEquals(9L, blocks.resolve(" ferme demo nord ", "Q1"));
    }

    @Test
    void aNewCodeOfAnUnknownFarmGoesToTheDefaultFarm() {
        when(farmDirectory.findBlockByCode("Q1")).thenReturn(Optional.empty());
        when(farmDirectory.findFarmByName("FERME-DEMO")).thenReturn(Optional.empty());
        when(blockRegistry.findOrCreateBlock(null, "Q1")).thenReturn(block(9L, 1L, "Q1"));

        assertEquals(9L, blocks.resolve("FERME-DEMO", "Q1"));
    }

    @Test
    void aBlankBlockIsRefusedAndKeptOnAnUpdate() {
        assertThrows(BadRequestException.class, () -> blocks.resolve("Farm", " "));
        assertNull(blocks.resolveOrNull("Farm", null));
    }

    @Test
    void theLabelIsTheBlockCodeAndTheNameOfItsFarm() {
        when(farmDirectory.listBlocks(List.of(1L))).thenReturn(List.of(block(1L, 1L, "A")));
        when(farmDirectory.findFarm(1L)).thenReturn(Optional.of(farm(1L, "Ferme Demo Nord")));

        assertEquals(new ProductTransformationBlocks.Label("Ferme Demo Nord", "A"), blocks.label(1L));
    }

    @Test
    void anUnknownFarmHasNoBlock() {
        when(farmDirectory.findFarmByName("Nowhere")).thenReturn(Optional.empty());

        assertTrue(blocks.blockIdsOfFarm("Nowhere").isEmpty());
    }

    private static FarmDto farm(Long id, String name) {
        return new FarmDto(id, name, null, null, null, null, null, null);
    }

    private static BlockDto block(Long id, Long farmId, String code) {
        return new BlockDto(id, farmId, code, code, 0.0, null, null, null, null);
    }
}
