package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

/**
 * One administrative change: who did what to which record, and when. Rows are
 * only ever added: a later change (re-enabling an operator, say) adds a new row
 * and keeps the earlier ones. People are referred to by their operator id, and
 * no email, name or secret is stored here.
 */
@Entity
@Immutable
@Table(name = "site_security_audit_events",
        indexes = {
                @Index(name = "idx_site_security_audit_events_target", columnList = "targetType,targetId"),
                @Index(name = "idx_site_security_audit_events_occurred", columnList = "occurredAt")
        })
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSecurityAuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Instant occurredAt;

    /** The account that made the change: an admin, the account itself, or "system". */
    @Column(nullable = false, length = 36)
    private String actorOperatorId;

    /** ADMIN, SELF or SYSTEM. Empty on rows written before self-service events existed: those were admins. */
    @Column(length = 16)
    private String actorKind;

    /**
     * Plain text on purpose. Mapped as an enum (even through a converter),
     * Hibernate adds a CHECK constraint listing today's actions, and
     * ddl-auto=update never changes an existing constraint: adding an action
     * later would make every audited change fail on PostgreSQL.
     */
    @Getter(AccessLevel.NONE)
    @Column(name = "action", nullable = false, length = 32)
    private String actionName;

    @Column(nullable = false, length = 16)
    private String targetType;

    @Column(nullable = false, length = 36)
    private String targetId;

    /** A short, non-personal detail, such as "read->admin" for a role change. */
    @Column(length = 64)
    private String detail;

    public SiteSecurityAuditAction getAction() {
        return SiteSecurityAuditAction.valueOf(actionName);
    }
}
