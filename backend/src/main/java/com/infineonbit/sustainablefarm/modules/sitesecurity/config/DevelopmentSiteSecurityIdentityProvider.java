package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Development profile only: every request acts as a Site Security administrator, so that the
 * screens can be used before the platform authentication exists. It is not a sign-in: it asks
 * for nothing, issues no session and writes no row.
 */
@Component
@Profile("dev")
public class DevelopmentSiteSecurityIdentityProvider implements SiteSecurityIdentityProvider {

    /** The actor recorded in the audit trail for changes made with this identity. */
    public static final String DEVELOPMENT_ACTOR = "development";

    @Override
    public Optional<SiteSecurityIdentity> current() {
        return Optional.of(new SiteSecurityIdentity(null, Set.of("PLATFORM_USER"), true));
    }
}
