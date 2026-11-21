package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.GateEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Site Security demo data (gates, credentials, access log), {@code dev} profile only. The zones
 * are reference data ({@link SiteSecurityReferenceDataSeeder}).
 *
 * <p>Credentials and log entries reference people of the shared directory: the first active
 * people, as loaded by the platform demo data set ({@code core.config.DemoDataSeeder}). This
 * seeder never creates a person, so it runs once the application is ready, after every startup
 * runner. Each table is seeded only when it is empty, so a restart never duplicates rows.</p>
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SiteSecurityDevDataSeeder {

    /** Function, type and level of the demo credentials, given to the first active people. */
    private static final List<String[]> DEMO_CREDENTIALS = List.of(
            new String[] {"Chief Agronomist", "Staff", "restricted", "Permanent", "Active", "10 min ago"},
            new String[] {"Facilities Tech", "Service", "controlled", "2027-12-31", "Active", "1 hour ago"},
            new String[] {"Security Lead", "Security", "critical", "Permanent", "Active", "Just now"},
            new String[] {"Harvest Contractor", "Visitor", "open", "2027-06-30", "Authorized", "3 hours ago"});

    private final GateRepository gateRepository;
    private final CredentialUserRepository userRepository;
    private final LogEntryRepository logRepository;
    private final PersonDirectory personDirectory;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        seed();
    }

    void seed() {
        if (gateRepository.count() == 0) {
            gateRepository.saveAll(Arrays.asList(
                GateEntity.builder().id("g1").name("Gate 1 · North Drive").shortName("Gate 1").zone("open").status("Active").detail("Main visitor & vehicle intake").build(),
                GateEntity.builder().id("g2").name("Gate 2 · Barn Yard").shortName("Gate 2").zone("controlled").status("Active").detail("Equipment & staff access").build(),
                GateEntity.builder().id("g3").name("Gate 3 · Processing Hub").shortName("Gate 3").zone("restricted").status("Secure").detail("Keycard & PIN required").build(),
                GateEntity.builder().id("g4").name("Gate 4 · Control Vault").shortName("Gate 4").zone("critical").status("Locked").detail("Biometric + escort required").build()
            ));
        }

        List<Long> people = personDirectory.listActive().stream()
                .map(PersonDto::id)
                .limit(DEMO_CREDENTIALS.size())
                .toList();

        if (userRepository.count() == 0 && !people.isEmpty()) {
            List<CredentialUserEntity> credentials = new ArrayList<>();
            for (int i = 0; i < people.size(); i++) {
                String[] demo = DEMO_CREDENTIALS.get(i);
                credentials.add(CredentialUserEntity.builder().id("u" + (i + 1)).personId(people.get(i))
                        .role(demo[0]).type(demo[1]).level(demo[2]).validUntil(demo[3]).status(demo[4])
                        .lastActive(demo[5]).build());
            }
            userRepository.saveAll(credentials);
        }

        if (logRepository.count() == 0) {
            logRepository.saveAll(Arrays.asList(
                log("l1", "LOG-8821", "10:28:14", person(people, 2), "Security", "critical", "Critical Zone", "Entry", "Approved", "Gate 4", "Biometric Scan", "Routine perimeter sweep"),
                log("l2", "LOG-8820", "10:27:02", null, "Unknown", "restricted", "Restricted Zone", "Entry", "Denied", "Gate 3", "Keycard Reject", "Invalid badge ID — escalated"),
                log("l3", "LOG-8819", "10:14:50", person(people, 1), "Service", "controlled", "Controlled Zone", "Entry", "Approved", "Gate 2", "RFID Pass", "HVAC maintenance"),
                log("l4", "LOG-8818", "09:45:11", person(people, 3), "Visitor", "open", "Open Zone", "Entry", "Approved", "Gate 1", "QR Guest Code", "Temp visitor pass"),
                log("l5", "LOG-8817", "09:30:00", person(people, 0), "Staff", "restricted", "Restricted Zone", "Entry", "Approved", "Gate 3", "PIN + Keycard", "Crop analysis"),
                log("l6", "LOG-8816", "08:15:22", person(people, 2), "Security", "open", "Open Zone", "Exit", "Approved", "Gate 1", "Manual Gate Open", "Perimeter check")
            ));
        }
    }

    private static Long person(List<Long> people, int index) {
        return index < people.size() ? people.get(index) : null;
    }

    private static LogEntryEntity log(String id, String ref, String time, Long personId, String type, String zone,
            String zoneName, String action, String status, String gate, String method, String note) {
        return LogEntryEntity.builder().id(id).ref(ref).time(time).date("2026-09-05").personId(personId)
                .type(type).zone(zone).zoneName(zoneName).action(action).status(status).gate(gate)
                .method(method).note(note).build();
    }
}
