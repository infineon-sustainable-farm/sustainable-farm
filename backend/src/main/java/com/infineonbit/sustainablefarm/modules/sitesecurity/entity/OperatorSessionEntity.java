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
 * A signed-in operator. Only the SHA-256 hash of the cookie value is stored, so
 * reading this table does not let anyone sign in. The session ends at a fixed
 * time: using it does not extend it.
 */
@Entity
@Table(name = "site_security_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_site_security_sessions_token", columnNames = "tokenHash"),
        indexes = @Index(name = "idx_site_security_sessions_operator", columnList = "operatorId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorSessionEntity {
    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String operatorId;

    @ToString.Exclude
    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;
}
