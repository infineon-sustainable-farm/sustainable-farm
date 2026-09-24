package com.infineonbit.sustainablefarm.modules.plants.entity;

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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exchange rate between two currencies: one unit of the base currency is worth
 * {@code rate} units of the quote currency, for example 1 EUR = 655.957 XOF.
 *
 * <p>A reference row, sourced and editable in the database. The EUR-XOF parity
 * has been fixed since 1999, but a reform is announced: the rate is read from
 * this table on every conversion and never written in the code, except as the
 * default of {@code CurrencyRateLoader}.
 *
 * <p>The currency codes are plain text without a CHECK constraint, like
 * {@link FertilizerMovement#getCurrency()}: a new currency must not need a
 * database migration.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "currency_rate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_currency_rate_base_currency_quote_currency",
                columnNames = {"base_currency", "quote_currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ISO 4217 code of the currency converted from, for example {@code "EUR"}. */
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    /** ISO 4217 code of the currency converted to, for example {@code "XOF"}. */
    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    /** Units of the quote currency for one unit of the base currency. */
    @Column(name = "rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal rate;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
