package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import java.util.Locale;
import java.util.Set;

/**
 * Values of the Farm Security account columns. They are stored as plain text
 * (never as database enums): an enum column gets a CHECK constraint that
 * ddl-auto=update can never extend.
 */
public final class AccountValues {

    private AccountValues() {
    }

    /** Application role: what the account may do in Farm Security. */
    public enum AccountRole { ADMIN, USER }

    /** Stored account state. EXPIRED and ACTIVATION_STARTED are derived from the invitation. */
    public enum AccountStatus { PENDING, ACTIVE, DISABLED }

    /**
     * Why an account is DISABLED, which also says what enabling it restores:
     * ADMIN (was active: back to ACTIVE), ADMIN_PENDING (disabled before
     * activation: back to PENDING), INVITATION_REVOKED (back to PENDING).
     */
    public enum DisabledReason { ADMIN, ADMIN_PENDING, INVITATION_REVOKED }

    /** Professional category of the person. Descriptive only: it grants nothing. */
    public enum UserCategory { WORKER, VISITOR, CONTRACTOR, SECURITY }

    /**
     * Features a USER may use. Admins can use everything. LEGACY_READ_ALL is only
     * given by the migration to operators who had the old read-only role: it
     * keeps their previous read access (every view, every zone) and nothing more.
     */
    public enum Permission {
        VIEW_DASHBOARD, VIEW_ZONES, VIEW_ACCESS_LOG, VIEW_ACCESS_REGISTER, LEGACY_READ_ALL;

        /** The permissions an admin can grant in the invite and edit forms. */
        public static final Set<Permission> GRANTABLE =
                Set.of(VIEW_DASHBOARD, VIEW_ZONES, VIEW_ACCESS_LOG, VIEW_ACCESS_REGISTER);
    }

    /** Single-use links sent by email. */
    public enum TokenPurpose { INVITATION, PASSWORD_RESET }

    /** Emails the accounts feature sends. */
    public enum EmailKind { INVITATION, PASSWORD_SETUP, PASSWORD_RESET, PASSWORD_CHANGED, ACTIVATION_NOTICE }

    /** Our side: did the provider accept the message. SUBMITTED is not delivery. */
    public enum SubmissionStatus { QUEUED, SENDING, RETRY_SCHEDULED, SUBMITTED, FAILED, LOCAL_CONSOLE, MAIL_DISABLED }

    /** Provider side. Only provider evidence, or an admin attesting it, may change it. */
    public enum DeliveryStatus { UNKNOWN, CONFIRMED_BY_ADMIN }

    public static String api(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }

    /** Parses an API value (any case). Throws IllegalArgumentException for an unknown value. */
    public static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing value");
        }
        return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    }
}
