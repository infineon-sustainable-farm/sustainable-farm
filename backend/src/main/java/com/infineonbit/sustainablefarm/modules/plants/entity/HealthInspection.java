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
 * A visit of a block to check the health of its trees, with a health score and
 * the problems seen, stored as {@link HealthFinding} rows.
 *
 * <p>A visit with nothing found is recorded too: it is the trace that the block
 * was checked. The block needs no recorded planting, since termites can be seen
 * before the trees go in, as for a fertilizer application.
 *
 * <p>A block is the (farm, block) pair: a NULL farm is a farm of its own, as in
 * the planting lookup.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "health_inspection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id")
    private Integer farmId;

    /** Block code as stored, trimmed and upper-cased, for example {@code "C"}. */
    @Column(name = "block_code", nullable = false)
    private String blockCode;

    @Column(name = "inspected_on", nullable = false)
    private LocalDate inspectedOn;

    /** Health of the block seen during the visit, from 0 to 100. */
    @Column(name = "health_score_pct", nullable = false)
    private Integer healthScorePct;

    @Column(name = "observer")
    private String observer;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 30)
    private InspectionMethod method;

    /** Where the row comes from, for example {@code "user_entry"} for the inspection route. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
