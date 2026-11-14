package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.EmailStateDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.UserDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountTokenEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.DisabledReason;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.EmailDeliveryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import java.time.Instant;
import java.util.Locale;

/** What admins see: the displayed status is derived from the stored state and the latest invitation. */
public final class AccountViews {

    public static final String PENDING = "pending";
    public static final String ACTIVATION_STARTED = "activation_started";
    public static final String ACTIVE = "active";
    public static final String EXPIRED = "expired";
    public static final String REVOKED = "revoked";
    public static final String DISABLED = "disabled";

    private AccountViews() {
    }

    /**
     * pending / activation_started / expired: not activated yet, from the latest invitation.
     * active: activated (accepted) and within validity. revoked: invitation withdrawn.
     * disabled: disabled by an admin, or validity ended.
     */
    public static String displayStatus(OperatorEntity account, AccountTokenEntity latestInvitation, Instant now) {
        AccountStatus status = account.effectiveStatus();
        if (status == AccountStatus.DISABLED) {
            return DisabledReason.INVITATION_REVOKED.name().equals(account.getDisabledReason()) ? REVOKED : DISABLED;
        }
        if (status == AccountStatus.ACTIVE) {
            return account.validityEndedAt(now) ? DISABLED : ACTIVE;
        }
        if (latestInvitation == null || latestInvitation.getRevokedAt() != null || !latestInvitation.getExpiresAt().isAfter(now)) {
            return EXPIRED;
        }
        return latestInvitation.getConfirmedAt() != null ? ACTIVATION_STARTED : PENDING;
    }

    public static UserDto toDto(OperatorEntity account, AccountTokenEntity latestInvitation,
            EmailDeliveryEntity latestEmail, Instant now) {
        boolean pending = account.effectiveStatus() == AccountStatus.PENDING;
        return UserDto.builder()
                .id(account.getId())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .role(AccountValues.api(account.effectiveRole()))
                .category(account.getCategory() == null ? null : account.getCategory().toLowerCase(Locale.ROOT))
                .permissions(account.effectivePermissions().stream().map(p -> p.toLowerCase(Locale.ROOT)).sorted().toList())
                .zoneIds(account.effectiveZoneIds().stream().sorted().toList())
                .validUntil(account.getValidUntil())
                .status(displayStatus(account, latestInvitation, now))
                .passwordSet(account.hasPassword())
                .legacyCodeSignIn(account.effectiveStatus() == AccountStatus.ACTIVE && !account.hasPassword())
                .invitedAt(latestInvitation == null ? null : latestInvitation.getCreatedAt())
                .invitedBy(account.getCreatedBy())
                .invitationExpiresAt(pending && latestInvitation != null ? latestInvitation.getExpiresAt() : null)
                .activationStartedAt(latestInvitation == null ? null : latestInvitation.getConfirmedAt())
                .activatedAt(account.getActivatedAt())
                .firstLoginAt(account.getFirstLoginAt())
                .lastLoginAt(account.getLastLoginAt())
                .disabledAt(account.getDisabledAt())
                .disabledBy(account.getDisabledBy())
                .validityEnded(account.validityEndedAt(now))
                .emailDelivery(latestEmail == null ? null : emailState(latestEmail))
                .build();
    }

    static EmailStateDto emailState(EmailDeliveryEntity delivery) {
        return emailState(delivery, id -> null);
    }

    /** With the attesting admin's email resolved (history view); the table view leaves it out. */
    static EmailStateDto emailState(EmailDeliveryEntity delivery, java.util.function.Function<String, String> emailOf) {
        return EmailStateDto.builder()
                .deliveryConfirmedAt(delivery.getDeliveryConfirmedAt())
                .deliveryConfirmedByEmail(delivery.getDeliveryConfirmedBy() == null ? null
                        : emailOf.apply(delivery.getDeliveryConfirmedBy()))
                .deliveryEvidence(delivery.getDeliveryEvidence())
                .deliveryId(delivery.getId())
                .kind(delivery.getKind().toLowerCase(Locale.ROOT))
                .submission(delivery.getSubmissionStatus().toLowerCase(Locale.ROOT))
                .delivery(delivery.getDeliveryStatus().toLowerCase(Locale.ROOT))
                .attempts(delivery.getAttempts())
                .submittedAt(delivery.getSubmittedAt())
                .lastError(delivery.getLastError())
                .build();
    }

    /** "a***@e***.org": enough for the person to recognise their address, not to learn someone else's. */
    public static String maskEmail(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        int dot = domain.lastIndexOf('.');
        String domainHead = dot > 0 ? domain.substring(0, dot) : domain;
        String tld = dot > 0 ? domain.substring(dot) : "";
        return local.charAt(0) + "***@" + domainHead.charAt(0) + "***" + tld;
    }

    public static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        return fullName.trim().split("\\s+")[0];
    }
}
