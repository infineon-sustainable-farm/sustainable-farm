package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Agronomic reference of a mango variety: what one tree yields in full
 * production, and when the variety is harvested.
 *
 * <p>A catalogue row, attached to no farm and no block: it says what a variety
 * is worth in general, not what an orchard holds. Each value carries its source,
 * and the user enters and corrects it through
 * {@code /api/plants/variety-references}; the yield forecast reads it on every
 * request and keeps no copy of it in code.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "variety_reference",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_variety_reference_variety_name",
                columnNames = "variety_name"),
        check = {
                @CheckConstraint(
                        name = "ck_variety_reference_harvest_start_month",
                        constraint = "harvest_start_month BETWEEN 1 AND 12"),
                @CheckConstraint(
                        name = "ck_variety_reference_harvest_end_month",
                        constraint = "harvest_end_month BETWEEN 1 AND 12")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VarietyReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Variety name as written in the catalogue, for example {@code "Amelie"}. A
     * planted variety finds its row ignoring case, accents and surrounding spaces.
     */
    @Column(name = "variety_name", nullable = false)
    private String varietyName;

    /** Yield of one tree in full production, per year, in kg. */
    @Column(name = "yield_per_tree_kg", nullable = false)
    private Double yieldPerTreeKg;

    @Column(name = "yield_source", nullable = false)
    private String yieldSource;

    /** First month of the harvest season, from 1 (January) to 12. */
    @Column(name = "harvest_start_month", nullable = false)
    private Integer harvestStartMonth;

    /**
     * Last month of the harvest season, from 1 to 12. It is lower than the start
     * month when the season runs over the new year, for example November to February.
     */
    @Column(name = "harvest_end_month", nullable = false)
    private Integer harvestEndMonth;

    @Column(name = "season_source", nullable = false)
    private String seasonSource;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
