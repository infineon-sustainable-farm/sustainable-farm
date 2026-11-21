package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.Optional;

/**
 * Boundary toward the platform authentication, which the maintainers build separately. Its
 * adapter will replace the active implementation without changing the module authorization.
 * Until then, only the development profile has an identity; elsewhere every request is refused.
 */
public interface SiteSecurityIdentityProvider {
    Optional<SiteSecurityIdentity> current();
}
