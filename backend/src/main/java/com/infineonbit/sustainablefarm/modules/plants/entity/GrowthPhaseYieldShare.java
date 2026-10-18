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
 * Share of the full-production yield that a tree gives in one growth phase.
 *
 * <p>Part of the agronomic reference, like {@link VarietyReference}: attached
 * to no farm, and sourced. {@code growthPhase} holds the exact labels computed
 * from the tree age ("establishment", "gradual production", "full production").
 *
 * <p>A row is a correction: every phase has a default share in code, and a
 * row replaces it. A phase without a row keeps its default, so the table may
 * be empty.
 */
@Entity
@Table(name = "growth_phase_yield_share",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_growth_phase_yield_share_growth_phase",
                columnNames = "growth_phase"),
        check = @CheckConstraint(
                name = "ck_growth_phase_yield_share_yield_share",
                constraint = "yield_share BETWEEN 0 AND 1"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrowthPhaseYieldShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "growth_phase", nullable = false)
    private String growthPhase;

    /** From 0 (no harvest) to 1 (the full yield of the variety). */
    @Column(name = "yield_share", nullable = false)
    private Double yieldShare;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
