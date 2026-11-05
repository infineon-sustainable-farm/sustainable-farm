package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.GateEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.ZoneEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSecurityDataSeederTest {

    @Mock
    private GateRepository gateRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private CredentialUserRepository userRepository;

    @Mock
    private LogEntryRepository logRepository;

    @InjectMocks
    private SiteSecurityDataSeeder seeder;

    @Test
    void run_shouldSeedEachDataset_whenRepositoriesAreEmpty() {
        when(gateRepository.count()).thenReturn(0L);
        when(zoneRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(logRepository.count()).thenReturn(0L);

        seeder.run();

        verify(gateRepository).saveAll(any());
        verify(zoneRepository).saveAll(any());
        verify(userRepository).saveAll(any());
        verify(logRepository).saveAll(any());
    }

    @Test
    void run_shouldPreserveOriginalSeedValues() {
        when(gateRepository.count()).thenReturn(0L);
        when(zoneRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(logRepository.count()).thenReturn(0L);

        seeder.run();

        ArgumentCaptor<List<GateEntity>> gates = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<ZoneEntity>> zones = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<CredentialUserEntity>> users = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<LogEntryEntity>> logs = ArgumentCaptor.forClass(List.class);
        verify(gateRepository).saveAll(gates.capture());
        verify(zoneRepository).saveAll(zones.capture());
        verify(userRepository).saveAll(users.capture());
        verify(logRepository).saveAll(logs.capture());

        assertEquals(List.of("g1", "g2", "g3", "g4"), gates.getValue().stream().map(GateEntity::getId).toList());
        assertEquals("Gate 1 · North Drive", gates.getValue().get(0).getName());
        assertEquals(List.of("open", "controlled", "restricted", "critical"),
                zones.getValue().stream().map(ZoneEntity::getId).toList());
        assertEquals("Amina Diallo", users.getValue().get(2).getName());
        assertEquals(List.of("l1", "l2", "l3", "l4", "l5", "l6"),
                logs.getValue().stream().map(LogEntryEntity::getId).toList());
        assertEquals("Denied", logs.getValue().get(1).getStatus());
    }

    @Test
    void run_shouldBeIdempotent_whenExecutedTwice() {
        when(gateRepository.count()).thenReturn(0L, 4L);
        when(zoneRepository.count()).thenReturn(0L, 4L);
        when(userRepository.count()).thenReturn(0L, 4L);
        when(logRepository.count()).thenReturn(0L, 6L);

        seeder.run();
        seeder.run();

        verify(gateRepository).saveAll(any());
        verify(zoneRepository).saveAll(any());
        verify(userRepository).saveAll(any());
        verify(logRepository).saveAll(any());
    }

    @Test
    void run_shouldSeedOnlyEmptyDatasets() {
        when(gateRepository.count()).thenReturn(0L);
        when(zoneRepository.count()).thenReturn(1L);
        when(userRepository.count()).thenReturn(0L);
        when(logRepository.count()).thenReturn(1L);

        seeder.run();

        verify(gateRepository).saveAll(any());
        verify(userRepository).saveAll(any());
        verify(zoneRepository, never()).saveAll(any());
        verify(logRepository, never()).saveAll(any());
    }

    @Test
    void run_shouldNotOverwriteExistingDatasets() {
        when(gateRepository.count()).thenReturn(1L);
        when(zoneRepository.count()).thenReturn(1L);
        when(userRepository.count()).thenReturn(1L);
        when(logRepository.count()).thenReturn(1L);

        seeder.run();

        verify(gateRepository, never()).saveAll(any());
        verify(zoneRepository, never()).saveAll(any());
        verify(userRepository, never()).saveAll(any());
        verify(logRepository, never()).saveAll(any());
    }
}
