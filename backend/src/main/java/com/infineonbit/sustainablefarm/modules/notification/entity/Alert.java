package com.infineonbit.sustainablefarm.modules.notification.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus;
import com.infineonbit.sustainablefarm.modules.notification.api.SourceModule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * An alert raised by a module, the single alert store of the application (ADR 0004).
 *
 * <p>The columns are the union of what energy supply, sales and marketing and water supply
 * record about an alert; a module leaves the others empty.</p>
 */
@Entity
@Table(name = "alert",
        uniqueConstraints = @UniqueConstraint(name = "alert_code_key", columnNames = "code"),
        indexes = {
                @Index(name = "idx_alert_source_status", columnList = "source_module, status"),
                @Index(name = "idx_alert_source_title", columnList = "source_module, title"),
                @Index(name = "idx_alert_recipient", columnList = "recipient_person_id"),
                @Index(name = "idx_alert_related_entity", columnList = "related_entity_type, related_entity_id")
        })
@Getter
@Setter
public class Alert extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_module", nullable = false, length = 30)
    private SourceModule source;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "type", length = 50)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id", length = 64)
    private String relatedEntityId;

    @Column(name = "region", length = 80)
    private String region;

    @Column(name = "origin", length = 80)
    private String origin;

    @Column(name = "recipient_person_id")
    private Long recipientPersonId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
