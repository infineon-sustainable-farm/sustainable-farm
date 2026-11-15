package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityIdentity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Farm Security authorization assignment keyed by the central platform user.
 * This is not an application account and contains no credentials or sessions.
 */
@Entity
@Table(name = "site_security_operators",
        uniqueConstraints = @UniqueConstraint(name = "uk_site_security_operators_central_user",
                columnNames = "central_user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "central_user_id", nullable = false, length = 128)
    private String centralUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SiteSecurityRole role;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "site_security_account_permissions",
            joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "permission", length = 32)
    private Set<String> permissions = new HashSet<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "site_security_account_zones",
            joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "zone_id", length = 32)
    private Set<String> zoneIds = new HashSet<>();

    public boolean isAuthorizationActive() {
        return role == SiteSecurityRole.ADMIN || role == SiteSecurityRole.USER;
    }

    public SiteSecurityPrincipal toPrincipal(SiteSecurityIdentity identity) {
        return new SiteSecurityPrincipal(identity.centralUserId(), role, permissions, zoneIds);
    }
}
