package com.infineonbit.sustainablefarm.modules.farm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockRegistry;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

/** A code designates the block of that code; a new code creates the block under its farm or the default farm. */
@ExtendWith(MockitoExtension.class)
class BlockRegistryServiceTest {
    @Mock
    private FarmRepository farmRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void anExistingCodeIsTheExistingBlockWhateverTheCase() {
        when(blockRepository.findByCode("A")).thenReturn(Optional.of(block(5L, farm(1L), "A")));

        BlockDto block = service().findOrCreateBlock(null, " a ");

        assertEquals(5L, block.id());
        assertEquals(1L, block.farmId());
        verify(blockRepository, never()).saveAndFlush(any());
    }

    @Test
    void anExistingCodeOfAnotherFarmIsRefused() {
        when(blockRepository.findByCode("A")).thenReturn(Optional.of(block(5L, farm(1L), "A")));

        assertThrows(BadRequestException.class, () -> service().findOrCreateBlock(2L, "A"));
    }

    @Test
    void aNewCodeWithoutFarmGoesToTheFarmOfTheLowestId() {
        Farm first = farm(1L);
        when(blockRepository.findByCode("C")).thenReturn(Optional.empty());
        when(farmRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(first));
        when(blockRepository.saveAndFlush(any(Block.class))).thenAnswer(call -> withId(call.getArgument(0), 9L));

        BlockDto block = service().findOrCreateBlock(null, "c");

        assertEquals("C", block.code());
        assertEquals("C", block.name());
        assertEquals(0.0, block.areaHectares());
        assertEquals(1L, block.farmId());
    }

    @Test
    void aNewCodeWithoutAnyFarmCreatesTheDefaultFarm() {
        when(blockRepository.findByCode("C")).thenReturn(Optional.empty());
        when(farmRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(farmRepository.save(any(Farm.class))).thenAnswer(call -> {
            Farm farm = call.getArgument(0);
            farm.setId(3L);
            return farm;
        });
        when(blockRepository.saveAndFlush(any(Block.class))).thenAnswer(call -> withId(call.getArgument(0), 9L));

        BlockDto block = service().findOrCreateBlock(null, "C");

        assertEquals(3L, block.farmId());
        verify(farmRepository).save(org.mockito.ArgumentMatchers.argThat(
                farm -> BlockRegistry.DEFAULT_FARM_NAME.equals(farm.getName())));
    }

    @Test
    void aNewCodeOfAnUnknownFarmIsRefused() {
        when(blockRepository.findByCode("C")).thenReturn(Optional.empty());
        when(farmRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service().findOrCreateBlock(7L, "C"));
    }

    @Test
    void aNewCodeCreatedAtTheSameTimeByAnotherWriteIsTheBlockOfThatWrite() {
        when(blockRepository.findByCode("C")).thenReturn(Optional.empty(), Optional.of(block(9L, farm(1L), "C")));
        when(farmRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(farm(1L)));
        when(blockRepository.saveAndFlush(any(Block.class))).thenThrow(new DataIntegrityViolationException("uk_block_code"));

        BlockDto block = service().findOrCreateBlock(null, "C");

        assertEquals(9L, block.id());
    }

    @Test
    void aBlankCodeIsRefused() {
        assertThrows(BadRequestException.class, () -> service().findOrCreateBlock(null, " "));
    }

    private static Block withId(Block block, Long id) {
        block.setId(id);
        return block;
    }

    private static Farm farm(Long id) {
        Farm farm = new Farm();
        farm.setId(id);
        farm.setName("Farm " + id);
        return farm;
    }

    private static Block block(Long id, Farm farm, String code) {
        Block block = new Block();
        block.setId(id);
        block.setFarm(farm);
        block.setCode(code);
        block.setName("Field " + id);
        block.setAreaHectares(1.0);
        return block;
    }

    private BlockRegistryService service() {
        return new BlockRegistryService(farmRepository, blockRepository, transactionManager);
    }
}
