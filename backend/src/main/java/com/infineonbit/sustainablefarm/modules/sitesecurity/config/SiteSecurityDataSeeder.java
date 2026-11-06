package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.GateEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import java.time.LocalDate;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class SiteSecurityDataSeeder implements CommandLineRunner {

    private final GateRepository gateRepository;
    private final CredentialUserRepository userRepository;
    private final LogEntryRepository logRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (gateRepository.count() == 0) {
            gateRepository.saveAll(Arrays.asList(
                GateEntity.builder().id("g1").name("Gate 1 · North Drive").shortName("Gate 1").zone("open").status("Active").detail("Main visitor & vehicle intake").build(),
                GateEntity.builder().id("g2").name("Gate 2 · Barn Yard").shortName("Gate 2").zone("controlled").status("Active").detail("Equipment & staff access").build(),
                GateEntity.builder().id("g3").name("Gate 3 · Processing Hub").shortName("Gate 3").zone("restricted").status("Secure").detail("Keycard & PIN required").build(),
                GateEntity.builder().id("g4").name("Gate 4 · Control Vault").shortName("Gate 4").zone("critical").status("Locked").detail("Biometric + escort required").build()
            ));
        }

        if (userRepository.count() == 0) {
            userRepository.saveAll(Arrays.asList(
                CredentialUserEntity.builder().id("u1").name("Dr. Elena Rostova").role("Chief Agonomist").type("Staff").level("restricted").validUntil("Permanent").status("Active").initials("ER").lastActive("10 min ago").build(),
                CredentialUserEntity.builder().id("u2").name("Marcus Vance").role("Facilities Tech").type("Service").level("controlled").validUntil("2026-12-31").status("Active").initials("MV").lastActive("1 hour ago").build(),
                CredentialUserEntity.builder().id("u3").name("Amina Diallo").role("Security Lead").type("Security").level("critical").validUntil("Permanent").status("Active").initials("AD").lastActive("Just now").build(),
                CredentialUserEntity.builder().id("u4").name("Lucas Meyer").role("Harvest Contractor").type("Visitor").level("open").validUntil(LocalDate.now().toString()).status("Authorized").initials("LM").lastActive("3 hours ago").build()
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
}
