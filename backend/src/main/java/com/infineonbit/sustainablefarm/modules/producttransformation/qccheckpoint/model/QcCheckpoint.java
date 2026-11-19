package com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.model;

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
@Table(name = "qc_checkpoint",
        uniqueConstraints = @UniqueConstraint(name = "uk_qc_checkpoint_code", columnNames = "code"),
        indexes = {
        @Index(name = "idx_qc_checkpoint_inspector_id", columnList = "inspector_id"),
        @Index(name = "idx_qc_checkpoint_checkpoint_time", columnList = "checkpoint_time"),
        @Index(name = "idx_qc_checkpoint_batch_id", columnList = "batch_id")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
public class QcCheckpoint extends BaseEntity {

    /** Business code, assigned by the client and shown by the API; the key is {@code id}. */
    @Column(name = "code", nullable = false, length = 50)
    private String checkpointId;

    // Lazy: the read queries fetch it (entity graph), responses are built in a read-only transaction.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", foreignKey = @ForeignKey(name = "fk_qc_checkpoint_batch"), nullable = false)
    @ToString.Exclude
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

    // Lazy: the read queries fetch it (entity graph), responses are built in a read-only transaction.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id", foreignKey = @ForeignKey(name = "fk_qc_checkpoint_inspector"))
    @ToString.Exclude
    private Operator inspector;

    @Column(name = "checkpoint_time", nullable = false)
    private LocalDateTime checkpointTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Validate inspector is certified
     * Business Rule: QC inspectors must be certified
     */
    @PrePersist
    @PreUpdate
    void validateInspector() {
        if (inspector != null && inspector.getRole() != Role.QC_INSPECTOR) {
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

}