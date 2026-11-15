package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SiteSecurityIdentityProviderTest {

    @Test
    void developmentProviderSuppliesOnlyDeterministicCentralIdentity() {
        SiteSecurityIdentity identity = new DevelopmentSiteSecurityIdentityProvider()
                .current()
                .orElseThrow();

        assertEquals(DevelopmentSiteSecurityIdentityProvider.DEVELOPMENT_USER_ID, identity.centralUserId());
        assertEquals(Set.of("PLATFORM_USER"), identity.globalRoles());
        assertTrue(identity.development());
    }

    @Test
    void unavailableProviderFailsClosed() {
        assertFalse(new UnavailableSiteSecurityIdentityProvider().current().isPresent());
    }
}
