package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.Permission;
import java.util.Set;

/**
 * Who is calling: a signed-in account, or the gate scanner (no account id or
 * email). Role, permissions and zones are read from the account row on every
 * request, so a change applies to the next request.
 */
public record SiteSecurityPrincipal(String operatorId, String email, SiteSecurityRole role,
        Set<String> permissions, Set<String> zoneIds, String fullName, boolean passwordSet) {

    public static final String GATE_SCANNER = "gate-scanner";

    public SiteSecurityPrincipal {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        zoneIds = zoneIds == null ? Set.of() : Set.copyOf(zoneIds);
        if (role == SiteSecurityRole.READ) {
            role = SiteSecurityRole.USER;
        }
    }

    /** An account with no particular permission set (admins, tests). READ means the legacy read-only access. */
    public SiteSecurityPrincipal(String operatorId, String email, SiteSecurityRole role) {
        this(operatorId, email, role,
                role == SiteSecurityRole.READ ? Set.of(Permission.LEGACY_READ_ALL.name()) : Set.of(),
                Set.of(), null, false);
    }

    public static SiteSecurityPrincipal gateScanner() {
        return new SiteSecurityPrincipal(null, null, SiteSecurityRole.GATE, Set.of(), Set.of(), null, false);
    }

    public boolean isAdmin() {
        return role == SiteSecurityRole.ADMIN;
    }

    /** Admins can use everything; users need the permission (the legacy read-only one covers every view). */
    public boolean can(Permission permission) {
        return isAdmin() || permissions.contains(permission.name())
                || (permission != Permission.LEGACY_READ_ALL && permissions.contains(Permission.LEGACY_READ_ALL.name()));
    }

    /** Admins and legacy read-only accounts see every zone; other users only their assigned zones. */
    public boolean seesAllZones() {
        return isAdmin() || permissions.contains(Permission.LEGACY_READ_ALL.name());
    }

    public boolean seesZone(String zoneId) {
        return seesAllZones() || (zoneId != null && zoneIds.contains(zoneId));
    }

    /** The name recorded in createdBy or disabledBy fields. */
    public String actor() {
        return email != null ? email : GATE_SCANNER;
    }

    @Override
    public String toString() {
        // Never log the email: the operator id is enough to find the person.
        return "SiteSecurityPrincipal[operatorId=" + operatorId + ", role=" + role + "]";
    }
}
