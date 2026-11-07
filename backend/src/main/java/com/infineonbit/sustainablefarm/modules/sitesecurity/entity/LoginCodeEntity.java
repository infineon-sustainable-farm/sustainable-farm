package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** A one-time sign-in code. Only its keyed hash is stored, never the code itself. */
@Entity
@Table(name = "site_security_login_codes",
        indexes = @Index(name = "idx_site_security_login_codes_operator", columnList = "operatorId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginCodeEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String operatorId;

    @Column(nullable = false, length = 64)
    @ToString.Exclude
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    /** Set when the code is used or replaced by a newer one. */
    private Instant consumedAt;

    @Column(nullable = false)
    private Instant createdAt;
}
