package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.ZoneEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSecurityReferenceDataSeederTest {

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private SiteSecurityReferenceDataSeeder seeder;

    @Test
    void run_shouldCreateTheFourZones_whenNoneExists() {
        when(zoneRepository.count()).thenReturn(0L);

        seeder.run();

        ArgumentCaptor<List<ZoneEntity>> zones = ArgumentCaptor.forClass(List.class);
        verify(zoneRepository).saveAll(zones.capture());
        assertEquals(List.of("open", "controlled", "restricted", "critical"),
                zones.getValue().stream().map(ZoneEntity::getId).toList());
    }

    @Test
    void run_shouldNotTouchExistingZones() {
        when(zoneRepository.count()).thenReturn(1L);

        seeder.run();

        verify(zoneRepository, never()).saveAll(any());
    }
}
