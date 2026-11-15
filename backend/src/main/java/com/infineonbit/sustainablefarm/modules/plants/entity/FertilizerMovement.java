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
 * A dated change in the stock of a {@link FertilizerProduct}: a purchase, an
 * application on a block, a loss, or an adjustment after a count.
 *
 * <p>The movement is stored, never the balance. The stock of a fertilizer is
 * computed on every read from its movements (see
 * {@link com.infineonbit.sustainablefarm.modules.plants.repository.FertilizerMovementRepository}).
 *
 * <p>One table holds every type, and the columns of the other types stay NULL:
 * <ul>
 *     <li>an APPLICATION has a block and an applicator, and optionally a farm
 *         and a method, as GLOBALG.A.P. asks for each application;</li>
 *     <li>a PURCHASE has a supplier, and optionally a total cost with its currency;</li>
 *     <li>a LOSS has a reason.</li>
 * </ul>
 * These rules are enforced by the service, not by the database.
 *
 * <p>An application belongs to a block, not to a planted variety: the base
 * fertilizer goes into the planting hole before or while the tree is planted.
 *
 * <p>Quantities and costs are exact decimals. A sum of floating-point values
 * drifts on PostgreSQL (0.3 - 0.1 gives 0.19999999999999998) and would refuse a
 * legitimate application.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "fertilizer_movement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private FertilizerProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private FertilizerMovementType movementType;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    /**
     * Quantity moved, in the unit of the product. Always positive: the movement
     * type says whether it enters or leaves the stock.
     */
    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    /** Farm of an application, or NULL. */
    @Column(name = "farm_id")
    private Integer farmId;

    /** Block of an application, trimmed and upper-cased; NULL for the other types. */
    @Column(name = "block_code")
    private String blockCode;

    /** Person or team who applied the fertilizer; NULL for the other types. */
    @Column(name = "applicator")
    private String applicator;

    /** How an application was made, for example "around the tree base", or NULL. */
    @Column(name = "method")
    private String method;

    /** Seller of a purchase; NULL for the other types. */
    @Column(name = "supplier")
    private String supplier;

    /** Total price of a purchase in {@link #currency}, or NULL when it was not entered. */
    @Column(name = "total_cost", precision = 14, scale = 2)
    private BigDecimal totalCost;

    /**
     * ISO 4217 code of {@link #totalCost}, NULL when there is no cost. Stored as
     * plain text without a CHECK constraint: the codes are checked by the API, and
     * a new currency, such as the announced Eco, must not need a database migration.
     */
    @Column(name = "currency", length = 3)
    private String currency;

    /** Why a quantity was lost, for example "expired"; NULL for the other types. */
    @Column(name = "reason")
    private String reason;

    /** Where the movement comes from, for example {@code "user_entry"} for the movement routes. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
