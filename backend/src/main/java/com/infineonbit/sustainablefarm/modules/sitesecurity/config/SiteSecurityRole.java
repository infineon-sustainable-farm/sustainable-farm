package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

/**
 * Access level of a caller. ADMIN includes everything READ can do. GATE is the
 * scanner at a gate: it has no operator account and can only call the
 * endpoints marked {@link GateScanner}.
 */
public enum SiteSecurityRole {
    READ,
    ADMIN,
    GATE
}
