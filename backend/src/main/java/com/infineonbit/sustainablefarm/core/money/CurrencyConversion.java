package com.infineonbit.sustainablefarm.core.money;

import java.time.LocalDate;

/**
 * Converts an amount of money to another currency, at the rate recorded for a date.
 *
 * <p>Port of {@code core}: a module that needs a conversion injects this interface and depends
 * on no other module. The implementation is a Spring bean of the module that owns the exchange
 * rates, today {@code plants} (table {@code currency_rate}, ADR 0004).
 *
 * <p>The converted amount is rounded to the minor unit of the target currency, halves rounded
 * up: to the franc for XOF, to the cent for EUR.
 */
public interface CurrencyConversion {

    /**
     * The amount in the target currency.
     *
     * @param money  the amount to convert
     * @param target the currency asked for
     * @param date   the date of the amount, which selects the rate when the owner keeps a history
     * @return {@code money} itself when it is already in {@code target}, otherwise the converted
     *         amount, rounded to the minor unit of {@code target}
     * @throws ExchangeRateNotFoundException if no rate is recorded between the two currencies
     */
    Money convert(Money money, CurrencyCode target, LocalDate date);
}
