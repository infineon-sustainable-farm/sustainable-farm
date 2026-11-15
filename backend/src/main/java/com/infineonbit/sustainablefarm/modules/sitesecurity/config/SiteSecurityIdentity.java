package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.Set;

/**
 * Identity supplied by the central platform. Site Security never authenticates
 * this identity; it only resolves its own role and zone permissions.
 */
public record SiteSecurityIdentity(String centralUserId, Set<String> globalRoles, boolean development) {

    public SiteSecurityIdentity {
        if (centralUserId == null || centralUserId.isBlank()) {
            throw new IllegalArgumentException("A central user id is required.");
        }
        globalRoles = globalRoles == null ? Set.of() : Set.copyOf(globalRoles);
    }
}
