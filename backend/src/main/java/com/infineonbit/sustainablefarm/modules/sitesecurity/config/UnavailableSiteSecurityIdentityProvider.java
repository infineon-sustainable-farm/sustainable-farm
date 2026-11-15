package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production-safe placeholder until the central platform identity adapter is
 * connected. Anonymous requests fail closed instead of receiving a module role.
 */
@Component
@Profile("!dev")
public class UnavailableSiteSecurityIdentityProvider implements SiteSecurityIdentityProvider {

    @Override
    public Optional<SiteSecurityIdentity> current() {
        return Optional.empty();
    }
}
