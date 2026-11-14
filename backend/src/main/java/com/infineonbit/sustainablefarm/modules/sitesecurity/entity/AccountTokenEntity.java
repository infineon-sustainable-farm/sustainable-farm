package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A single-use link sent by email: an invitation (or password setup) or a
 * password reset. Only the SHA-256 hash of the random value is stored; the
 * value itself exists only in the email.
 */
@Entity
@Table(name = "site_security_account_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_site_security_account_tokens_hash", columnNames = "tokenHash"),
        indexes = @Index(name = "idx_site_security_account_tokens_account", columnList = "accountId,purpose"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTokenEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String accountId;

    /** INVITATION or PASSWORD_RESET, as plain text. */
    @Column(nullable = false, length = 24)
    private String purpose;

    @ToString.Exclude
    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    private Instant revokedAt;

    /** The recipient pressed "Confirm invitation": a hint, never acceptance. */
    private Instant confirmedAt;

    /** Operator id of the admin, or "self" / "system". */
    @Column(nullable = false, length = 36)
    private String createdBy;

    public boolean isOpenAt(Instant now) {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }
}
