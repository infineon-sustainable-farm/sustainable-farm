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
 * The maturity of the fruit of a variety row as seen in the field on a given
 * day (PL-05-01, "Manual entry"), with how it was seen and by whom.
 *
 * <p>Like {@link HarvestRecord}, the row holds no farm and no block: the
 * {@link Variety} row already stands for one (farm, block, variety) triple.
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
@Table(name = "maturity_observation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaturityObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variety_id", nullable = false)
    private Variety variety;

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
}
