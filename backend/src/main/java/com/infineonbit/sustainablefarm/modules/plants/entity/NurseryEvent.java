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

import java.time.Instant;
import java.time.LocalDate;

/**
 * A dated fact about a {@link NurseryBatch}: a change of stage, a loss or a
 * transplant.
 *
 * <p>The event is stored, never the balance. The columns filled depend on the
 * type, and the service keeps to it:
 * <ul>
 *     <li>STAGE_CHANGE: {@code stage};</li>
 *     <li>LOSS: {@code quantity} and {@code reason};</li>
 *     <li>TRANSPLANT: {@code quantity}, {@code blockCode} and
 *         {@code populationEvent}, the PLANTING the transplant created.</li>
 * </ul>
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@EntityListeners(BlockLabelListener.class)
@Table(name = "nursery_event", indexes = {
                @Index(name = "idx_nursery_event_block_id", columnList = "block_id"),
                @Index(name = "idx_nursery_event_batch_id", columnList = "batch_id"),
                @Index(name = "idx_nursery_event_population_event_id", columnList = "population_event_id")})
@Getter
@Setter
@NoArgsConstructor
public class NurseryEvent extends BaseEntity implements BlockRow {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private NurseryBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private NurseryEventType eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    /** The stage reached, for a stage change only. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30)
    private NurseryStage stage;

    /** Plants lost or transplanted, always positive; {@code null} for a stage change. */
    @Column(name = "quantity")
    private Integer quantity;

    /** Why the plants were lost, for a loss only. */
    @Column(name = "reason")
    private String reason;

    /** Block the plants went to, a {@code farm.block} row (foreign key {@code fk_nursery_event_block}), for a transplant only. */
    @Column(name = "block_id")
    private Long blockId;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final BlockLink block = new BlockLink();

    /** The PLANTING event the transplant created in the orchard, for a transplant only. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "population_event_id")
    private PopulationEvent populationEvent;

    /** Where the row comes from, for example {@code "user_entry"} for the nursery routes. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /** Code of the block the plants went to, read from the farm referential; {@code null} but for a transplant. */
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
        block.requireResolved("nursery event");
    }
}
