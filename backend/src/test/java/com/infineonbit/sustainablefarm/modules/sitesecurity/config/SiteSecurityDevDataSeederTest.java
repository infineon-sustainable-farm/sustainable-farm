package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSecurityDevDataSeederTest {

    @Mock
    private GateRepository gateRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private CredentialUserRepository userRepository;

    @Mock
    private LogEntryRepository logRepository;

    @InjectMocks
    private SiteSecurityDevDataSeeder seeder;

    // --- idempotence ---

    @Test
    void seed_shouldNotSeed_whenDataAlreadyExists() {
        when(gateRepository.count()).thenReturn(4L);
        when(zoneRepository.count()).thenReturn(4L);
        when(userRepository.count()).thenReturn(4L);
        when(logRepository.count()).thenReturn(6L);

        seeder.seed();

        verify(gateRepository, never()).saveAll(any());
        verify(zoneRepository, never()).saveAll(any());
        verify(userRepository, never()).saveAll(any());
        verify(logRepository, never()).saveAll(any());
    }

    @Test
    void seed_shouldSeed_whenRepositoriesAreEmpty() {
        when(gateRepository.count()).thenReturn(0L);
        when(zoneRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(logRepository.count()).thenReturn(0L);

        seeder.seed();

        verify(gateRepository).saveAll(any());
        verify(zoneRepository).saveAll(any());
        verify(userRepository).saveAll(any());
        verify(logRepository).saveAll(any());
    }
}
