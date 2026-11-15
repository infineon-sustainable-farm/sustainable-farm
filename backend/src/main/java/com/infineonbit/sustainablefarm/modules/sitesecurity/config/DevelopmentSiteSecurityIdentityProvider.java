package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Temporary deterministic development identity. It is deliberately profile
 * scoped and does not create an administrator row in the database.
 */
@Component
@Profile("dev")
public class DevelopmentSiteSecurityIdentityProvider implements SiteSecurityIdentityProvider {

    public static final String DEVELOPMENT_USER_ID = "dev-sitesecurity-admin";

    @Override
    public Optional<SiteSecurityIdentity> current() {
        return Optional.of(new SiteSecurityIdentity(DEVELOPMENT_USER_ID, Set.of("PLATFORM_USER"), true));
    }
}
