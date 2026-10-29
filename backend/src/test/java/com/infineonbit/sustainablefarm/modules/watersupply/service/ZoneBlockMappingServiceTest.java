package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.ZoneBlockMapping;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneBlockMappingRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Plants zone-to-block mapping (1.1): normalization, farm consistency and the resolver lookups. */
@ExtendWith(MockitoExtension.class)
class ZoneBlockMappingServiceTest {
    @Mock
    private ZoneBlockMappingRepository mappingRepository;

    @Mock
    private FieldZoneRepository zoneRepository;

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private FarmRepository farmRepository;

    @Test
    void createNormalizesTheBlockCodeToUpperCase() {
        UUID farmId = UUID.randomUUID();
        UUID zoneId = stubZoneOfFarm(farmId);
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(new Farm()));
        when(mappingRepository.saveAndFlush(any(ZoneBlockMapping.class))).thenAnswer(call -> call.getArgument(0));

        ZoneBlockMappingResponse created = service().create(new ZoneBlockMappingCreateRequest(zoneId, "  b2 ", farmId));

        assertEquals("B2", created.blockCode());
        assertEquals(zoneId, created.zoneId());
    }

    @Test
    void createRejectsAFarmThatDoesNotOwnTheZone() {
        UUID zoneFarmId = UUID.randomUUID();
        UUID otherFarmId = UUID.randomUUID();
        UUID zoneId = stubZoneOfFarm(zoneFarmId);
        when(farmRepository.findById(otherFarmId)).thenReturn(Optional.of(new Farm()));

        assertThrows(IllegalArgumentException.class,
                () -> service().create(new ZoneBlockMappingCreateRequest(zoneId, "A", otherFarmId)));
        verify(mappingRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsUnknownFarm() {
        UUID farmId = UUID.randomUUID();
        when(farmRepository.findById(farmId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service().create(new ZoneBlockMappingCreateRequest(UUID.randomUUID(), "A", farmId)));
    }

    @Test
    void zoneIdsForBlockSearchesEveryFarmWhenNoFarmIsGiven() {
        UUID zoneA = UUID.randomUUID();
        UUID zoneB = UUID.randomUUID();
        when(mappingRepository.findByBlockCode("A")).thenReturn(List.of(mapping(zoneA, "A"), mapping(zoneB, "A"), mapping(zoneA, "A")));

        assertEquals(List.of(zoneA, zoneB), service().zoneIdsForBlock(null, "a"));
    }

    @Test
    void zoneIdsForBlockRejectsABlankBlockCode() {
        assertThrows(IllegalArgumentException.class, () -> service().zoneIdsForBlock(null, "  "));
    }

    @Test
    void blockCodesForZonesGroupsAndSortsInOneLookup() {
        UUID zoneA = UUID.randomUUID();
        UUID zoneB = UUID.randomUUID();
        when(mappingRepository.findByZoneIdIn(List.of(zoneA, zoneB)))
                .thenReturn(List.of(mapping(zoneA, "C"), mapping(zoneA, "A"), mapping(zoneB, "B")));

        Map<UUID, List<String>> blocks = service().blockCodesForZones(java.util.Arrays.asList(zoneA, null, zoneB, zoneA));

        assertEquals(List.of("A", "C"), blocks.get(zoneA));
        assertEquals(List.of("B"), blocks.get(zoneB));
    }

    @Test
    void blockCodesForZonesWithoutZoneDoesNotQuery() {
        assertTrue(service().blockCodesForZones(List.of()).isEmpty());
        verify(mappingRepository, never()).findByZoneIdIn(any());
    }

    @Test
    void blockCodesFollowThePlantsRule() {
        // Plants: 1 to 10 letters or digits, without prefix or space.
        for (String code : List.of("Block A", "ABCDEFGHIJK", "B-2")) {
            assertThrows(IllegalArgumentException.class, () -> service().zoneIdsForBlock(null, code), code);
        }
        assertEquals(List.of(), service().zoneIdsForBlock(null, "abcdefghij"));
    }

    @Test
    void aBlockAlreadyLinkedToAnotherZoneIsAConflict() {
        UUID farmId = UUID.randomUUID();
        UUID zoneId = stubZoneOfFarm(farmId);
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(new Farm()));
        ZoneBlockMapping other = mapping(UUID.randomUUID(), "A");
        other.setId(UUID.randomUUID());
        when(mappingRepository.findByFarmIdAndBlockCode(farmId, "A")).thenReturn(List.of(other));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().create(new ZoneBlockMappingCreateRequest(zoneId, "a", farmId)));

        // Plants: a zone may cover several blocks, but a block belongs to only one zone.
        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getReason().contains("another zone"));
        verify(mappingRepository, never()).saveAndFlush(any());
    }

    @Test
    void movingALinkKeepsItsOwnBlock() {
        UUID farmId = UUID.randomUUID();
        UUID zoneId = stubZoneOfFarm(farmId);
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(new Farm()));
        ZoneBlockMapping current = mapping(zoneId, "A");
        current.setId(UUID.randomUUID());
        current.setFarmId(farmId);
        when(mappingRepository.findById(current.getId())).thenReturn(Optional.of(current));
        when(mappingRepository.findByFarmIdAndBlockCode(farmId, "A")).thenReturn(List.of(current));
        when(mappingRepository.saveAndFlush(current)).thenReturn(current);

        assertEquals("A", service().update(current.getId(),
                new com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingUpdateRequest(null, "a", null)).blockCode());
    }

    @Test
    void normalizeBlockCodeTreatsBlankAsNull() {
        assertNull(ZoneBlockMappingService.normalizeBlockCode(" "));
        assertEquals("A1", ZoneBlockMappingService.normalizeBlockCode("a1"));
    }

    /** A zone whose field belongs to {@code farmId} (the farm itself is stubbed by each test). */
    private UUID stubZoneOfFarm(UUID farmId) {
        UUID zoneId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        Zone zone = new Zone();
        zone.setId(zoneId);
        zone.setFieldId(fieldId);
        Field field = new Field();
        field.setFarmId(farmId);
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
        return zoneId;
    }

    private static ZoneBlockMapping mapping(UUID zoneId, String blockCode) {
        ZoneBlockMapping mapping = new ZoneBlockMapping();
        mapping.setZoneId(zoneId);
        mapping.setBlockCode(blockCode);
        mapping.setFarmId(UUID.randomUUID());
        return mapping;
    }

    private ZoneBlockMappingService service() {
        return new ZoneBlockMappingService(mappingRepository, zoneRepository, fieldRepository, farmRepository);
    }
}
