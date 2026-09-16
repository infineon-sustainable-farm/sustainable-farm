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
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteSecurityService {

    private final GateRepository gateRepository;
    private final ZoneRepository zoneRepository;
    private final CredentialUserRepository userRepository;
    private final LogEntryRepository logRepository;

    @PostConstruct
    public void initDefaultData() {
        if (gateRepository.count() == 0) {
            gateRepository.saveAll(Arrays.asList(
                GateEntity.builder().id("g1").name("Gate 1 · North Drive").shortName("Gate 1").zone("open").status("Active").detail("Main visitor & vehicle intake").build(),
                GateEntity.builder().id("g2").name("Gate 2 · Barn Yard").shortName("Gate 2").zone("controlled").status("Active").detail("Equipment & staff access").build(),
                GateEntity.builder().id("g3").name("Gate 3 · Processing Hub").shortName("Gate 3").zone("restricted").status("Secure").detail("Keycard & PIN required").build(),
                GateEntity.builder().id("g4").name("Gate 4 · Control Vault").shortName("Gate 4").zone("critical").status("Locked").detail("Biometric + escort required").build()
            ));
        }

        if (zoneRepository.count() == 0) {
            zoneRepository.saveAll(Arrays.asList(
                ZoneEntity.builder().id("open").name("Open Zone").tagline("Visitor-facing areas").authorizedUsers(24).entryPoints(2).status("Open Access").description("Public access areas including farm shop, main drive, and visitor center.").requirements(Arrays.asList("No clearance needed", "Visitor check-in recommended")).build(),
                ZoneEntity.builder().id("controlled").name("Controlled Zone").tagline("Operational farm grounds").authorizedUsers(14).entryPoints(3).status("Monitored").description("Active farming zones including barns, storage sheds, and equipment yards.").requirements(Arrays.asList("Staff or escorted visitor pass", "Safety briefing completed")).build(),
                ZoneEntity.builder().id("restricted").name("Restricted Zone").tagline("Processing & chemical storage").authorizedUsers(6).entryPoints(1).status("Protected").description("High-value equipment, product transformation, and hazardous chemical stores.").requirements(Arrays.asList("Level 3 badge clearance", "Dual-custody access protocol")).build(),
                ZoneEntity.builder().id("critical").name("Critical Zone").tagline("Server, power & water control").authorizedUsers(2).entryPoints(1).status("Locked Down").description("Infrastructure backbone: primary power distribution, main water valves, and security control room.").requirements(Arrays.asList("Level 4 clearance + biometric", "Security escort required", "Continuous audit logging")).build()
            ));
        }

        if (userRepository.count() == 0) {
            userRepository.saveAll(Arrays.asList(
                CredentialUserEntity.builder().id("u1").name("Dr. Elena Rostova").role("Chief Agonomist").type("Staff").level("restricted").validUntil("Permanent").status("Active").initials("ER").lastActive("10 min ago").build(),
                CredentialUserEntity.builder().id("u2").name("Marcus Vance").role("Facilities Tech").type("Service").level("controlled").validUntil("2026-12-31").status("Active").initials("MV").lastActive("1 hour ago").build(),
                CredentialUserEntity.builder().id("u3").name("Amina Diallo").role("Security Lead").type("Security").level("critical").validUntil("Permanent").status("Active").initials("AD").lastActive("Just now").build(),
                CredentialUserEntity.builder().id("u4").name("Lucas Meyer").role("Harvest Contractor").type("Visitor").level("open").validUntil("Today, 18:00").status("Authorized").initials("LM").lastActive("3 hours ago").build()
            ));
        }

        if (logRepository.count() == 0) {
            logRepository.saveAll(Arrays.asList(
                LogEntryEntity.builder().id("l1").ref("LOG-8821").time("10:28:14").date("2026-09-05").user("Amina Diallo").initials("AD").type("Security").zone("critical").zoneName("Critical Zone").action("Entry").status("Approved").gate("Gate 4").method("Biometric Scan").note("Routine perimeter sweep").build(),
                LogEntryEntity.builder().id("l2").ref("LOG-8820").time("10:27:02").date("2026-09-05").user("Unknown Subject").initials("??").type("Unknown").zone("restricted").zoneName("Restricted Zone").action("Entry").status("Denied").gate("Gate 3").method("Keycard Reject").note("Invalid badge ID — escalated").build(),
                LogEntryEntity.builder().id("l3").ref("LOG-8819").time("10:14:50").date("2026-09-05").user("Marcus Vance").initials("MV").type("Service").zone("controlled").zoneName("Controlled Zone").action("Entry").status("Approved").gate("Gate 2").method("RFID Pass").note("HVAC maintenance").build(),
                LogEntryEntity.builder().id("l4").ref("LOG-8818").time("09:45:11").date("2026-09-05").user("Lucas Meyer").initials("LM").type("Visitor").zone("open").zoneName("Open Zone").action("Entry").status("Approved").gate("Gate 1").method("QR Guest Code").note("Temp visitor pass").build(),
                LogEntryEntity.builder().id("l5").ref("LOG-8817").time("09:30:00").date("2026-09-05").user("Dr. Elena Rostova").initials("ER").type("Staff").zone("restricted").zoneName("Restricted Zone").action("Entry").status("Approved").gate("Gate 3").method("PIN + Keycard").note("Crop analysis").build(),
                LogEntryEntity.builder().id("l6").ref("LOG-8816").time("08:15:22").date("2026-09-05").user("Amina Diallo").initials("AD").type("Security").zone("open").zoneName("Open Zone").action("Exit").status("Approved").gate("Gate 1").method("Manual Gate Open").note("Perimeter check").build()
            ));
        }
    }

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

    public UserDto createCredential(CreateUserRequestDto req) {
        String id = "u-" + System.currentTimeMillis();
        CredentialUserEntity entity = CredentialUserEntity.builder()
                .id(id)
                .name(req.getName())
                .role(req.getRole())
                .type(req.getType())
                .level(req.getLevel())
                .validUntil(req.getValidUntil())
                .status(req.getStatus() != null ? req.getStatus() : "Active")
                .initials(req.getInitials() != null ? req.getInitials() : initialsOf(req.getName()))
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

    private String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
