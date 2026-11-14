package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage 0: existing operators get account fields without losing access, sessions or zones. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-migration;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class SiteSecurityAccountMigrationTest {

    @Autowired private SiteSecurityAccountMigration migration;
    @Autowired private OperatorRepository accounts;
    @Autowired private OperatorSessionRepository sessions;
    @Autowired private SessionService sessionService;

    @BeforeEach
    void resetData() {
        sessions.deleteAll();
        accounts.deleteAll();
    }

    private OperatorEntity legacy(String email, SiteSecurityRole role, boolean active) {
        return accounts.save(OperatorEntity.builder().id(UUID.randomUUID().toString()).email(email).role(role)
                .active(active).createdBy("test").createdAt(Instant.now()).build());
    }

    @Test
    void legacyOperators_shouldKeepTheirAccess_andGetAccountFields() throws Exception {
        String adminSession = TestSessions.open(accounts, sessions, "old.admin@example.test", SiteSecurityRole.ADMIN);
        String readerSession = TestSessions.open(accounts, sessions, "old.reader@example.test", SiteSecurityRole.READ);
        OperatorEntity disabled = legacy("old.disabled@example.test", SiteSecurityRole.READ, false);

        migration.run();

        OperatorEntity admin = accounts.findByEmail("old.admin@example.test").orElseThrow();
        OperatorEntity reader = accounts.findByEmail("old.reader@example.test").orElseThrow();
        OperatorEntity off = accounts.findById(disabled.getId()).orElseThrow();
        assertEquals("ACTIVE", admin.getAccountStatus());
        assertEquals("ADMIN", admin.getAccountRole());
        assertEquals("ACTIVE", reader.getAccountStatus());
        assertEquals("USER", reader.getAccountRole());
        assertEquals(Set.of("LEGACY_READ_ALL"), reader.getPermissions());
        assertTrue(reader.getZoneIds().isEmpty(), "zones are never granted automatically");
        assertEquals("READ", reader.getRole().name(), "legacy column kept valid for old databases");
        assertEquals("DISABLED", off.getAccountStatus());
        assertEquals("ADMIN", off.getDisabledReason());
        assertFalse(admin.hasPassword());

        // No session was revoked.
        assertTrue(sessionService.resolve(adminSession).isPresent());
        assertTrue(sessionService.resolve(readerSession).isPresent());
    }

    @Test
    void secondRun_shouldChangeNothing() throws Exception {
        legacy("old.reader@example.test", SiteSecurityRole.READ, true);
        migration.run();
        OperatorEntity once = accounts.findByEmail("old.reader@example.test").orElseThrow();
        once.setPermissions(new java.util.HashSet<>(Set.of("VIEW_ZONES")));
        accounts.save(once);

        migration.run();

        assertEquals(Set.of("VIEW_ZONES"), accounts.findByEmail("old.reader@example.test").orElseThrow().getPermissions());
    }
}
