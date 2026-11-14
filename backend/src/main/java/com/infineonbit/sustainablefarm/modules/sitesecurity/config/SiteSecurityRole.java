package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

/**
 * Access level of a caller. ADMIN can do everything. USER can only use what its
 * permissions allow (see {@link UserPermission}, {@link AnyAccount}). GATE is the
 * scanner at a gate: it has no account and can only call the endpoints marked
 * {@link GateScanner}. READ is the value of the legacy role column for accounts
 * that are not admins; callers holding it are treated as USER.
 */
public enum SiteSecurityRole {
    READ,
    ADMIN,
    GATE,
    USER
}
