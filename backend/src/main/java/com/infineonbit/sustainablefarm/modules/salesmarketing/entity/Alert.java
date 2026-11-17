package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

/**
 * Maps to the "alerts" table.
 * Foreign keys are kept as plain Integer id fields.
 */
@Entity
@Table(name = "alerts", uniqueConstraints = @UniqueConstraint(name = "alerts_alert_code_key", columnNames = "alert_code"),
        indexes = {
                @Index(name = "idx_alerts_severity", columnList = "severity"),
                @Index(name = "idx_alerts_status", columnList = "status")
        })
@Check(name = "alerts_severity_check", constraints = "severity IN ('critical', 'warning', 'info')")
@Check(name = "alerts_status_check", constraints = "status IN ('open', 'acknowledged', 'resolved', 'dismissed')")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Integer alertId;

    @Column(name = "alert_code", length = 20)
    private String alertCode;

    @Column(name = "alert_type", length = 50)
    private String alertType;

    @NotBlank(message = "severity is required")
    @ColumnDefault("'info'")
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @NotBlank(message = "message is required")
    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Column(name = "region", length = 80)
    private String region;

    @Column(name = "origin", length = 80)
    private String origin;

    @Column(name = "related_table", length = 50)
    private String relatedTable;

    @Column(name = "related_id")
    private Integer relatedId;

    @ColumnDefault("'open'")
    @Column(name = "status", length = 20)
    private String status;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ---- Getters and setters ----

    public Integer getAlertId() { return alertId; }
    public void setAlertId(Integer alertId) { this.alertId = alertId; }

    public String getAlertCode() { return alertCode; }
    public void setAlertCode(String alertCode) { this.alertCode = alertCode; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getRelatedTable() { return relatedTable; }
    public void setRelatedTable(String relatedTable) { this.relatedTable = relatedTable; }

    public Integer getRelatedId() { return relatedId; }
    public void setRelatedId(Integer relatedId) { this.relatedId = relatedId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}