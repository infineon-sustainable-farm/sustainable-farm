package com.infineonbit.sustainablefarm.modules.producttransformation.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The farm and block texts of Product Transformation resolved through the site referential. */
class ProductTransformationBlocksTest {

    private final FarmDirectory farmDirectory = mock(FarmDirectory.class);
    private final ProductTransformationBlocks blocks = new ProductTransformationBlocks(farmDirectory);

    @Test
    void anExistingCodeWithoutFarmTextIsUsed() {
        when(farmDirectory.findBlockByCode("A")).thenReturn(Optional.of(block(1L, 1L, "A")));

        assertEquals(1L, blocks.resolve(null, " a "));
    }

    @Test
    void anExistingCodeOfTheFarmOfANumericFarmTextIsUsed() {
        when(farmDirectory.findBlockByCode("Q1")).thenReturn(Optional.of(block(9L, 2L, "Q1")));
        when(farmDirectory.findFarm(2L)).thenReturn(Optional.of(farm(2L, "North")));

        assertEquals(9L, blocks.resolve("2", "q1"));
    }

    @Test
    void anExistingCodeOfTheFarmOfThatNameIsUsed() {
        when(farmDirectory.findBlockByCode("Q1")).thenReturn(Optional.of(block(9L, 1L, "Q1")));
        when(farmDirectory.findFarmByName("ferme demo nord")).thenReturn(Optional.of(farm(1L, "Ferme Demo Nord")));

        assertEquals(9L, blocks.resolve(" ferme demo nord ", "Q1"));
    }

    @Test
    void anUnknownCodeIsRefusedAndNoBlockIsCreated() {
        when(farmDirectory.findBlockByCode("Q1")).thenReturn(Optional.empty());

        BadRequestException refused = assertThrows(BadRequestException.class, () -> blocks.resolve("1", "Q1"));

        assertTrue(refused.getMessage().contains("Unknown block Q1"));
    }

    @Test
    void anUnknownFarmTextIsRefused() {
        when(farmDirectory.findBlockByCode("A")).thenReturn(Optional.of(block(1L, 1L, "A")));
        when(farmDirectory.findFarmByName("FERME-DEMO")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> blocks.resolve("FERME-DEMO", "A"));
    }

    @Test
    void aBlockOfAnotherFarmIsRefused() {
        when(farmDirectory.findBlockByCode("A")).thenReturn(Optional.of(block(1L, 1L, "A")));
        when(farmDirectory.findFarm(2L)).thenReturn(Optional.of(farm(2L, "North")));

        assertThrows(BadRequestException.class, () -> blocks.resolve("2", "A"));
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
