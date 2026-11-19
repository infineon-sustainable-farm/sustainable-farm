package com.infineonbit.sustainablefarm.modules.plants.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An application of a {@link FertilizerProduct} on a block that the farmer
 * plans ahead: which fertilizer, where, when and how much.
 *
 * <p>The quantity is the farmer's own figure, in the unit of the fertilizer.
 * The application recommends no rate. A planned application takes nothing from
 * the stock: only the real application does, the {@link FertilizerMovement}
 * linked here once the plan is done.
 *
 * <p>No cost is stored. The planned cost is computed on every read from the
 * price of the last purchase of the fertilizer, so it follows the prices.
 *
 * <p>Like an application, a plan belongs to a block of the farm referential
 * ({@code block_id}, foreign key {@code fk_fertilizer_plan_block}; plants picks
 * the block and never creates one) and needs no recorded planting. Its farm and
 * block code are read from the farm referential, as for a {@link FertilizerMovement}.
 */
@Entity
@EntityListeners(BlockLabelListener.class)
@Table(name = "fertilizer_plan", indexes = {
        @Index(name = "idx_fertilizer_plan_block_id", columnList = "block_id"),
        @Index(name = "idx_fertilizer_plan_product_id", columnList = "product_id"),
        @Index(name = "idx_fertilizer_plan_status_planned_date", columnList = "status, planned_date")})
@Getter
@Setter
@NoArgsConstructor
public class FertilizerPlan extends BaseEntity implements BlockRow {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private FertilizerProduct product;

    /** Block the fertilizer is planned on, a {@code farm.block} row. */
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final BlockLink block = new BlockLink();

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    /** Quantity planned, in the unit of the product. Always positive. */
    @Column(name = "planned_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal plannedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FertilizerPlanStatus status;

    /** The real application, once the plan is DONE; NULL otherwise. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movement_id")
    private FertilizerMovement movement;

    /** Free remark of the farmer, or NULL. */
    @Column(name = "note")
    private String note;

    /** Where the row comes from, for example {@code "user_entry"} for the plan routes. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /** Farm of the block, read from the farm referential. */
    public Long getFarmId() {
        return block.label(blockId).farmId();
    }

    /** Sets the farm by hand, on a row built in memory; a saved row takes it from its block. */
    public void setFarmId(Long farmId) {
        block.setFarmId(farmId);
    }

    /** Code of the block, read from the farm referential. */
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
        block.requireResolved("planned fertilizer application");
    }
}
