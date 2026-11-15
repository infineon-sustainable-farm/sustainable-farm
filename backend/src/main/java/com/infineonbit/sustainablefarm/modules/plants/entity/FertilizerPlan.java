package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * <p>Like an application, a plan belongs to a block and needs no recorded
 * planting. The table and its columns follow the naming of the other modules:
 * English snake_case and a singular table name.
 */
@Entity
@Table(name = "fertilizer_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private FertilizerProduct product;

    /** Farm of the block, or NULL. */
    @Column(name = "farm_id")
    private Integer farmId;

    /** Block the fertilizer is planned on, trimmed and upper-cased. */
    @Column(name = "block_code", nullable = false)
    private String blockCode;

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
}
