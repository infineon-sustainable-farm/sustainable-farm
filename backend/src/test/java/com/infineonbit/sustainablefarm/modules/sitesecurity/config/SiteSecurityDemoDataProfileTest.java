package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Outside the dev profile, the zones load (reference data) but no demo data, and there is no
 * identity: Site Security fails closed. The dev profile itself runs on PostgreSQL only (the
 * platform demo data set is PostgreSQL SQL); it is covered by BaselineMigrationTest and, for
 * the seeder logic, by SiteSecurityDevDataSeederTest.
 */
@SpringBootTest
@ActiveProfiles("test")
class SiteSecurityDemoDataProfileTest {

    @Autowired private ApplicationContext context;
    @Autowired private ZoneRepository zones;
    @Autowired private GateRepository gates;
    @Autowired private CredentialUserRepository credentials;
    @Autowired private LogEntryRepository logs;
    @Autowired private SiteSecurityIdentityProvider identityProvider;

    @Test
    void shouldLoadZonesOnly_andHaveNoIdentity() {
        assertTrue(context.getBeansOfType(SiteSecurityDevDataSeeder.class).isEmpty());
        assertTrue(context.getBeansOfType(DevelopmentSiteSecurityIdentityProvider.class).isEmpty());
        assertFalse(context.getBeansOfType(SiteSecurityReferenceDataSeeder.class).isEmpty());
        assertEquals(4, zones.count());
        assertEquals(0, gates.count());
        assertEquals(0, credentials.count());
        assertEquals(0, logs.count());
        assertFalse(identityProvider.current().isPresent());
    }
}
