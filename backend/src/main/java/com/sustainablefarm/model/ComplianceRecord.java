package com.sustainablefarm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;

/**
 * ComplianceRecord Entity
 * HACCP compliance data - Core Processing Entity
 * 
 * Business Rule: Mandatory for HACCP certification
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "compliance_record")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRecord {

    @Id
    @Column(name = "record_id", length = 50)
    private String recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, referencedColumnName = "batch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Batch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_type", nullable = false, length = 30)
    private ComplianceType complianceType;

    @Column(name = "requirement", nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 10)
    private ComplianceResult result;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id", referencedColumnName = "operator_id")
    private Operator auditor;

    @Column(name = "audit_date", nullable = false)
    private LocalDate auditDate;

    @Column(name = "next_audit_date")
    private LocalDate nextAuditDate;

    @Column(name = "created_at", updatable = false)
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
        validateAuditor();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
        validateAuditor();
    }

    /**
     * Validate auditor is certified
     * Business Rule: Auditors must have AUDITOR role
     */
    private void validateAuditor() {
        if (auditor != null && auditor.getRole() != Operator.Role.AUDITOR) {
            throw new IllegalArgumentException(
                "Auditor must have AUDITOR role. Current role: " + auditor.getRole()
            );
        }
    }

    /**
     * Check if compliance is met
     * Business Rule: Result must be COMPLIANT for HACCP certification
     */
    public boolean isCompliant() {
        return result == ComplianceResult.COMPLIANT;
    }

    /**
     * Check if audit is overdue
     * Business Rule: Next audit date tracking for compliance
     */
    public boolean isAuditOverdue() {
        return nextAuditDate != null && nextAuditDate.isBefore(LocalDate.now());
    }

    /**
     * Compliance Type Enum
     * Business Rule: Matches database CHECK constraint
     */
    public enum ComplianceType {
        HACCP,
        FOOD_SAFETY,
        EU_EXPORT,
        HYGIENE,
        TRACEABILITY
    }

    /**
     * Compliance Result Enum
     * Business Rule: Matches database CHECK constraint
     */
    public enum ComplianceResult {
        COMPLIANT,
        NON_COMPLIANT,
        PENDING
    }
}