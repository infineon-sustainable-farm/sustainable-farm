package com.infineonbit.sustainablefarm.modules.plants.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
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
 * Exchange rate between two currencies: one unit of the base currency is worth
 * {@code rate} units of the quote currency, for example 1 EUR = 655.957 XOF.
 *
 * <p>A reference row, sourced, that the user enters and replaces through
 * {@code PUT /api/plants/currency-rates/EUR/XOF}. The EUR-XOF parity has been
 * fixed since 1999, but a reform is announced: the rate is read from this table
 * on every conversion and never written in the code, except as the row of the
 * demo data set ({@code seed/demo-data.sql}) in the dev profile.
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
public class CurrencyRate extends BaseEntity {

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

    /** A row with every column, {@code id} first ({@code null} for a new row); the audit timestamps are set on save. */
    public CurrencyRate(Long id,
            String baseCurrency,
            String quoteCurrency,
            BigDecimal rate,
            String source,
            Instant lastUpdated) {
        setId(id);
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
        this.source = source;
        this.lastUpdated = lastUpdated;
    }
}
