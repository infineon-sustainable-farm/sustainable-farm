package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteSecurityService {

    private final GateRepository gateRepository;
    private final ZoneRepository zoneRepository;
    private final CredentialUserRepository userRepository;
    private final LogEntryRepository logRepository;

    public OverviewResponseDto getOverview() {
        List<GateDto> gates = gateRepository.findAll().stream()
                .map(this::mapGateToDto)
                .collect(Collectors.toList());

        OverviewStatsDto stats = OverviewStatsDto.builder()
                .activePoints(4)
                .controlledZones(3)
                .visitors(2)
                .alerts(1)
                .build();

        return OverviewResponseDto.builder()
                .gates(gates)
                .stats(stats)
                .build();
    }

    public List<ZoneDto> getZones() {
        return zoneRepository.findAll().stream()
                .map(this::mapZoneToDto)
                .collect(Collectors.toList());
    }

    public List<UserDto> getCredentials() {
        return userRepository.findAll().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    private static final Set<String> ALLOWED_TYPES = Set.of("Staff", "Service", "Security", "Visitor");
    private static final Set<String> ALLOWED_STATUSES = Set.of("Active", "Authorized", "Suspended", "Expired");

    @Transactional
    public UserDto createCredential(CreateUserRequestDto req) {
        String name = req.getName().trim();
        if (userRepository.existsByName(name)) {
            throw new IllegalArgumentException("A credential for '" + name + "' already exists.");
        }
        if (!zoneRepository.existsById(req.getLevel())) {
            throw new IllegalArgumentException("Unknown access level '" + req.getLevel()
                    + "': no zone with this id is defined.");
        }
        if (!ALLOWED_TYPES.contains(req.getType())) {
            throw new IllegalArgumentException("Invalid credential type '" + req.getType()
                    + "'. Allowed values: " + String.join(", ", ALLOWED_TYPES) + ".");
        }
        if (req.getStatus() != null && !ALLOWED_STATUSES.contains(req.getStatus())) {
            throw new IllegalArgumentException("Invalid status '" + req.getStatus()
                    + "'. Allowed values: " + String.join(", ", ALLOWED_STATUSES) + ".");
        }
        validateValidUntil(req.getValidUntil());

        String id = "u-" + System.currentTimeMillis();
        CredentialUserEntity entity = CredentialUserEntity.builder()
                .id(id)
                .name(name)
                .role(req.getRole())
                .type(req.getType())
                .level(req.getLevel())
                .validUntil(req.getValidUntil())
                .status(req.getStatus() != null ? req.getStatus() : "Active")
                .initials(req.getInitials() != null ? req.getInitials() : initialsOf(name))
                .lastActive("Just now")
                .build();

        CredentialUserEntity saved = userRepository.save(entity);
        return mapUserToDto(saved);
    }

    public List<LogEntryDto> getAccessLogs(String filter) {
        List<LogEntryEntity> logs;
        if (filter == null || filter.equalsIgnoreCase("all")) {
            logs = logRepository.findAll();
        } else if (filter.equalsIgnoreCase("approved")) {
            logs = logRepository.findByStatus("Approved");
        } else if (filter.equalsIgnoreCase("denied")) {
            logs = logRepository.findByStatus("Denied");
        } else if (filter.equalsIgnoreCase("visitors")) {
            logs = logRepository.findByType("Visitor");
        } else if (filter.equalsIgnoreCase("staff")) {
            logs = logRepository.findByType("Staff");
        } else if (filter.equalsIgnoreCase("service")) {
            logs = logRepository.findByType("Service");
        } else {
            logs = logRepository.findAll();
        }

        return logs.stream()
                .map(this::mapLogToDto)
                .collect(Collectors.toList());
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

    private ZoneDto mapZoneToDto(ZoneEntity e) {
        return ZoneDto.builder()
                .id(e.getId())
                .name(e.getName())
                .tagline(e.getTagline())
                .authorizedUsers(e.getAuthorizedUsers())
                .entryPoints(e.getEntryPoints())
                .status(e.getStatus())
                .description(e.getDescription())
                .requirements(e.getRequirements() != null ? e.getRequirements() : new ArrayList<>())
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
                .status(e.getStatus())
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

    private void validateValidUntil(String validUntil) {
        if ("Permanent".equalsIgnoreCase(validUntil)) {
            return;
        }
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(validUntil);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid validUntil '" + validUntil + "': expected an ISO date (yyyy-MM-dd) or 'Permanent'.");
        }
        if (parsed.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("validUntil '" + validUntil + "' is in the past.");
        }
    }

    private String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
