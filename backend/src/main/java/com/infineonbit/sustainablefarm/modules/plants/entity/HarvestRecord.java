package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

/**
 * A quantity of mangoes picked from a variety row on a given day.
 *
 * <p>Like {@link PopulationEvent}, the record holds no farm and no block: the
 * {@link Variety} row already stands for one (farm, block, variety) triple.
 * Several records may share a day, one per picking.
 *
 * <p>Each record gets a lot number when it is recorded (PL-05-05 "Batch
 * number"), so the modules downstream can trace what they receive: see
 * {@link #lotNumberOf}. The identifier stays the technical link.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "harvest_record",
        uniqueConstraints = @UniqueConstraint(name = HarvestRecord.LOT_NUMBER_CONSTRAINT, columnNames = "lot_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HarvestRecord {

    /** Name of the unique constraint on {@code lot_number}. */
    public static final String LOT_NUMBER_CONSTRAINT = "uk_harvest_record_lot_number";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Lot number given when the harvest is recorded, for example
     * {@code "LOT-2026-A-002"}: see {@link #lotNumberOf}. It is unique and
     * never changes. NULL on the harvests recorded before lot numbers existed.
     *
     * <p>30 characters hold the longest block code of a harvest (10) and any
     * sequence.
     */
    @Column(name = "lot_number", length = 30)
    private String lotNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variety_id", nullable = false)
    private Variety variety;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Column(name = "quantity_kg", nullable = false)
    private Double quantityKg;

    /** Where the record comes from, for example {@code "user_entry"} for the harvest route. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /**
     * Lot number of a harvest: {@code "LOT-"}, the year of the harvest date,
     * the block of its variety row, and the sequence of the lot among those of
     * that block and year, with at least three digits. For example
     * {@code "LOT-2026-A-002"} is the second lot given to block A for a harvest
     * of 2026.
     *
     * <p>A block code holds only letters and digits, so the hyphens separate
     * the parts without ambiguity: the lots of block A never share the prefix
     * of those of block A1.
     *
     * @param year      year of the harvest date
     * @param blockCode block code as stored, for example {@code "A"}
     * @param sequence  rank of the lot within its block and year, from 1
     * @return the lot number
     */
    public static String lotNumberOf(int year, String blockCode, int sequence) {
        return lotPrefixOf(year, blockCode) + String.format(Locale.ROOT, "%03d", sequence);
    }

    /**
     * Beginning shared by the lot numbers of a block for a year, hyphen
     * included, for example {@code "LOT-2026-A-"}: see {@link #lotNumberOf}.
     *
     * @param year      year of the harvest date
     * @param blockCode block code as stored, for example {@code "A"}
     * @return the prefix
     */
    public static String lotPrefixOf(int year, String blockCode) {
        return String.format(Locale.ROOT, "LOT-%04d-%s-", year, blockCode);
    }
}
