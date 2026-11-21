package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

/** The changes kept in the audit trail. Stored as plain text: new values need no migration. */
public enum SiteSecurityAuditAction {
    OPERATOR_CREATED,
    OPERATOR_ROLE_CHANGED,
    OPERATOR_DISABLED,
    OPERATOR_ENABLED,
    CREDENTIAL_CREATED,
    CREDENTIAL_REVOKED;

    /** What kind of record the action changes. */
    public String targetType() {
        return name().startsWith("CREDENTIAL_") ? "CREDENTIAL" : "OPERATOR";
    }
}
