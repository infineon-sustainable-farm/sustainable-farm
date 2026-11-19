package com.infineonbit.sustainablefarm.modules.farm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.farm.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import com.infineonbit.sustainablefarm.modules.farm.repository.BlockRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.farm.repository.FarmZoneRepository;
import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {
    @Mock
    private FarmRepository farmRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private FarmZoneRepository zoneRepository;

    @Mock
    private VarietyCatalog varietyCatalog;

    @Test
    void createZoneRejectsUnknownField() {
        ZoneCreateRequest zone = new ZoneCreateRequest(7L, "Zone", 1.0, null, null, null, null);
        when(blockRepository.findById(7L)).thenReturn(Optional.empty());
        FarmService service = new FarmService(farmRepository, blockRepository, zoneRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.createZone(zone));
        verify(zoneRepository, never()).save(any());
    }

    @Test
    void createFieldRejectsUnknownFarm() {
        FieldCreateRequest request = new FieldCreateRequest(3L, "Field", 1.0, null, null, null, null);
        when(farmRepository.findById(3L)).thenReturn(Optional.empty());
        BlockService service = new BlockService(farmRepository, blockRepository, varietyCatalog, null);

        assertThrows(ResourceNotFoundException.class, () -> service.createField(request));
        verify(blockRepository, never()).save(any());
    }

    @Test
    void createFieldStoresTheCatalogueKeyOfItsCropType() {
        FieldCreateRequest request = new FieldCreateRequest(3L, "Field", 1.0, "Amélie", null, null, null);
        when(farmRepository.findById(3L)).thenReturn(Optional.of(new Farm()));
        when(varietyCatalog.resolve("Amélie")).thenReturn(Optional.of(new CatalogVariety("AMELIE", "Amelie")));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BlockService service = new BlockService(farmRepository, blockRepository, varietyCatalog, null);

        service.createField(request);

        ArgumentCaptor<Block> saved = ArgumentCaptor.forClass(Block.class);
        verify(blockRepository).save(saved.capture());
        assertEquals("AMELIE", saved.getValue().getCropType());
    }

    @Test
    void createFieldRejectsACropTypeOutsideThePlantsCatalogue() {
        FieldCreateRequest request = new FieldCreateRequest(3L, "Field", 1.0, "mangue", null, null, null);
        when(farmRepository.findById(3L)).thenReturn(Optional.of(new Farm()));
        when(varietyCatalog.resolve("mangue")).thenReturn(Optional.empty());
        BlockService service = new BlockService(farmRepository, blockRepository, varietyCatalog, null);

        assertThrows(BadRequestException.class, () -> service.createField(request));
        verify(blockRepository, never()).save(any());
    }
}
