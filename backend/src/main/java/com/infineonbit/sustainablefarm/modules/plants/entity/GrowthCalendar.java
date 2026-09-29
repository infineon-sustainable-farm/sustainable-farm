package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
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
 * <p>One row per (farm, block): {@code blockKey} holds that pair and carries a
 * unique constraint, so two plantings of a new block sent at the same time
 * cannot both create a row.
 */
@Entity
@Table(name = "calendrier_croissance",
        uniqueConstraints = @UniqueConstraint(name = GrowthCalendar.KEY_CONSTRAINT, columnNames = "block_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrowthCalendar {

    /** Name of the unique constraint on {@code block_key}. */
    public static final String KEY_CONSTRAINT = "uk_calendrier_croissance_block_key";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_ferme")
    private Integer farmId;

    @Column(name = "bloc_parcelle", nullable = false)
    private String blockCode;

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
     * it existed, until the startup filling reaches them.
     *
     * <p>267 characters hold the longest farm (11) and block (255) with the
     * separator.
     */
    @Column(name = "block_key", length = 267)
    private String blockKey;

    /**
     * Natural key of a calendar row: the farm, 0 for a row without a farm (farm
     * identifiers start at 1), and the block, joined by {@code "|"}. For example
     * {@code "0|C"}. It follows the rule of the planting lookup
     * ({@code GrowthCalendarRepository.findByFarmAndBlock}): a NULL farm is a
     * value of its own, and the block is taken as stored.
     *
     * @param farmId    farm identifier, or {@code null} for no farm
     * @param blockCode block code as stored
     * @return the key, or {@code null} when the block is missing
     */
    public static String keyOf(Integer farmId, String blockCode) {
        if (blockCode == null) {
            return null;
        }
        return (farmId == null ? 0 : farmId) + "|" + blockCode;
    }

    @PrePersist
    @PreUpdate
    void refreshBlockKey() {
        blockKey = keyOf(farmId, blockCode);
    }
}
