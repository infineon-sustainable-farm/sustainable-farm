package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.Optional;

/**
 * Adapter boundary for the central identity provider. A future platform
 * integration can replace the active implementation without changing module
 * authorization services.
 */
public interface SiteSecurityIdentityProvider {
    Optional<SiteSecurityIdentity> current();
}
