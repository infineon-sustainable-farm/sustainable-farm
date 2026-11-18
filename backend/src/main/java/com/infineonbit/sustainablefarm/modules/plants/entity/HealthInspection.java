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
 * A visit of a block to check the health of its trees, with a health score and
 * the problems seen, stored as {@link HealthFinding} rows.
 *
 * <p>A visit with nothing found is recorded too: it is the trace that the block
 * was checked. The block needs no recorded planting, since termites can be seen
 * before the trees go in, as for a fertilizer application.
 *
 * <p>The block is a {@code farm.block} row, which gives the farm, as for a
 * planting.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@EntityListeners(BlockLabelListener.class)
@Table(name = "health_inspection", indexes = @Index(name = "idx_health_inspection_block_id", columnList = "block_id"))
@Getter
@Setter
@NoArgsConstructor
public class HealthInspection implements BlockRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Inspected block, a {@code farm.block} row (foreign key {@code fk_health_inspection_block}). */
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final BlockLink block = new BlockLink();

    @Column(name = "inspected_on", nullable = false)
    private LocalDate inspectedOn;

    /** Health of the block seen during the visit, from 0 to 100. */
    @Column(name = "health_score_pct", nullable = false)
    private Integer healthScorePct;

    @Column(name = "observer")
    private String observer;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 30)
    private InspectionMethod method;

    /** Where the row comes from, for example {@code "user_entry"} for the inspection route. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /** Farm of the block, read from the farm referential; {@code null} without a block. */
    public Long getFarmId() {
        return block.label(blockId).farmId();
    }

    /** Sets the farm by hand, on a row built in memory; a saved row takes it from its block. */
    public void setFarmId(Long farmId) {
        block.setFarmId(farmId);
    }

    /** Code of the block, for example {@code "C"}, read from the farm referential. */
    public String getBlockCode() {
        return block.label(blockId).code();
    }

    /** Sets the block code by hand, on a row built in memory; a saved row takes it from its block. */
    public void setBlockCode(String blockCode) {
        block.setCode(blockCode);
    }

    /** Points the row to a block of the farm referential, with the label plants shows for it. */
    public void assignBlock(Long blockId, BlockLabel label) {
        this.blockId = blockId;
        block.set(blockId, label);
    }

    @Override
    public void useBlockLabels(BlockLabelSource source) {
        block.useSource(source);
    }

    @PrePersist
    @PreUpdate
    void requireResolvedBlock() {
        block.requireResolved("health inspection");
    }
}
