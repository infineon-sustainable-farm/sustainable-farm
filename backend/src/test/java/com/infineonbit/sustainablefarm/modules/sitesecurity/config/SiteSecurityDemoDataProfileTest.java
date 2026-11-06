package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Demo data must load with the dev profile only, while zones load in every environment. */
class SiteSecurityDemoDataProfileTest {

    @Nested
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sitesecurity-dev;DB_CLOSE_DELAY=-1")
    @ActiveProfiles({"test", "dev"})
    class WithDevProfile {
        @Autowired private ZoneRepository zones;
        @Autowired private GateRepository gates;
        @Autowired private CredentialUserRepository credentials;
        @Autowired private LogEntryRepository logs;

        @Test
        void shouldLoadZonesAndDemoData() {
            assertEquals(4, zones.count());
            assertEquals(4, gates.count());
            assertEquals(4, credentials.count());
            assertEquals(6, logs.count());
        }
    }

    @Nested
    @SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sitesecurity-prod-like;DB_CLOSE_DELAY=-1")
    @ActiveProfiles("test")
    class WithoutDevProfile {
        @Autowired private ApplicationContext context;
        @Autowired private ZoneRepository zones;
        @Autowired private GateRepository gates;
        @Autowired private CredentialUserRepository credentials;
        @Autowired private LogEntryRepository logs;

        @Test
        void shouldLoadZonesOnly() {
            assertTrue(context.getBeansOfType(SiteSecurityDataSeeder.class).isEmpty());
            assertFalse(context.getBeansOfType(SiteSecurityReferenceDataSeeder.class).isEmpty());
            assertEquals(4, zones.count());
            assertEquals(0, gates.count());
            assertEquals(0, credentials.count());
            assertEquals(0, logs.count());
        }
    }
}
