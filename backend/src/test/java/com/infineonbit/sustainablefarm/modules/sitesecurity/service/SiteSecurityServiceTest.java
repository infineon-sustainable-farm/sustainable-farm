package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.CreateUserRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.GateDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.LogEntryDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.OverviewResponseDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.UserDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.ZoneDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.Permission;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialNotificationStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.GateEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.ZoneEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import com.infineonbit.sustainablefarm.modules.staff.api.InMemoryPersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSecurityServiceTest {

    @Mock
    private GateRepository gateRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private CredentialUserRepository userRepository;

    @Mock
    private LogEntryRepository logRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SiteSecurityAuditService audit;

    private static final SiteSecurityPrincipal ADMIN =
            new SiteSecurityPrincipal("operator-admin", SiteSecurityRole.ADMIN, Set.of(), Set.of());

    private InMemoryPersonDirectory people;
    private SiteSecurityService siteSecurityService;
    private Long johnId;

    @BeforeEach
    void setUp() {
        people = new InMemoryPersonDirectory();
        johnId = people.create(new PersonDetails("John Doe", "john.doe@example.test", null, null)).id();
        siteSecurityService = new SiteSecurityService(gateRepository, zoneRepository, userRepository, logRepository,
                eventPublisher, audit, people);
    }

    private CreateUserRequestDto validRequest() {
        return CreateUserRequestDto.builder()
                .personId(johnId)
                .role("Technician")
                .type("Staff")
                .level("restricted")
                .validUntil(LocalDate.now().plusYears(1).toString())
                .build();
    }

    // --- createCredential: the holder is a person of the shared directory ---

    @Test
    void createCredential_shouldStoreThePersonId_andCopyNoPersonalData() {
        when(zoneRepository.existsById("restricted")).thenReturn(true);
        when(userRepository.saveAndFlush(any(CredentialUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = siteSecurityService.createCredential(validRequest(), ADMIN);

        assertEquals(johnId, result.getPersonId());
        assertEquals("John Doe", result.getName());
        assertEquals("JD", result.getInitials());
        assertEquals("john.doe@example.test", result.getEmail());
        assertEquals("restricted", result.getLevel());
        assertEquals("Active", result.getStatus());
        assertEquals("pending", result.getNotificationStatus());
        assertNotNull(result.getId());

        ArgumentCaptor<CredentialUserEntity> captor = ArgumentCaptor.forClass(CredentialUserEntity.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertEquals(johnId, captor.getValue().getPersonId());
        verify(audit).record(eq(ADMIN), eq(SiteSecurityAuditAction.CREDENTIAL_CREATED), any(), eq("restricted"));
        verify(eventPublisher).publishEvent(any(CredentialNotificationEvent.class));
    }

    @Test
    void createCredential_forAPersonWithoutEmail_sendsNoEmail() {
        Long noEmail = people.add("Mariam Ouédraogo").id();
        when(zoneRepository.existsById("restricted")).thenReturn(true);
        when(userRepository.saveAndFlush(any(CredentialUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = siteSecurityService.createCredential(validRequest().toBuilder().personId(noEmail).build(), ADMIN);

        assertNull(result.getNotificationStatus());
        assertNull(result.getEmail());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createCredential_shouldRejectAnUnknownPerson() {
        CreateUserRequestDto req = validRequest().toBuilder().personId(999L).build();

        assertThrows(BadRequestException.class, () -> siteSecurityService.createCredential(req, ADMIN));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCredential_shouldRejectADeactivatedPerson() {
        people.update(johnId, new PersonDetails(null, null, null, false));

        assertThrows(BusinessRuleException.class, () -> siteSecurityService.createCredential(validRequest(), ADMIN));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCredential_shouldThrow_whenThePersonAlreadyHasACredential() {
        when(userRepository.existsByPersonId(johnId)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> siteSecurityService.createCredential(validRequest(), ADMIN));

        assertEquals("A credential for 'John Doe' already exists.", ex.getMessage());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCredential_shouldReportConflict_whenConcurrentDuplicateHitsTheConstraint() {
        when(zoneRepository.existsById("restricted")).thenReturn(true);
        when(userRepository.saveAndFlush(any(CredentialUserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uk_site_security_users_person_id"));

        assertThrows(ConflictException.class, () -> siteSecurityService.createCredential(validRequest(), ADMIN));
    }

    @Test
    void createCredential_shouldGenerateADifferentIdForEachCredential() {
        when(zoneRepository.existsById("restricted")).thenReturn(true);
        when(userRepository.saveAndFlush(any(CredentialUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        Long other = people.add("Jane Roe").id();

        String first = siteSecurityService.createCredential(validRequest(), ADMIN).getId();
        String second = siteSecurityService.createCredential(validRequest().toBuilder().personId(other).build(), ADMIN).getId();

        assertNotEquals(first, second);
    }

    @Test
    void createCredential_shouldRejectVisitorAboveControlledZone() {
        CreateUserRequestDto req = validRequest().toBuilder().type("Visitor").build();
        when(zoneRepository.existsById("restricted")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> siteSecurityService.createCredential(req, ADMIN));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCredential_shouldRejectPermanentVisitor_whateverTheCase() {
        CreateUserRequestDto req = validRequest().toBuilder()
                .type("Visitor").level("open").validUntil("permanent").build();
        when(zoneRepository.existsById("open")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> siteSecurityService.createCredential(req, ADMIN));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCredential_shouldAcceptDatedVisitorInControlledZone() {
        CreateUserRequestDto req = validRequest().toBuilder().type("Visitor").level("controlled").build();
        when(zoneRepository.existsById("controlled")).thenReturn(true);
        when(userRepository.saveAndFlush(any(CredentialUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("controlled", siteSecurityService.createCredential(req, ADMIN).getLevel());
    }

    @Test
    void createCredential_shouldThrow_whenLevelIsNotAKnownZone() {
        when(zoneRepository.existsById("restricted")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> siteSecurityService.createCredential(validRequest(), ADMIN));

        assertTrue(ex.getMessage().contains("Unknown access level 'restricted'"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCredential_shouldThrow_whenValidUntilIsInPast() {
        CreateUserRequestDto req = validRequest().toBuilder().validUntil("2020-01-01").build();
        when(zoneRepository.existsById("restricted")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> siteSecurityService.createCredential(req, ADMIN));

        assertTrue(ex.getMessage().contains("in the past"));
    }

    @Test
    void createCredential_shouldThrow_whenValidUntilIsMalformed() {
        CreateUserRequestDto req = validRequest().toBuilder().validUntil("tomorrow-ish").build();
        when(zoneRepository.existsById("restricted")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> siteSecurityService.createCredential(req, ADMIN));

        assertTrue(ex.getMessage().contains("Invalid validUntil"));
    }

    @Test
    void createCredential_shouldAcceptPermanentValidUntil() {
        CreateUserRequestDto req = validRequest().toBuilder().validUntil("Permanent").build();
        when(zoneRepository.existsById("restricted")).thenReturn(true);
        when(userRepository.saveAndFlush(any(CredentialUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("Permanent", siteSecurityService.createCredential(req, ADMIN).getValidUntil());
    }

    // --- revokeCredential ---

    @Test
    void revokeCredential_shouldRevokeOnce_andNotifyThePerson() {
        CredentialUserEntity credential = credential("u1", johnId, "restricted");
        when(userRepository.lockById("u1")).thenReturn(Optional.of(credential));

        UserDto revoked = siteSecurityService.revokeCredential("u1", ADMIN);

        assertEquals("Revoked", revoked.getStatus());
        assertEquals(CredentialNotificationStatus.PENDING, credential.getNotificationStatus());
        verify(audit).record(ADMIN, SiteSecurityAuditAction.CREDENTIAL_REVOKED, "u1", null);
        verify(eventPublisher).publishEvent(any(CredentialNotificationEvent.class));
    }

    // --- reads: names come from the shared directory, zones restrict users ---

    @Test
    void getCredentials_shouldReadNamesFromTheDirectory_andHideEmailsFromUsers() {
        when(userRepository.findAll()).thenReturn(List.of(
                credential("u1", johnId, "restricted"), credential("u2", johnId + 100, "critical")));
        SiteSecurityPrincipal user = new SiteSecurityPrincipal("operator-user", SiteSecurityRole.USER,
                Set.of(Permission.VIEW_ACCESS_REGISTER.name()), Set.of("restricted"));

        List<UserDto> asAdmin = siteSecurityService.getCredentials(ADMIN);
        List<UserDto> asUser = siteSecurityService.getCredentials(user);

        assertEquals(2, asAdmin.size());
        assertEquals("John Doe", asAdmin.get(0).getName());
        assertEquals("john.doe@example.test", asAdmin.get(0).getEmail());
        assertNull(asAdmin.get(1).getName(), "an unknown person has no name");
        assertEquals(1, asUser.size(), "a user only sees the credentials of its zones");
        assertNull(asUser.get(0).getEmail());
    }

    @Test
    void getAccessLogs_shouldReturnAll_whenFilterIsAll() {
        when(logRepository.findAll(any(Sort.class))).thenReturn(Arrays.asList(logEntry("l1", "Approved", "Staff", johnId)));

        List<LogEntryDto> result = siteSecurityService.getAccessLogs(LogFilter.ALL);

        assertEquals(1, result.size());
        assertEquals("l1", result.get(0).getId());
        assertEquals("John Doe", result.get(0).getUser());
        assertEquals("JD", result.get(0).getInitials());
    }

    @Test
    void getAccessLogs_shouldKeepAnUnidentifiedSubjectAnonymous() {
        when(logRepository.findByStatus(eq("Denied"), any(Sort.class)))
                .thenReturn(Arrays.asList(logEntry("l2", "Denied", "Unknown", null)));

        LogEntryDto entry = siteSecurityService.getAccessLogs(LogFilter.DENIED).get(0);

        assertEquals("Denied", entry.getStatus());
        assertNull(entry.getPersonId());
        assertNull(entry.getUser());
        assertNull(entry.getInitials());
    }

    @Test
    void getAccessLogs_shouldFilterByType() {
        when(logRepository.findByType(eq("Visitor"), any(Sort.class)))
                .thenReturn(Arrays.asList(logEntry("l3", "Approved", "Visitor", johnId)));

        List<LogEntryDto> result = siteSecurityService.getAccessLogs(LogFilter.VISITORS);

        assertEquals(1, result.size());
        assertEquals("Visitor", result.get(0).getType());
    }

    @Test
    void getZones_shouldMapEntitiesToDtos() {
        ZoneEntity zone = ZoneEntity.builder()
                .id("critical").name("Critical Zone").tagline("Infrastructure")
                .status("Locked Down")
                .description("desc").requirements(Arrays.asList("Level 4 clearance"))
                .build();
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(zone));

        List<ZoneDto> result = siteSecurityService.getZones();

        assertEquals(1, result.size());
        ZoneDto dto = result.get(0);
        assertEquals("critical", dto.getId());
        assertEquals("Critical Zone", dto.getName());
        assertEquals(0, dto.getAuthorizedUsers());
        assertEquals(1, dto.getRequirements().size());
    }

    @Test
    void getZones_shouldDefaultRequirementsToEmptyList() {
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(ZoneEntity.builder().id("open").name("Open Zone").build()));

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

    private static CredentialUserEntity credential(String id, Long personId, String level) {
        return CredentialUserEntity.builder()
                .id(id).personId(personId).role("Security Lead").type("Security")
                .level(level).validUntil("Permanent").status("Active").lastActive("Just now").build();
    }

    private static LogEntryEntity logEntry(String id, String status, String type, Long personId) {
        return LogEntryEntity.builder()
                .id(id).ref("LOG-" + id).time("10:00:00").date("2026-09-05")
                .personId(personId).type(type).zone("open")
                .zoneName("Open Zone").action("Entry").status(status)
                .gate("Gate 1").method("RFID Pass").note("note").build();
    }
}
