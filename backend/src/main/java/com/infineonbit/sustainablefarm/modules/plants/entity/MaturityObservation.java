package com.infineonbit.sustainablefarm.modules.plants.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The maturity of the fruit of a variety row as seen in the field on a given
 * day (PL-05-01, "Manual entry"), with how it was seen and by whom.
 *
 * <p>Like {@link HarvestRecord}, the row holds no farm and no block: the
 * {@link BlockVariety} row already stands for one (block, variety) pair, the block giving the farm.
 * Several observations may share a day.
 *
 * <p>Only the observations are stored, never the status of a variety: the
 * harvest status reads them on every request, and the latest observation of
 * the season under way takes precedence over the estimate.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "maturity_observation",
        indexes = @Index(name = "idx_maturity_observation_block_variety_id", columnList = "block_variety_id"))
@Getter
@Setter
@NoArgsConstructor
public class MaturityObservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_variety_id", nullable = false)
    private BlockVariety variety;

    @Column(name = "observed_on", nullable = false)
    private LocalDate observedOn;

    /** The status the observer concluded; its CHECK constraint holds the three values of {@link MaturityStatus}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "maturity_status", nullable = false, length = 30)
    private MaturityStatus maturityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private MaturityMethod method;

    /**
     * Value read with the method, in its {@link MaturityMethod#unit() unit}:
     * °Bx for {@code BRIX}, percent for {@code DRY_MATTER}. NULL for the other
     * methods, and when no value was read.
     */
    @Column(name = "measured_value", precision = 4, scale = 1)
    private BigDecimal measuredValue;

    @Column(name = "observer")
    private String observer;

    /** Where the row comes from, for example {@code "user_entry"} for the observation route. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /** A row with every column, {@code id} first ({@code null} for a new row); the audit timestamps are set on save. */
    public MaturityObservation(Long id,
            BlockVariety variety,
            LocalDate observedOn,
            MaturityStatus maturityStatus,
            MaturityMethod method,
            BigDecimal measuredValue,
            String observer,
            String source,
            Instant lastUpdated) {
        setId(id);
        this.variety = variety;
        this.observedOn = observedOn;
        this.maturityStatus = maturityStatus;
        this.method = method;
        this.measuredValue = measuredValue;
        this.observer = observer;
        this.source = source;
        this.lastUpdated = lastUpdated;
    }
}
