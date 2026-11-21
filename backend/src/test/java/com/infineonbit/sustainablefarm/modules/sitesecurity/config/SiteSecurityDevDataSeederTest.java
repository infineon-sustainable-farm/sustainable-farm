package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.staff.api.InMemoryPersonDirectory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSecurityDevDataSeederTest {

    @Mock
    private GateRepository gateRepository;

    @Mock
    private CredentialUserRepository userRepository;

    @Mock
    private LogEntryRepository logRepository;

    private InMemoryPersonDirectory people;
    private SiteSecurityDevDataSeeder seeder;

    @BeforeEach
    void setUp() {
        people = new InMemoryPersonDirectory();
        seeder = new SiteSecurityDevDataSeeder(gateRepository, userRepository, logRepository, people);
    }

    @Test
    void seed_shouldNotSeed_whenDataAlreadyExists() {
        people.add("Awa Traoré");
        when(gateRepository.count()).thenReturn(4L);
        when(userRepository.count()).thenReturn(4L);
        when(logRepository.count()).thenReturn(6L);

        seeder.seed();

        verify(gateRepository, never()).saveAll(any());
        verify(userRepository, never()).saveAll(any());
        verify(logRepository, never()).saveAll(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void seed_shouldReferenceExistingPeople_andNeverCreateOne() {
        Long first = people.add("Awa Traoré").id();
        Long second = people.add("Issouf Coulibaly").id();
        when(gateRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(logRepository.count()).thenReturn(0L);

        seeder.seed();

        ArgumentCaptor<List<CredentialUserEntity>> credentials = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(credentials.capture());
        assertEquals(List.of(first, second),
                credentials.getValue().stream().map(CredentialUserEntity::getPersonId).toList());

        ArgumentCaptor<List<LogEntryEntity>> logs = ArgumentCaptor.forClass(List.class);
        verify(logRepository).saveAll(logs.capture());
        assertEquals(6, logs.getValue().size());
        assertNull(logs.getValue().get(1).getPersonId(), "the unidentified subject has no person");
        assertTrue(logs.getValue().stream().map(LogEntryEntity::getPersonId)
                .allMatch(id -> id == null || id.equals(first) || id.equals(second)));
        assertEquals(2, people.listActive().size(), "no person is created");
    }

    @Test
    void seed_shouldSkipCredentials_whenTheDirectoryIsEmpty() {
        when(gateRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(logRepository.count()).thenReturn(0L);

        seeder.seed();

        verify(gateRepository).saveAll(any());
        verify(userRepository, never()).saveAll(any());
        verify(logRepository).saveAll(any());
    }
}
