package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.Permission;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A Farm Security account (administrator or user). The email is stored in lower case.
 *
 * <p>The account columns added for passwords and invitations are all nullable,
 * so ddl-auto=update can add them to an existing table. {@code accountStatus}
 * and {@code accountRole} are the source of truth; the original {@code active}
 * and {@code role} columns are kept in step (role holds ADMIN or READ, which
 * the CHECK constraint of existing databases accepts) so that older queries,
 * and a rollback to the previous version, keep working. Rows that predate the
 * migration (new columns empty) are read through the {@code effective*} methods.</p>
 */
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

    /** Legacy mirror of accountRole: ADMIN or READ. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SiteSecurityRole role;

    /** Legacy mirror of accountStatus: true exactly when the account is ACTIVE. */
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

    @Column(length = 120)
    private String fullName;

    @Column(length = 16)
    private String accountRole;

    @Column(length = 16)
    private String accountStatus;

    @Column(length = 24)
    private String disabledReason;

    @Column(length = 16)
    private String category;

    /** Argon2id PHC string; empty until the person chooses a password. */
    @ToString.Exclude
    @Column(length = 255)
    private String passwordHash;

    private Instant passwordSetAt;

    /** When the person completed activation (invitation accepted). */
    private Instant activatedAt;

    private Instant firstLoginAt;

    private Instant lastPasswordLoginAt;

    /** Last day (UTC) on which the account may be used; empty means no end. */
    private LocalDate validUntil;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "site_security_account_permissions", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "permission", length = 32)
    private Set<String> permissions = new HashSet<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "site_security_account_zones", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "zone_id", length = 32)
    private Set<String> zoneIds = new HashSet<>();

    public AccountStatus effectiveStatus() {
        if (accountStatus != null) {
            return AccountStatus.valueOf(accountStatus);
        }
        return active ? AccountStatus.ACTIVE : AccountStatus.DISABLED;
    }

    public AccountRole effectiveRole() {
        if (accountRole != null) {
            return AccountRole.valueOf(accountRole);
        }
        return role == SiteSecurityRole.ADMIN ? AccountRole.ADMIN : AccountRole.USER;
    }

    /** An operator of the old read-only role, not migrated yet, keeps exactly that read access. */
    public Set<String> effectivePermissions() {
        if (accountRole == null && role != SiteSecurityRole.ADMIN) {
            return Set.of(Permission.LEGACY_READ_ALL.name());
        }
        return permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public Set<String> effectiveZoneIds() {
        return zoneIds == null ? Set.of() : Set.copyOf(zoneIds);
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    /** The instant access ends (start of the day after validUntil, UTC), or null. */
    public Instant validityEnd() {
        return validUntil == null ? null : validUntil.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public boolean validityEndedAt(Instant now) {
        Instant end = validityEnd();
        return end != null && !now.isBefore(end);
    }

    /** Active, and within its validity period: the only state that may hold a session. */
    public boolean usableAt(Instant now) {
        return effectiveStatus() == AccountStatus.ACTIVE && !validityEndedAt(now);
    }

    /**
     * Gives a row that predates accounts its account fields, exactly as the stage 0
     * migration does: same role and status, and an old read-only operator keeps
     * LEGACY_READ_ALL (and gets no zone). Does nothing on a migrated row.
     */
    public void adoptAccountFields() {
        if (accountStatus != null) {
            return;
        }
        AccountRole currentRole = effectiveRole();
        AccountStatus currentStatus = effectiveStatus();
        if (currentRole == AccountRole.USER) {
            Set<String> withLegacy = new HashSet<>(permissions == null ? Set.of() : permissions);
            withLegacy.add(Permission.LEGACY_READ_ALL.name());
            permissions = withLegacy;
        }
        applyRole(currentRole);
        applyStatus(currentStatus);
        if (currentStatus == AccountStatus.DISABLED && disabledReason == null) {
            disabledReason = AccountValues.DisabledReason.ADMIN.name();
        }
    }

    /** Sets the stored status and keeps the legacy {@code active} column in step. */
    public void applyStatus(AccountStatus status) {
        this.accountStatus = status.name();
        this.active = status == AccountStatus.ACTIVE;
    }

    /** Sets the application role and keeps the legacy {@code role} column in step. */
    public void applyRole(AccountRole newRole) {
        this.accountRole = newRole.name();
        this.role = newRole == AccountRole.ADMIN ? SiteSecurityRole.ADMIN : SiteSecurityRole.READ;
    }
}
