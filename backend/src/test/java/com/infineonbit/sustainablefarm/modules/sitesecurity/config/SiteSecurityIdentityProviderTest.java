package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SiteSecurityIdentityProviderTest {

    @Test
    void developmentProviderSuppliesAnIdentityWithoutPerson() {
        SiteSecurityIdentity identity = new DevelopmentSiteSecurityIdentityProvider()
                .current()
                .orElseThrow();

        assertNull(identity.personId());
        assertEquals(Set.of("PLATFORM_USER"), identity.globalRoles());
        assertTrue(identity.development());
    }

    @Test
    void unavailableProviderFailsClosed() {
        assertFalse(new UnavailableSiteSecurityIdentityProvider().current().isPresent());
    }

    @Test
    void aPlatformIdentityNeedsAPerson() {
        assertThrows(IllegalArgumentException.class, () -> new SiteSecurityIdentity(null, Set.of(), false));
        assertEquals(7L, new SiteSecurityIdentity(7L, null, false).personId());
    }
}
