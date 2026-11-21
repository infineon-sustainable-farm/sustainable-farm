package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Outside the development profile, until the platform authentication adapter is connected:
 * there is no identity, so every request fails closed (401) instead of receiving a module role.
 */
@Component
@Profile("!dev")
public class UnavailableSiteSecurityIdentityProvider implements SiteSecurityIdentityProvider {

    @Override
    public Optional<SiteSecurityIdentity> current() {
        return Optional.empty();
    }
}
