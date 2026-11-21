package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.*;
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
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDto;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Site Security data: gates, zones, the access register (credentials) and the access log.
 * People come from the shared directory ({@link PersonDirectory}, ADR 0004): a credential or a
 * log entry stores the id of a person, and the name and email are read from the directory.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteSecurityService {

    private static final Logger log = LoggerFactory.getLogger(SiteSecurityService.class);

    /** From the least to the most sensitive: a level also covers the zones below it. */
    private static final List<String> LEVEL_ORDER = List.of("open", "controlled", "restricted", "critical");
    private static final String OPEN_ZONE = "open";
    private static final Set<String> VISITOR_ALLOWED_LEVELS = Set.of("open", "controlled");
    private static final String VISITOR_TYPE = "Visitor";
    private static final String PERMANENT = "Permanent";
    private static final String INITIAL_STATUS = "Active";
    private static final String EXPIRED_STATUS = "Expired";
    private static final String REVOKED_STATUS = "Revoked";
    private static final Set<String> VALID_STATUSES = Set.of("Active", "Authorized");
    private static final Set<String> OPERATIONAL_GATE_STATUSES = Set.of("Active", "Secure");
    private static final String DENIED_STATUS = "Denied";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "date", "time");

    private final GateRepository gateRepository;
    private final ZoneRepository zoneRepository;
    private final CredentialUserRepository userRepository;
    private final LogEntryRepository logRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SiteSecurityAuditService audit;
    private final PersonDirectory personDirectory;

    /** The whole site, as an admin sees it. */
    private static final SiteSecurityPrincipal WHOLE_SITE =
            new SiteSecurityPrincipal(null, SiteSecurityRole.ADMIN, Set.of(), Set.of());

    public OverviewResponseDto getOverview() {
        return getOverview(WHOLE_SITE);
    }

    /** Admins see the whole site; a user only the gates, zones, credentials and denials of their zones. */
    public OverviewResponseDto getOverview(SiteSecurityPrincipal viewer) {
        List<GateEntity> gates = gateRepository.findAll().stream()
                .filter(gate -> viewer.seesZone(gate.getZone())).toList();
        List<ZoneEntity> zones = zoneRepository.findAll().stream()
                .filter(zone -> viewer.seesZone(zone.getId())).toList();
        long alerts = viewer.seesAllZones()
                ? logRepository.countByStatus(DENIED_STATUS)
                : logRepository.findByStatus(DENIED_STATUS, NEWEST_FIRST).stream()
                        .filter(entry -> viewer.seesZone(entry.getZone())).count();

        OverviewStatsDto stats = OverviewStatsDto.builder()
                .activePoints((int) gates.stream()
                        .filter(gate -> OPERATIONAL_GATE_STATUSES.contains(gate.getStatus()))
                        .count())
                .controlledZones((int) zones.stream().filter(zone -> !OPEN_ZONE.equals(zone.getId())).count())
                .totalZones(zones.size())
                .visitors((int) userRepository.findAll().stream()
                        .filter(user -> viewer.seesZone(user.getLevel()))
                        .filter(user -> VISITOR_TYPE.equals(user.getType()) && isValid(user))
                        .count())
                .alerts((int) alerts)
                .build();

        return OverviewResponseDto.builder()
                .gates(gates.stream().map(this::mapGateToDto).collect(Collectors.toList()))
                .stats(stats)
                .build();
    }

    public List<ZoneDto> getZones() {
        return getZones(WHOLE_SITE);
    }

    public List<ZoneDto> getZones(SiteSecurityPrincipal viewer) {
        List<CredentialUserEntity> users = userRepository.findAll();
        List<GateEntity> gates = gateRepository.findAll();
        return zoneRepository.findAll().stream()
                .filter(zone -> viewer.seesZone(zone.getId()))
                .map(zone -> mapZoneToDto(zone, users, gates))
                .collect(Collectors.toList());
    }

    public List<UserDto> getCredentials(SiteSecurityRole role) {
        return getCredentials(new SiteSecurityPrincipal(null, role, Set.of(), Set.of()));
    }

    /** Emails only for admins; a user only sees credentials whose level is one of their zones. */
    public List<UserDto> getCredentials(SiteSecurityPrincipal viewer) {
        List<CredentialUserEntity> visible = userRepository.findAll().stream()
                .filter(user -> viewer.seesZone(user.getLevel()))
                .toList();
        Map<Long, PersonDto> people = peopleOf(visible.stream().map(CredentialUserEntity::getPersonId).toList());
        List<UserDto> credentials = visible.stream()
                .map(user -> mapUserToDto(user, personOf(people, user.getPersonId())))
                .collect(Collectors.toList());
        if (!viewer.isAdmin()) {
            credentials.forEach(credential -> credential.setEmail(null));
        }
        return credentials;
    }

    @Transactional
    public UserDto createCredential(CreateUserRequestDto req, SiteSecurityPrincipal actor) {
        PersonDto person = requireActivePerson(req.getPersonId());
        if (userRepository.existsByPersonId(person.id())) {
            throw duplicateCredential(person);
        }
        if (!zoneRepository.existsById(req.getLevel())) {
            throw new BusinessRuleException("Unknown access level '" + req.getLevel()
                    + "': no zone with this id is defined.");
        }
        validateValidUntil(req.getValidUntil());
        validateVisitorRules(req);

        CredentialUserEntity entity = CredentialUserEntity.builder()
                .id(UUID.randomUUID().toString())
                .personId(person.id())
                .notificationStatus(hasEmail(person) ? CredentialNotificationStatus.PENDING : null)
                .role(req.getRole().trim())
                .type(req.getType())
                .level(req.getLevel())
                .validUntil(PERMANENT.equalsIgnoreCase(req.getValidUntil()) ? PERMANENT : req.getValidUntil())
                .status(INITIAL_STATUS)
                .lastActive("Not yet on site")
                .build();

        try {
            // Flush here so a concurrent duplicate fails inside this method, as a 409.
            CredentialUserEntity saved = userRepository.saveAndFlush(entity);
            audit.record(actor, SiteSecurityAuditAction.CREDENTIAL_CREATED, saved.getId(), saved.getLevel());
            if (saved.getNotificationStatus() != null) {
                eventPublisher.publishEvent(new CredentialNotificationEvent(
                        saved.getId(), CredentialNotificationEvent.Type.GRANTED));
            }
            return mapUserToDto(saved, person);
        } catch (DataIntegrityViolationException e) {
            throw duplicateCredential(person);
        }
    }

    /** Idempotent: revoking a credential that is already revoked changes nothing and records nothing. */
    @Transactional
    public UserDto revokeCredential(String id, SiteSecurityPrincipal actor) {
        CredentialUserEntity credential = userRepository.lockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No credential with id '" + id + "'."));
        PersonDto person = personDirectory.findById(credential.getPersonId()).orElse(null);
        if (!REVOKED_STATUS.equals(credential.getStatus())) {
            credential.setStatus(REVOKED_STATUS);
            if (hasEmail(person)) {
                credential.setNotificationStatus(CredentialNotificationStatus.PENDING);
                eventPublisher.publishEvent(new CredentialNotificationEvent(
                        credential.getId(), CredentialNotificationEvent.Type.REVOKED));
            }
            userRepository.save(credential);
            audit.record(actor, SiteSecurityAuditAction.CREDENTIAL_REVOKED, credential.getId(), null);
            log.info("Site Security credential revoked: {}", credential.getId());
        }
        return mapUserToDto(credential, person);
    }

    public List<LogEntryDto> getAccessLogs(LogFilter filter, SiteSecurityPrincipal viewer) {
        return getAccessLogs(filter).stream()
                .filter(entry -> viewer.seesZone(entry.getZone()))
                .collect(Collectors.toList());
    }

    public List<LogEntryDto> getAccessLogs(LogFilter filter) {
        List<LogEntryEntity> logs = switch (filter) {
            case ALL -> logRepository.findAll(NEWEST_FIRST);
            case APPROVED -> logRepository.findByStatus("Approved", NEWEST_FIRST);
            case DENIED -> logRepository.findByStatus(DENIED_STATUS, NEWEST_FIRST);
            case VISITORS -> logRepository.findByType(VISITOR_TYPE, NEWEST_FIRST);
            case STAFF -> logRepository.findByType("Staff", NEWEST_FIRST);
            case SERVICE -> logRepository.findByType("Service", NEWEST_FIRST);
        };

        Map<Long, PersonDto> people = peopleOf(logs.stream().map(LogEntryEntity::getPersonId).toList());
        return logs.stream()
                .map(entry -> mapLogToDto(entry, personOf(people, entry.getPersonId())))
                .collect(Collectors.toList());
    }

    private ConflictException duplicateCredential(PersonDto person) {
        return new ConflictException("A credential for '" + person.fullName() + "' already exists.");
    }

    /** The holder of a new credential: an active person of the shared directory, never created here. */
    private PersonDto requireActivePerson(Long personId) {
        PersonDto person = personDirectory.findById(personId).orElseThrow(() ->
                new BadRequestException("Person " + personId + " not found in the people directory"));
        if (!person.active()) {
            throw new BusinessRuleException("Person " + personId + " is deactivated in the people directory.");
        }
        return person;
    }

    private Map<Long, PersonDto> peopleOf(Collection<Long> personIds) {
        List<Long> ids = personIds.stream().filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Map.of() : personDirectory.findAllById(ids);
    }

    private static PersonDto personOf(Map<Long, PersonDto> people, Long personId) {
        return personId == null ? null : people.get(personId);
    }

    private static boolean hasEmail(PersonDto person) {
        return person != null && person.email() != null && !person.email().isBlank();
    }

    private void validateVisitorRules(CreateUserRequestDto req) {
        if (!VISITOR_TYPE.equals(req.getType())) {
            return;
        }
        if (!VISITOR_ALLOWED_LEVELS.contains(req.getLevel())) {
            throw new BusinessRuleException("A visitor credential is limited to the open and controlled zones.");
        }
        if (PERMANENT.equalsIgnoreCase(req.getValidUntil())) {
            throw new BusinessRuleException("A visitor credential must have an end date.");
        }
    }

    private void validateValidUntil(String validUntil) {
        if (PERMANENT.equalsIgnoreCase(validUntil)) {
            return;
        }
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(validUntil);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException(
                    "Invalid validUntil '" + validUntil + "': expected an ISO date (yyyy-MM-dd) or 'Permanent'.");
        }
        if (parsed.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("validUntil '" + validUntil + "' is in the past.");
        }
    }

    /** The status to act on: a revocation or an end date overrides the stored status. */
    private String effectiveStatus(CredentialUserEntity credential) {
        if (REVOKED_STATUS.equals(credential.getStatus())) {
            return REVOKED_STATUS;
        }
        return hasExpired(credential.getValidUntil()) ? EXPIRED_STATUS : credential.getStatus();
    }

    /** A validity that cannot be read is treated as expired: it cannot be proved valid. */
    private boolean hasExpired(String validUntil) {
        if (PERMANENT.equalsIgnoreCase(validUntil)) {
            return false;
        }
        if (validUntil == null) {
            return true;
        }
        try {
            return LocalDate.parse(validUntil).isBefore(LocalDate.now());
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    private boolean isValid(CredentialUserEntity credential) {
        return VALID_STATUSES.contains(effectiveStatus(credential));
    }

    private int authorizedUsers(String zoneId, List<CredentialUserEntity> users) {
        int zoneRank = LEVEL_ORDER.indexOf(zoneId);
        return (int) users.stream()
                .filter(this::isValid)
                .filter(user -> zoneRank >= 0 && LEVEL_ORDER.indexOf(user.getLevel()) >= zoneRank)
                .count();
    }

    private GateDto mapGateToDto(GateEntity e) {
        return GateDto.builder()
                .id(e.getId())
                .name(e.getName())
                .shortName(e.getShortName())
                .zone(e.getZone())
                .status(e.getStatus())
                .detail(e.getDetail())
                .build();
    }

    private ZoneDto mapZoneToDto(ZoneEntity e, List<CredentialUserEntity> users, List<GateEntity> gates) {
        return ZoneDto.builder()
                .id(e.getId())
                .name(e.getName())
                .tagline(e.getTagline())
                .authorizedUsers(authorizedUsers(e.getId(), users))
                .entryPoints((int) gates.stream().filter(gate -> e.getId().equals(gate.getZone())).count())
                .status(e.getStatus())
                .description(e.getDescription())
                // Copied here: this lazy collection cannot be read once the transaction ends.
                .requirements(e.getRequirements() != null ? new ArrayList<>(e.getRequirements()) : new ArrayList<>())
                .build();
    }

    private UserDto mapUserToDto(CredentialUserEntity e, PersonDto person) {
        String name = person == null ? null : person.fullName();
        return UserDto.builder()
                .id(e.getId())
                .personId(e.getPersonId())
                .name(name)
                .role(e.getRole())
                .type(e.getType())
                .level(e.getLevel())
                .validUntil(e.getValidUntil())
                .status(effectiveStatus(e))
                .initials(initialsOf(name))
                .lastActive(e.getLastActive())
                .email(person == null ? null : person.email())
                .notificationStatus(e.getNotificationStatus() == null
                        ? null : e.getNotificationStatus().name().toLowerCase(Locale.ROOT))
                .build();
    }

    private LogEntryDto mapLogToDto(LogEntryEntity e, PersonDto person) {
        String name = person == null ? null : person.fullName();
        return LogEntryDto.builder()
                .id(e.getId())
                .ref(e.getRef())
                .time(e.getTime())
                .date(e.getDate())
                .personId(e.getPersonId())
                .user(name)
                .initials(initialsOf(name))
                .type(e.getType())
                .zone(e.getZone())
                .zoneName(e.getZoneName())
                .action(e.getAction())
                .status(e.getStatus())
                .gate(e.getGate())
                .method(e.getMethod())
                .note(e.getNote())
                .build();
    }

    /** Empty for an unknown person: the screen shows its own placeholder. */
    private static String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
