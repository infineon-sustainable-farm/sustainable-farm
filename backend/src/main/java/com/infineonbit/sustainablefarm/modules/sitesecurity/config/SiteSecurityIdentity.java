package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.Set;

/**
 * The caller as the platform authentication supplies it. Site Security never authenticates
 * anyone: it only resolves its own role, permissions and zones for this identity.
 *
 * <p>The platform authentication is not built yet. When it is, its adapter
 * ({@link SiteSecurityIdentityProvider}) gives the person of the shared people directory the
 * signed-in user corresponds to; Site Security does not assume how that link is made.</p>
 *
 * @param personId    the caller's person in the shared directory; empty only for the
 *                    development identity
 * @param globalRoles platform roles, never used as Site Security roles
 * @param development the profile-scoped development identity, which acts as an administrator
 */
public record SiteSecurityIdentity(Long personId, Set<String> globalRoles, boolean development) {

    public SiteSecurityIdentity {
        if (personId == null && !development) {
            throw new IllegalArgumentException("A person id is required.");
        }
        globalRoles = globalRoles == null ? Set.of() : Set.copyOf(globalRoles);
    }
}
