package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A person allowed to sign in to Site Security. The email is stored in lower case. */
@Entity
@Table(name = "site_security_operators",
        uniqueConstraints = @UniqueConstraint(name = "uk_site_security_operators_email", columnNames = "email"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorEntity {
    @Id
    private String id;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SiteSecurityRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, length = 254)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 254)
    private String disabledBy;

    private Instant disabledAt;

    private Instant lastLoginAt;
}
