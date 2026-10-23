package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
@Table(name = "nursery_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NurseryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id")
    private Integer farmId;

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

    /** Block the batch is meant for, stored like the block of a planting, or {@code null}. */
    @Column(name = "planned_block_code")
    private String plannedBlockCode;

    /** Where the row comes from, for example {@code "user_entry"} for the batch route. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
