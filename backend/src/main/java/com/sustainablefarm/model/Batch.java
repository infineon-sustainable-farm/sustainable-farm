package com.sustainablefarm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch Entity
 * Central traceability entity for mango processing - Core Processing Entity
 * 
 * Business Rule: One batch corresponds to one harvest event
 * Business Rule: Status transitions: CREATED → INTAKE → WASHING → DRYING → PACKAGING → COMPLETED → SHIPPED
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Entity
@Table(name = "batch")
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Batch {

    @Id
    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "mango_variety", nullable = false, length = 20)
    private HarvestEvent.MangoVariety mangoVariety;

    @Column(name = "harvest_quantity_kg", nullable = false, precision = 10, scale = 2)
    private Double harvestQuantityKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    private BatchStatus currentStatus = BatchStatus.CREATED;

    @Column(name = "farm_id", nullable = false, length = 50)
    private String farmId;

    @Column(name = "block_id", nullable = false, length = 50)
    private String blockId;

    @Column(name = "created_at", updatable = false)
    private java.time.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.time.Timestamp updatedAt;

    // Relationships
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WashSortRecord> washSortRecords = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DryingRun> dryingRuns = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PackagingRecord> packagingRecords = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<QcCheckpoint> qcCheckpoints = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ComplianceRecord> complianceRecords = new ArrayList<>();

    @OneToOne(mappedBy = "batch", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RawIntake rawIntake;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.sql.Timestamp(System.currentTimeMillis());
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.sql.Timestamp(System.currentTimeMillis());
    }

    /**
     * Batch Status Enum
     * Business Rule: Matches database CHECK constraint
     * Business Rule: Status transitions must follow the processing workflow
     */
    public enum BatchStatus {
        CREATED,
        INTAKE,
        WASHING,
        DRYING,
        PACKAGING,
        COMPLETED,
        SHIPPED,
        REJECTED
    }

    /**
     * Advance batch status to next stage
     * Business Rule: Validates status transition
     */
    public void advanceStatus() {
        switch (this.currentStatus) {
            case CREATED:
                this.currentStatus = BatchStatus.INTAKE;
                break;
            case INTAKE:
                this.currentStatus = BatchStatus.WASHING;
                break;
            case WASHING:
                this.currentStatus = BatchStatus.DRYING;
                break;
            case DRYING:
                this.currentStatus = BatchStatus.PACKAGING;
                break;
            case PACKAGING:
                this.currentStatus = BatchStatus.COMPLETED;
                break;
            case COMPLETED:
                this.currentStatus = BatchStatus.SHIPPED;
                break;
            default:
                throw new IllegalStateException("Cannot advance status from " + this.currentStatus);
        }
    }
}