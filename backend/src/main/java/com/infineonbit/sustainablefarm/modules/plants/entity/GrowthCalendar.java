package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Planting calendar of a block of the farm.
 *
 * <p>Column names are the domain names of the schema and are mapped explicitly,
 * like {@link Variety}.
 *
 * <p>There is deliberately no tree-age column. The age of the trees is never
 * stored nor entered: it is computed from {@code plantingDate} and the
 * current date when the entry is read (see the service layer).
 *
 * <p>{@code currentStage} is a field observation, not the computed growth phase.
 * It stays NULL until someone records it.
 *
 * <p>One row per block: {@code blockKey} holds its id and carries a
 * unique constraint, so two plantings of a new block sent at the same time
 * cannot both create a row.
 */
@Entity
@EntityListeners(BlockLabelListener.class)
@Table(name = "calendrier_croissance",
        uniqueConstraints = @UniqueConstraint(name = GrowthCalendar.KEY_CONSTRAINT, columnNames = "block_key"),
        indexes = @Index(name = "idx_calendrier_croissance_block_id", columnList = "block_id"))
@Getter
@Setter
@NoArgsConstructor
public class GrowthCalendar implements BlockRow {

    /** Name of the unique constraint on {@code block_key}. */
    public static final String KEY_CONSTRAINT = "uk_calendrier_croissance_block_key";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Block of the calendar, a {@code farm.block} row (foreign key {@code fk_calendrier_croissance_block}). */
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final BlockLink block = new BlockLink();

    /** Not provided by the source study. Stays NULL until recorded. */
    @Column(name = "date_plantation")
    private LocalDate plantingDate;

    /** How precise the planting date is, in the words the source allows. */
    @Column(name = "precision_date")
    private String datePrecision;

    /** Observed growth stage. Not the computed phase. Stays NULL until observed. */
    @Column(name = "stade_actuel")
    private String currentStage;

    @Column(name = "phase_annees")
    private String phaseYears;

    /** Local rainfall for this block. The source only gives a farm-wide range. */
    @Column(name = "pluviometrie_locale_mm")
    private Double localRainfallMm;

    @Column(name = "source")
    private String source;

    @Column(name = "date_maj")
    private Instant lastUpdated;

    /**
     * Natural key of the row, written on every insert and update and never
     * exposed by the API. See {@link #keyOf}. NULL on the rows written before
     * it existed, until they are updated or, in the dev profile, the startup
     * filling reaches them.
     *
     * <p>20 characters hold the longest block id.
     */
    @Column(name = "block_key", length = 20)
    private String blockKey;

    /**
     * Natural key of a calendar row: the block id, for example {@code "3"}.
     * It follows the rule of the planting lookup
     * ({@code GrowthCalendarRepository.findByBlock}). The block gives the farm,
     * so the farm is not part of the key.
     *
     * @param blockId id of the block
     * @return the key, or {@code null} when the block is missing
     */
    public static String keyOf(Long blockId) {
        return blockId == null ? null : String.valueOf(blockId);
    }

    @PrePersist
    @PreUpdate
    void refreshBlockKey() {
        block.requireResolved("growth calendar row");
        blockKey = keyOf(blockId);
    }

    /** Farm of the block, read from the farm referential; {@code null} without a block. */
    public Long getFarmId() {
        return block.label(blockId).farmId();
    }

    /** Sets the farm by hand, on a row built in memory; a saved row takes it from its block. */
    public void setFarmId(Long farmId) {
        block.setFarmId(farmId);
    }

    /** Code of the block, for example {@code "A"}, read from the farm referential. */
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
}
