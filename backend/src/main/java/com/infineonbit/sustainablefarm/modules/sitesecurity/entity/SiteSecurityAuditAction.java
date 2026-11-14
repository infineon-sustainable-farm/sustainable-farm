package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

/** The changes kept in the audit trail. Stored as plain text: new values need no migration. */
public enum SiteSecurityAuditAction {
    OPERATOR_CREATED,
    OPERATOR_ROLE_CHANGED,
    OPERATOR_DISABLED,
    OPERATOR_ENABLED,
    CREDENTIAL_CREATED,
    CREDENTIAL_REVOKED,
    ACCOUNT_INVITED,
    ACCOUNT_UPDATED,
    INVITATION_RESENT,
    INVITATION_REVOKED,
    INVITATION_LINK_COPIED,
    INVITATION_CONFIRMED,
    ACCOUNT_ACTIVATED,
    ACCOUNT_FIRST_SIGN_IN,
    ACCOUNT_SIGN_IN_BLOCKED,
    PASSWORD_SET,
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET,
    EMAIL_SUBMITTED,
    EMAIL_SUBMISSION_FAILED,
    EMAIL_DELIVERY_CONFIRMED,
    CODE_SIGN_IN_RETIRED,
    CODE_SIGN_IN_RESTORED;

    /** What kind of record the action changes. */
    public String targetType() {
        if (name().startsWith("CREDENTIAL_")) {
            return "CREDENTIAL";
        }
        if (name().startsWith("CODE_SIGN_IN_")) {
            return "SETTING";
        }
        return "OPERATOR";
    }
}
