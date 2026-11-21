package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

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
 * Site Security authorization of one person of the shared people directory (ADR 0004): the
 * module role, the permissions of a regular user and the zones it may see. It references the
 * person by id ({@code person_id}, foreign key to {@code person}) and copies none of its
 * details. It is not an application account: it holds no credential, session or password,
 * and the person is resolved by the platform authentication, not by this module.
 */
@Entity
@Table(name = "site_security_operators",
        uniqueConstraints = @UniqueConstraint(name = "uk_site_security_operators_person_id",
                columnNames = "person_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSecurityOperatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    /** The person of the shared directory ({@code staff.api.PersonDirectory}). */
    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SiteSecurityRole role;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "site_security_operator_permissions",
            joinColumns = @JoinColumn(name = "operator_id"))
    @Column(name = "permission", length = 32)
    private Set<String> permissions = new HashSet<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "site_security_operator_zones",
            joinColumns = @JoinColumn(name = "operator_id"))
    @Column(name = "zone_id", length = 32)
    private Set<String> zoneIds = new HashSet<>();

    public boolean isAuthorizationActive() {
        return role == SiteSecurityRole.ADMIN || role == SiteSecurityRole.USER;
    }

    public SiteSecurityPrincipal toPrincipal() {
        return new SiteSecurityPrincipal(id, role, permissions, zoneIds);
    }
}
