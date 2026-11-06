package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.*;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.GateEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.ZoneEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public OverviewResponseDto getOverview() {
        List<GateEntity> gates = gateRepository.findAll();
        List<ZoneEntity> zones = zoneRepository.findAll();

        OverviewStatsDto stats = OverviewStatsDto.builder()
                .activePoints((int) gates.stream()
                        .filter(gate -> OPERATIONAL_GATE_STATUSES.contains(gate.getStatus()))
                        .count())
                .controlledZones((int) zones.stream().filter(zone -> !OPEN_ZONE.equals(zone.getId())).count())
                .totalZones(zones.size())
                .visitors((int) userRepository.findAll().stream()
                        .filter(user -> VISITOR_TYPE.equals(user.getType()) && isValid(user))
                        .count())
                .alerts((int) logRepository.countByStatus(DENIED_STATUS))
                .build();

        return OverviewResponseDto.builder()
                .gates(gates.stream().map(this::mapGateToDto).collect(Collectors.toList()))
                .stats(stats)
                .build();
    }

    public List<ZoneDto> getZones() {
        List<CredentialUserEntity> users = userRepository.findAll();
        List<GateEntity> gates = gateRepository.findAll();
        return zoneRepository.findAll().stream()
                .map(zone -> mapZoneToDto(zone, users, gates))
                .collect(Collectors.toList());
    }

    public List<UserDto> getCredentials() {
        return userRepository.findAll().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto createCredential(CreateUserRequestDto req) {
        String name = req.getName().trim();
        if (userRepository.existsByName(name)) {
            throw duplicateCredential(name);
        }
        if (!zoneRepository.existsById(req.getLevel())) {
            throw new BusinessRuleException("Unknown access level '" + req.getLevel()
                    + "': no zone with this id is defined.");
        }
        validateValidUntil(req.getValidUntil());
        validateVisitorRules(req);

        CredentialUserEntity entity = CredentialUserEntity.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .role(req.getRole().trim())
                .type(req.getType())
                .level(req.getLevel())
                .validUntil(PERMANENT.equalsIgnoreCase(req.getValidUntil()) ? PERMANENT : req.getValidUntil())
                .status(INITIAL_STATUS)
                .initials(initialsOf(name))
                .lastActive("Not yet on site")
                .build();

        try {
            // Flush here so a concurrent duplicate fails inside this method, as a 409.
            return mapUserToDto(userRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw duplicateCredential(name);
        }
    }

    /** Idempotent: revoking a credential that is already revoked changes nothing. */
    @Transactional
    public UserDto revokeCredential(String id) {
        CredentialUserEntity credential = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No credential with id '" + id + "'."));
        if (!REVOKED_STATUS.equals(credential.getStatus())) {
            credential.setStatus(REVOKED_STATUS);
            userRepository.save(credential);
            log.info("Site Security credential revoked: {}", credential.getId());
        }
        return mapUserToDto(credential);
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

        return logs.stream()
                .map(this::mapLogToDto)
                .collect(Collectors.toList());
    }

    private ConflictException duplicateCredential(String name) {
        return new ConflictException("A credential for '" + name + "' already exists.");
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

    private UserDto mapUserToDto(CredentialUserEntity e) {
        return UserDto.builder()
                .id(e.getId())
                .name(e.getName())
                .role(e.getRole())
                .type(e.getType())
                .level(e.getLevel())
                .validUntil(e.getValidUntil())
                .status(effectiveStatus(e))
                .initials(e.getInitials())
                .lastActive(e.getLastActive())
                .build();
    }

    private LogEntryDto mapLogToDto(LogEntryEntity e) {
        return LogEntryDto.builder()
                .id(e.getId())
                .ref(e.getRef())
                .time(e.getTime())
                .date(e.getDate())
                .user(e.getUser())
                .initials(e.getInitials())
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

    private String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
