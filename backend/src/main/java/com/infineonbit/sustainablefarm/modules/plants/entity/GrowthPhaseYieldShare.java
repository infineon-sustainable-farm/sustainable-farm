package com.infineonbit.sustainablefarm.modules.plants.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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
public class GrowthPhaseYieldShare extends BaseEntity {

    /** Decimals kept of {@code yieldShare}, as the API accepts them. */
    public static final int SHARE_SCALE = 3;

    @Column(name = "growth_phase", nullable = false)
    private String growthPhase;

    /** From 0 (no harvest) to 1 (the full yield of the variety). */
    @Column(name = "yield_share", nullable = false, precision = 4, scale = SHARE_SCALE)
    private BigDecimal yieldShare;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /** A row with every column, {@code id} first ({@code null} for a new row); the audit timestamps are set on save. */
    public GrowthPhaseYieldShare(Long id,
            String growthPhase,
            BigDecimal yieldShare,
            String source,
            Instant lastUpdated) {
        setId(id);
        this.growthPhase = growthPhase;
        this.yieldShare = yieldShare;
        this.source = source;
        this.lastUpdated = lastUpdated;
    }
}
