package com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.model;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Role;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;

import jakarta.persistence.*;
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
@Table(name = "compliance_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_compliance_record_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_compliance_record_batch_id", columnList = "batch_id"),
                @Index(name = "idx_compliance_record_auditor_id", columnList = "auditor_id")
        })
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ComplianceRecord extends BaseEntity {

    /** Business code, assigned by the client and shown by the API; the key is {@code id}. */
    @Column(name = "code", nullable = false, length = 50)
    private String recordId;

    // Lazy: the read queries fetch it (entity graph), responses are built in a read-only transaction.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", foreignKey = @ForeignKey(name = "fk_compliance_record_batch"), nullable = false)
    @ToString.Exclude
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

    // Lazy: the read queries fetch it (entity graph), responses are built in a read-only transaction.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id", foreignKey = @ForeignKey(name = "fk_compliance_record_auditor"))
    @ToString.Exclude
    private Operator auditor;

    @Column(name = "audit_date", nullable = false)
    private LocalDate auditDate;

    @Column(name = "next_audit_date")
    private LocalDate nextAuditDate;

    /**
     * Validate auditor is certified
     * Business Rule: Auditors must have AUDITOR role
     */
    @PrePersist
    @PreUpdate
    void validateAuditor() {
        if (auditor != null && auditor.getRole() != Role.AUDITOR) {
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

}