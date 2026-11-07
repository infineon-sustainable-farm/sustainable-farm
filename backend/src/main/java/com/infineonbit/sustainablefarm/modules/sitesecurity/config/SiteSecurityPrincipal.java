package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

/**
 * Who is calling: a signed-in operator, or the gate scanner (no operator id or
 * email). The role is read from the operator's row on every request, so a role
 * change applies to the next request.
 */
public record SiteSecurityPrincipal(String operatorId, String email, SiteSecurityRole role) {

    public static final String GATE_SCANNER = "gate-scanner";

    public static SiteSecurityPrincipal gateScanner() {
        return new SiteSecurityPrincipal(null, null, SiteSecurityRole.GATE);
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
