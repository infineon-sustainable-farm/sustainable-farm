package com.infineonbit.sustainablefarm.modules.producttransformation.batch.model;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.model.QcCheckpoint;
import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.model.ComplianceRecord;
import com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.model.RawIntake;
import com.infineonbit.sustainablefarm.modules.producttransformation.washsortrecord.model.WashSortRecord;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model.DryingRun;
import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.model.PackagingRecord;

import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

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
@Table(name = "batch",
        uniqueConstraints = @UniqueConstraint(name = "uk_batch_code", columnNames = "code"),
        indexes = {
        @Index(name = "idx_batch_current_status", columnList = "current_status"),
        @Index(name = "idx_batch_harvest_date", columnList = "harvest_date"),
        @Index(name = "idx_batch_variety_key", columnList = "variety_key"),
        @Index(name = "idx_batch_block_id", columnList = "block_id")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Batch extends BaseEntity {

    /** Business code, assigned by the client and shown by the API; the key is {@code id}. */
    @Column(name = "code", nullable = false, length = 50)
    private String batchId;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    /** Catalogue key of the variety, for example {@code KENT}: foreign key to plants {@code variety_reference}. */
    @Column(name = "variety_key", nullable = false, length = VarietyCatalog.KEY_LENGTH)
    private String varietyKey;

    @Column(name = "harvest_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal harvestQuantityKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    private BatchStatus currentStatus = BatchStatus.CREATED;

    /**
     * Block of the farm the lot was harvested on: foreign key to {@code farm.block} (ADR 0004). The
     * API shows it as {@code blockId} (the block code) and {@code farmId} (the name of its farm).
     */
    @Column(name = "block_id", nullable = false)
    private Long farmBlockId;

    // Relationships
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<WashSortRecord> washSortRecords = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<DryingRun> dryingRuns = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PackagingRecord> packagingRecords = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<QcCheckpoint> qcCheckpoints = new ArrayList<>();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ComplianceRecord> complianceRecords = new ArrayList<>();

    @OneToOne(mappedBy = "batch", cascade = CascadeType.ALL)
    @ToString.Exclude
    private RawIntake rawIntake;

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