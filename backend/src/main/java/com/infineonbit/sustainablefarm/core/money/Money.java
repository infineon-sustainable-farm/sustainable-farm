package com.infineonbit.sustainablefarm.core.money;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount of money in one currency, the shared form of every money field (ADR 0002).
 *
 * <p>The amount is always kept at scale 2, rounded to the nearest with halves rounded up
 * ({@link RoundingMode#HALF_UP}), as the costs of the plants module are: {@code 10.005 EUR}
 * becomes {@code 10.01 EUR}, {@code 1500 XOF} becomes {@code 1500.00 XOF}. Two amounts are
 * equal when their currency and their value at scale 2 are equal.
 *
 * <p>JPA embeddable mapped by default on the columns {@code amount numeric(14,2)} and
 * {@code currency varchar(3)}. An entity names its own columns {@code <prefix>_amount} and
 * {@code <prefix>_currency} with {@code @AttributeOverride}:
 *
 * <pre>{@code
 * @Embedded
 * @AttributeOverride(name = "amount", column = @Column(name = "price_amount", precision = 14, scale = 2))
 * @AttributeOverride(name = "currency", column = @Column(name = "price_currency", length = 3))
 * private Money price;
 * }</pre>
 *
 * <p>Amounts of different currencies are never added or compared: convert one first with
 * {@link CurrencyConversion}.
 *
 * @param amount   the value, at scale 2
 * @param currency the currency of the value
 */
@Embeddable
public record Money(
        @Column(name = "amount", precision = 14, scale = 2)
        BigDecimal amount,

        @Enumerated(EnumType.STRING)
        @Column(name = "currency", length = 3)
        CurrencyCode currency) {

    /** Scale of every stored amount, that of {@code numeric(14,2)}. */
    public static final int SCALE = 2;

    /**
     * @throws NullPointerException if the amount or the currency is missing
     */
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** An amount, rounded to scale 2. */
    public static Money of(BigDecimal amount, CurrencyCode currency) {
        return new Money(amount, currency);
    }

    /** An amount written in decimal, for example {@code Money.of("655.96", EUR)}. */
    public static Money of(String amount, CurrencyCode currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    /** Zero in a currency. */
    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * The sum of two amounts of the same currency.
     *
     * @throws IllegalArgumentException if the currencies differ
     */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * The difference of two amounts of the same currency.
     *
     * @throws IllegalArgumentException if the currencies differ
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /** This amount times a factor (a quantity, a share), rounded to scale 2. */
    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor), currency);
    }

    /** This amount rounded to the minor unit of its currency: to the franc for XOF, to the cent for EUR. */
    public Money roundToMinorUnit() {
        return new Money(currency.round(amount), currency);
    }

    /** {@code true} when the value is zero. */
    public boolean isZero() {
        return amount.signum() == 0;
    }

    /** {@code true} when the value is below zero. */
    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }

    private void requireSameCurrency(Money other) {
        if (other.currency != currency) {
            throw new IllegalArgumentException(
                    "Cannot combine " + currency + " and " + other.currency + " amounts: convert one first");
        }
    }
}
