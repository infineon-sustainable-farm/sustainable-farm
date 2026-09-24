package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversion of a cost between FCFA and euros, with no rate of its own.
 *
 * <p>The rate comes in as a parameter, read from {@code currency_rate} by the
 * caller. Amounts in FCFA are rounded to the franc, amounts in euros to the
 * cent, both to the nearest with halves rounded up.
 */
final class CurrencyConverter {

    private CurrencyConverter() {
    }

    /**
     * An amount in FCFA.
     *
     * @param amount   the amount, in {@code currency}
     * @param currency the currency of the amount
     * @param eurToXof how many FCFA one euro is worth
     * @return the amount in FCFA, rounded to the franc
     */
    static long toXof(BigDecimal amount, CurrencyCode currency, BigDecimal eurToXof) {
        BigDecimal xof = switch (currency) {
            case XOF -> amount;
            case EUR -> amount.multiply(eurToXof);
        };
        return xof.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * An amount in euros.
     *
     * @param amount   the amount, in {@code currency}
     * @param currency the currency of the amount
     * @param eurToXof how many FCFA one euro is worth
     * @return the amount in euros, rounded to the cent
     */
    static BigDecimal toEur(BigDecimal amount, CurrencyCode currency, BigDecimal eurToXof) {
        return switch (currency) {
            case EUR -> amount.setScale(2, RoundingMode.HALF_UP);
            case XOF -> amount.divide(eurToXof, 2, RoundingMode.HALF_UP);
        };
    }
}
