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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A quantity of mangoes picked from a variety row on a given day.
 *
 * <p>Like {@link PopulationEvent}, the record holds no farm and no block: the
 * {@link Variety} row already stands for one (farm, block, variety) triple.
 * Several records may share a day, one per picking.
 *
 * <p>There is no lot identifier yet: its format is to be decided with Crop
 * Storage, and adding the column later changes nothing here.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "harvest_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HarvestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
