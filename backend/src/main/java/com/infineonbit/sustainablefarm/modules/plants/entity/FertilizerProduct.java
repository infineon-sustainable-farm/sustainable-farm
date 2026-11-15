package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A fertilizer of the catalogue, such as "NPK 15-15-15" or "Compost".
 *
 * <p>The row holds no stock. The stock of a fertilizer is computed on every read
 * from its {@link FertilizerMovement} rows, as the number of trees of a variety
 * is computed from its population events.
 *
 * <p>The unique constraint compares names exactly. Two names that differ only by
 * case, accents or surrounding spaces are refused by the service, which compares
 * them in Java: accent folding in SQL would depend on the database.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "fertilizer_product",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fertilizer_product_name",
                columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FertilizerProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "fertilizer_type", nullable = false, length = 30)
    private FertilizerType fertilizerType;

    /** Nutrient content as written on the bag, for example {@code "15-15-15"} or {@code "46-0-0"}. */
    @Column(name = "composition", length = 100)
    private String composition;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 30)
    private FertilizerUnit unit;

    /**
     * Stock at or below which the fertilizer is flagged, in the unit of the
     * product. Entered by the user; no source sets it, so it has no default.
     */
    @Column(name = "reorder_threshold", precision = 12, scale = 3)
    private BigDecimal reorderThreshold;

    /** Where the row comes from, for example {@code "user_entry"} for the catalogue route. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
