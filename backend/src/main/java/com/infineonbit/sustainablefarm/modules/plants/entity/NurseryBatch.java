package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A batch of young plants in the nursery, from the seed or the purchase to the
 * transplant into the orchard.
 *
 * <p>The row holds what is known when the batch is started, and nothing that
 * changes afterwards. The current number of plants, the survival rate and the
 * current stage are computed on every read from its {@link NurseryEvent} rows,
 * as the stock of a fertilizer is computed from its movements. The initial
 * stage is the first of those events.
 *
 * <p>{@code batchCode} is unique within a farm, a missing farm being a farm of
 * its own. The service checks it: a unique constraint would not do, since
 * PostgreSQL never finds two missing farms equal.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@EntityListeners(BlockLabelListener.class)
@Table(name = "nursery_batch", indexes = {
        @Index(name = "idx_nursery_batch_farm_id", columnList = "farm_id"),
        @Index(name = "idx_nursery_batch_planned_block_id", columnList = "planned_block_id")})
@Getter
@Setter
@NoArgsConstructor
public class NurseryBatch implements BlockRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Farm of the nursery, a {@code farm} row (foreign key {@code fk_nursery_batch_farm}), or NULL. */
    @Column(name = "farm_id")
    private Long farmId;

    /** Letters, digits and hyphens, upper-cased, for example {@code "P1"}. */
    @Column(name = "batch_code", nullable = false, length = 20)
    private String batchCode;

    /** Variety grafted or to be grafted, for example {@code "Keitt"}. A transplant plants this variety. */
    @Column(name = "variety_name", nullable = false)
    private String varietyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 30)
    private NurseryOrigin origin;

    /** Nursery the plants were bought from; set for a purchased batch only. */
    @Column(name = "supplier")
    private String supplier;

    /** Lot number given by the supplier, optional; set for a purchased batch only. */
    @Column(name = "supplier_lot_number", length = 50)
    private String supplierLotNumber;

    /** Day of the sowing, or of the reception for a purchased batch. */
    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    @Column(name = "initial_count", nullable = false)
    private Integer initialCount;

    /** Planned day of the transplant, which may be in the future. */
    @Column(name = "planned_transplant_on", nullable = false)
    private LocalDate plannedTransplantOn;

    /** Block the batch is meant for, a {@code farm.block} row (foreign key {@code fk_nursery_batch_planned_block}), or NULL. */
    @Column(name = "planned_block_id")
    private Long plannedBlockId;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final BlockLink plannedBlock = new BlockLink();

    /** Where the row comes from, for example {@code "user_entry"} for the batch route. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /** Code of the block the batch is meant for, read from the farm referential, or {@code null}. */
    public String getPlannedBlockCode() {
        return plannedBlock.label(plannedBlockId).code();
    }

    /** Sets the block code by hand, on a row built in memory; a saved row takes it from its block. */
    public void setPlannedBlockCode(String blockCode) {
        plannedBlock.setCode(blockCode);
    }

    /** Points the row to a block of the farm referential, with the label plants shows for it. */
    public void assignPlannedBlock(Long blockId, BlockLabel label) {
        this.plannedBlockId = blockId;
        plannedBlock.set(blockId, label);
    }

    @Override
    public void useBlockLabels(BlockLabelSource source) {
        plannedBlock.useSource(source);
    }

    @PrePersist
    @PreUpdate
    void requireResolvedBlock() {
        plannedBlock.requireResolved("nursery batch");
    }
}
