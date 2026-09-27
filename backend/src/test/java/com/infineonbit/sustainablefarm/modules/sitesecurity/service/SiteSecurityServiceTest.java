package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.CreateUserRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.GateDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.LogEntryDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.OverviewResponseDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.UserDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.ZoneDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.GateEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.ZoneEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SiteSecurityServiceTest {

    @Mock
    private GateRepository gateRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private CredentialUserRepository userRepository;

    @Mock
    private LogEntryRepository logRepository;

    @InjectMocks
    private SiteSecurityService siteSecurityService;

    private CreateUserRequestDto validRequest() {
        return CreateUserRequestDto.builder()
                .name("John Doe")
                .role("Technician")
                .type("Staff")
                .level("restricted")
                .validUntil(LocalDate.now().plusYears(1).toString())
                .build();
    }

    // --- initDefaultData idempotence ---

    @Test
    void initDefaultData_shouldNotSeed_whenDataAlreadyExists() {
        when(gateRepository.count()).thenReturn(4L);
        when(zoneRepository.count()).thenReturn(4L);
        when(userRepository.count()).thenReturn(4L);
        when(logRepository.count()).thenReturn(6L);

        siteSecurityService.initDefaultData();

        verify(gateRepository, never()).saveAll(any());
        verify(zoneRepository, never()).saveAll(any());
        verify(userRepository, never()).saveAll(any());
        verify(logRepository, never()).saveAll(any());
    }

    @Test
    void initDefaultData_shouldSeed_whenRepositoriesAreEmpty() {
        when(gateRepository.count()).thenReturn(0L);
        when(zoneRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(logRepository.count()).thenReturn(0L);

        siteSecurityService.initDefaultData();

        verify(gateRepository).saveAll(any());
        verify(zoneRepository).saveAll(any());
        verify(userRepository).saveAll(any());
        verify(logRepository).saveAll(any());
    }

    // --- createCredential ---

    @Test
    void createCredential_shouldCreate_whenRequestIsValid() {
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(zoneRepository.existsById("restricted")).thenReturn(true);
        when(userRepository.save(any(CredentialUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = siteSecurityService.createCredential(validRequest());

        assertEquals("John Doe", result.getName());
        assertEquals("restricted", result.getLevel());
        assertEquals("Active", result.getStatus());
        assertEquals("JD", result.getInitials());
        assertNotNull(result.getId());

        ArgumentCaptor<CredentialUserEntity> captor = ArgumentCaptor.forClass(CredentialUserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals("John Doe", captor.getValue().getName());
    }

    @Test
    void createCredential_shouldThrow_whenNameAlreadyExists() {
        when(userRepository.existsByName("John Doe")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> siteSecurityService.createCredential(validRequest()));

        assertEquals("A credential for 'John Doe' already exists.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createCredential_shouldThrow_whenLevelIsNotAKnownZone() {
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(zoneRepository.existsById("restricted")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> siteSecurityService.createCredential(validRequest()));

        assertTrue(ex.getMessage().contains("Unknown access level 'restricted'"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createCredential_shouldThrow_whenTypeIsNotAllowed() {
        CreateUserRequestDto req = validRequest().toBuilder().type("Superuser").build();
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(zoneRepository.existsById("restricted")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> siteSecurityService.createCredential(req));

        assertTrue(ex.getMessage().contains("Invalid credential type 'Superuser'"));
    }

    @Test
    void createCredential_shouldThrow_whenStatusIsNotAllowed() {
        CreateUserRequestDto req = validRequest().toBuilder().status("GodMode").build();
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(zoneRepository.existsById("restricted")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> siteSecurityService.createCredential(req));

        assertTrue(ex.getMessage().contains("Invalid status 'GodMode'"));
    }

    @Test
    void createCredential_shouldThrow_whenValidUntilIsInPast() {
        CreateUserRequestDto req = validRequest().toBuilder().validUntil("2020-01-01").build();
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(zoneRepository.existsById("restricted")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> siteSecurityService.createCredential(req));

        assertTrue(ex.getMessage().contains("in the past"));
    }

    @Test
    void createCredential_shouldThrow_whenValidUntilIsMalformed() {
        CreateUserRequestDto req = validRequest().toBuilder().validUntil("tomorrow-ish").build();
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(zoneRepository.existsById("restricted")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> siteSecurityService.createCredential(req));

        assertTrue(ex.getMessage().contains("Invalid validUntil"));
    }

    @Test
    void createCredential_shouldAcceptPermanentValidUntil() {
        CreateUserRequestDto req = validRequest().toBuilder().validUntil("Permanent").build();
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(zoneRepository.existsById("restricted")).thenReturn(true);
        when(userRepository.save(any(CredentialUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = siteSecurityService.createCredential(req);

        assertEquals("Permanent", result.getValidUntil());
    }

    // --- getAccessLogs filtering ---

    @Test
    void getAccessLogs_shouldReturnAll_whenFilterIsAll() {
        when(logRepository.findAll()).thenReturn(Arrays.asList(logEntry("l1", "Approved", "Staff")));
        List<LogEntryDto> result = siteSecurityService.getAccessLogs("all");
        assertEquals(1, result.size());
        assertEquals("l1", result.get(0).getId());
    }

    @Test
    void getAccessLogs_shouldFilterByStatus() {
        when(logRepository.findByStatus("Denied")).thenReturn(Arrays.asList(logEntry("l2", "Denied", "Visitor")));
        List<LogEntryDto> result = siteSecurityService.getAccessLogs("denied");
        assertEquals(1, result.size());
        assertEquals("Denied", result.get(0).getStatus());
    }

    @Test
    void getAccessLogs_shouldFilterByType() {
        when(logRepository.findByType("Visitor")).thenReturn(Arrays.asList(logEntry("l3", "Approved", "Visitor")));
        List<LogEntryDto> result = siteSecurityService.getAccessLogs("visitors");
        assertEquals(1, result.size());
        assertEquals("Visitor", result.get(0).getType());
    }

    // --- DTO mapping ---

    @Test
    void getZones_shouldMapEntitiesToDtos() {
        ZoneEntity zone = ZoneEntity.builder()
                .id("critical").name("Critical Zone").tagline("Infrastructure")
                .authorizedUsers(2).entryPoints(1).status("Locked Down")
                .description("desc").requirements(Arrays.asList("Level 4 clearance"))
                .build();
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        List<ZoneDto> result = siteSecurityService.getZones();

        assertEquals(1, result.size());
        ZoneDto dto = result.get(0);
        assertEquals("critical", dto.getId());
        assertEquals("Critical Zone", dto.getName());
        assertEquals(2, dto.getAuthorizedUsers());
        assertEquals(1, dto.getRequirements().size());
    }

    @Test
    void getZones_shouldDefaultRequirementsToEmptyList() {
        ZoneEntity zone = ZoneEntity.builder().id("open").name("Open Zone").build();
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        List<ZoneDto> result = siteSecurityService.getZones();

        assertNotNull(result.get(0).getRequirements());
        assertTrue(result.get(0).getRequirements().isEmpty());
    }

    @Test
    void getOverview_shouldMapGatesAndStats() {
        GateEntity gate = GateEntity.builder()
                .id("g1").name("Gate 1 · North Drive").shortName("Gate 1")
                .zone("open").status("Active").detail("Main intake").build();
        when(gateRepository.findAll()).thenReturn(Arrays.asList(gate));

        OverviewResponseDto result = siteSecurityService.getOverview();

        assertEquals(1, result.getGates().size());
        GateDto gateDto = result.getGates().get(0);
        assertEquals("g1", gateDto.getId());
        assertEquals("Gate 1", gateDto.getShortName());
        assertNotNull(result.getStats());
    }

    @Test
    void getCredentials_shouldMapEntitiesToDtos() {
        CredentialUserEntity user = CredentialUserEntity.builder()
                .id("u1").name("Amina Diallo").role("Security Lead").type("Security")
                .level("critical").validUntil("Permanent").status("Active")
                .initials("AD").lastActive("Just now").build();
        when(userRepository.findAll()).thenReturn(Arrays.asList(user));

        List<UserDto> result = siteSecurityService.getCredentials();

        assertEquals(1, result.size());
        assertEquals("Amina Diallo", result.get(0).getName());
        assertEquals("critical", result.get(0).getLevel());
    }

    private LogEntryEntity logEntry(String id, String status, String type) {
        return LogEntryEntity.builder()
                .id(id).ref("LOG-" + id).time("10:00:00").date("2026-09-05")
                .user("Someone").initials("SO").type(type).zone("open")
                .zoneName("Open Zone").action("Entry").status(status)
                .gate("Gate 1").method("RFID Pass").note("note").build();
    }
}
