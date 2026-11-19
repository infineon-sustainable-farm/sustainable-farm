package com.infineonbit.sustainablefarm.modules.farm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneBlockMappingCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneBlockMappingResponse;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import com.infineonbit.sustainablefarm.modules.farm.entity.Zone;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmZoneRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Zone to Plants block links backed by Zone -> Block: the link is the code of the zone's block. */
@ExtendWith(MockitoExtension.class)
class ZoneBlockMappingServiceTest {
    @Mock
    private FarmZoneRepository zoneRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private BlockReferences blockReferences;

    @Test
    void createGivesTheNormalizedCodeToTheZoneBlock() {
        Zone zone = zone(10L, block(20L, farm(1L), null));
        when(farmRepository.existsById(1L)).thenReturn(true);
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(zone));
        when(blockRepository.findByCode("B2")).thenReturn(Optional.empty());

        ZoneBlockMappingResponse created = service().create(new ZoneBlockMappingCreateRequest(10L, "  b2 ", 1L));

        assertEquals("B2", created.blockCode());
        assertEquals(10L, created.zoneId());
        assertEquals(10L, created.id());
        assertEquals(1L, created.farmId());
        assertEquals("B2", zone.getBlock().getCode());
    }

    @Test
    void createRejectsAFarmThatDoesNotOwnTheZone() {
        when(farmRepository.existsById(2L)).thenReturn(true);
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(zone(10L, block(20L, farm(1L), null))));

        assertThrows(BadRequestException.class,
                () -> service().create(new ZoneBlockMappingCreateRequest(10L, "A", 2L)));
        verify(blockRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsUnknownFarm() {
        when(farmRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service().create(new ZoneBlockMappingCreateRequest(10L, "A", 1L)));
    }

    @Test
    void createRejectsAZoneAlreadyLinkedOrACodeOfAnotherBlock() {
        when(farmRepository.existsById(1L)).thenReturn(true);
        Farm farm = farm(1L);
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(zone(10L, block(20L, farm, "A"))));
        when(zoneRepository.findById(11L)).thenReturn(Optional.of(zone(11L, block(21L, farm, null))));
        when(blockRepository.findByCode("A")).thenReturn(Optional.of(block(20L, farm, "A")));

        assertThrows(ConflictException.class, () -> service().create(new ZoneBlockMappingCreateRequest(10L, "a", 1L)));
        assertThrows(ConflictException.class, () -> service().create(new ZoneBlockMappingCreateRequest(10L, "B", 1L)));
        assertThrows(ConflictException.class, () -> service().create(new ZoneBlockMappingCreateRequest(11L, "A", 1L)));
    }

    @Test
    void findAllListsTheZonesOfCodedBlocksWithFilters() {
        Farm farm = farm(1L);
        when(zoneRepository.findByBlockCodeIsNotNull())
                .thenReturn(List.of(zone(10L, block(20L, farm, "A")), zone(11L, block(21L, farm, "B"))));

        assertEquals(2, service().findAll(null, null, null).size());
        assertEquals(List.of(11L), service().findAll(null, "b", null).stream().map(ZoneBlockMappingResponse::zoneId).toList());
        assertEquals(0, service().findAll(null, null, 9L).size());
    }

    @Test
    void deleteClearsTheBlockCode() {
        Block block = block(20L, farm(1L), "A");
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(zone(10L, block)));

        service().delete(10L);

        assertNull(block.getCode());
    }

    @Test
    void deleteKeepsTheCodeOfABlockThatOtherModulesReference() {
        Block block = block(20L, farm(1L), "A");
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(zone(10L, block)));
        when(blockReferences.isReferenced(20L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service().delete(10L));

        assertEquals("A", block.getCode());
        verify(blockRepository, never()).save(any());
    }

    @Test
    void getOfAnUnlinkedZoneIsNotFound() {
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(zone(10L, block(20L, farm(1L), null))));

        assertThrows(ResourceNotFoundException.class, () -> service().get(10L));
    }

    @Test
    void normalizeBlockCodeTreatsBlankAsNull() {
        assertNull(FarmDirectory.normalizeBlockCode(" "));
        assertEquals("A1", FarmDirectory.normalizeBlockCode("a1"));
    }

    private static Farm farm(Long id) {
        Farm farm = new Farm();
        farm.setId(id);
        return farm;
    }

    private static Block block(Long id, Farm farm, String code) {
        Block block = new Block();
        block.setId(id);
        block.setFarm(farm);
        block.setCode(code);
        block.setName("Field " + id);
        return block;
    }

    private static Zone zone(Long id, Block block) {
        Zone zone = new Zone();
        zone.setId(id);
        zone.setBlock(block);
        zone.setName("Zone " + id);
        return zone;
    }

    private ZoneBlockMappingService service() {
        return new ZoneBlockMappingService(zoneRepository, blockRepository, farmRepository, blockReferences);
    }
}
