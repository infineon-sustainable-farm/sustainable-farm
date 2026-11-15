package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.Permission;
import java.util.Set;

/** Central identity plus Farm Security's own role, permissions, and zones. */
public record SiteSecurityPrincipal(String operatorId, SiteSecurityRole role,
        Set<String> permissions, Set<String> zoneIds) {

    public static final String GATE_SCANNER = "gate-scanner";

    public SiteSecurityPrincipal {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        zoneIds = zoneIds == null ? Set.of() : Set.copyOf(zoneIds);
        if (role == SiteSecurityRole.READ) {
            role = SiteSecurityRole.USER;
        }
    }

    public static SiteSecurityPrincipal gateScanner() {
        return new SiteSecurityPrincipal(null, SiteSecurityRole.GATE, Set.of(), Set.of());
    }

    public boolean isAdmin() {
        return role == SiteSecurityRole.ADMIN;
    }

    public boolean can(Permission permission) {
        return isAdmin() || permissions.contains(permission.name())
                || (permission != Permission.LEGACY_READ_ALL
                && permissions.contains(Permission.LEGACY_READ_ALL.name()));
    }

    public boolean seesAllZones() {
        return isAdmin() || permissions.contains(Permission.LEGACY_READ_ALL.name());
    }

    public boolean seesZone(String zoneId) {
        return seesAllZones() || (zoneId != null && zoneIds.contains(zoneId));
    }

    public String actor() {
        return operatorId == null ? GATE_SCANNER : operatorId;
    }

    @Override
    public String toString() {
        return "SiteSecurityPrincipal[operatorId=" + operatorId + ", role=" + role + "]";
    }
}
