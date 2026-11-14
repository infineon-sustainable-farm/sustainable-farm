package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A browser that has already proved it belongs to the account (a password
 * sign-in, an activation or a password reset). It does not sign anybody in: it
 * only lets that browser keep trying the password while strangers are being
 * held back, so an attacker cannot lock the owner out. Only the SHA-256 hash of
 * the cookie value is stored.
 */
@Entity
@Table(name = "site_security_trusted_devices",
        indexes = @Index(name = "idx_site_security_trusted_devices_account", columnList = "accountId"))
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustedDeviceEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String accountId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastUsedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    public boolean isValidAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
