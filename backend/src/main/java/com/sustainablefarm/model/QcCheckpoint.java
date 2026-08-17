package com.sustainablefarm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

/**
 * QcCheckpoint Entity
 * Quality control checkpoint data - Core Processing Entity
 * 
 * Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages
 * Business Rule: QC inspectors must be certified
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "qc_checkpoint")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class QcCheckpoint {

    @Id
    @Column(name = "checkpoint_id", length = 50)
    private String checkpointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, referencedColumnName = "batch_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Batch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private QcStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 10)
    private QcResult result;

    @Column(name = "defects", columnDefinition = "TEXT")
    private String defects;

    @Column(name = "defects_count", nullable = false)
    private Integer defectsCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id", referencedColumnName = "operator_id")
    private Operator inspector;

    @Column(name = "checkpoint_time", nullable = false)
    private LocalDateTime checkpointTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
        validateInspector();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
        validateInspector();
    }

    /**
     * Validate inspector is certified
     * Business Rule: QC inspectors must be certified
     */
    private void validateInspector() {
        if (inspector != null && inspector.getRole() != Operator.Role.QC_INSPECTOR) {
            throw new IllegalArgumentException(
                "Inspector must have QC_INSPECTOR role. Current role: " + inspector.getRole()
            );
        }
    }

    /**
     * Check if this is a mandatory checkpoint
     * Business Rule: Mandatory at washing (Phase 2) and cooling (Phase 5) stages
     */
    public boolean isMandatory() {
        return stage == QcStage.WASHING || stage == QcStage.COOLING;
    }

    /**
     * Check if checkpoint passed
     * Business Rule: Result must be PASS for batch to proceed
     */
    public boolean hasPassed() {
        return result == QcResult.PASS;
    }

    /**
     * QcStage Enum
     * Business Rule: Matches database CHECK constraint
     */
    public enum QcStage {
        INTAKE,
        WASHING,
        DRYING,
        COOLING,
        PACKAGING,
        FINAL
    }

    /**
     * QcResult Enum
     * Business Rule: Matches database CHECK constraint
     */
    public enum QcResult {
        PENDING,
        PASS,
        FAIL,
        REWORK
    }
}