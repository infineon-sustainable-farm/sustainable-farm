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
 * A plant protection treatment of a block, one line of the treatment record
 * GLOBALG.A.P. asks for: block, date, applicator, the problem it targets as its
 * justification, the trade name and active ingredient of the product, the
 * pre-harvest interval, the quantity and the equipment.
 *
 * <p>A treatment answers a {@link HealthFinding}, or is preventive and has
 * none. When it answers a finding, the farm, the block, the targeted issue and
 * its label are copied from the finding; the copy is safe, since neither side
 * ever changes after it is written. {@code targetOtherLabel} names the problem
 * when the targeted issue is "Other", as for sulfur against powdery mildew, so
 * that every treatment can be recorded.
 *
 * <p>The date from which the block can be harvested is computed on every read
 * as {@code treatedOn} plus {@code preHarvestIntervalDays}, never stored. No
 * harvest is refused because of it. There is no stock of plant protection
 * products.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@EntityListeners(BlockLabelListener.class)
@Table(name = "health_treatment", indexes = {
                @Index(name = "idx_health_treatment_block_id", columnList = "block_id"),
                @Index(name = "idx_health_treatment_target_issue_id", columnList = "target_issue_id"),
                @Index(name = "idx_health_treatment_finding_id", columnList = "finding_id"),
                @Index(name = "idx_health_treatment_equipment_id", columnList = "equipment_id")})
@Getter
@Setter
@NoArgsConstructor
public class HealthTreatment extends BaseEntity implements BlockRow {

    /** Treated block, a {@code farm.block} row (foreign key {@code fk_health_treatment_block}). */
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final BlockLink block = new BlockLink();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_issue_id", nullable = false)
    private HealthIssueReference targetIssue;

    @Column(name = "target_other_label", length = 60)
    private String targetOtherLabel;

    /** The finding the treatment answers, or {@code null} for a preventive treatment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id")
    private HealthFinding finding;

    @Column(name = "treated_on", nullable = false)
    private LocalDate treatedOn;

    /** Trade name of the product, for example {@code "Copper fungicide A"}. */
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "active_ingredient", nullable = false)
    private String activeIngredient;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 30)
    private TreatmentUnit unit;

    /** Days to wait after the treatment before harvesting, 0 or more. */
    @Column(name = "pre_harvest_interval_days", nullable = false)
    private Integer preHarvestIntervalDays;

    @Column(name = "applicator", nullable = false)
    private String applicator;

    /**
     * The machine used, an {@code equipment} row of the machinery register (foreign key
     * {@code fk_health_treatment_equipment}, emptied if machinery deletes the machine), or
     * {@code null}: none given, or a treatment recorded before the machines were picked.
     */
    @Column(name = "equipment_id")
    private Long equipmentId;

    /**
     * Name of the machine as machinery had it when the treatment was recorded, kept with the
     * treatment record; free text on the treatments recorded before the machines were picked.
     */
    @Column(name = "equipment")
    private String equipment;

    /** Where the row comes from, for example {@code "user_entry"} for the treatment routes. */
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
        block.requireResolved("health treatment");
    }
}
