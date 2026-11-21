package com.infineonbit.sustainablefarm.core.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Currencies handled by the application: the CFA franc of the farm (XOF), the euro, and the US
 * dollar in which machinery has always entered its costs (no exchange rate is recorded for it).
 *
 * <p>Stored as its name ({@code "XOF"}, {@code "EUR"}): with {@code @Enumerated(EnumType.STRING)}
 * in a {@code varchar(3)} column, or as plain text without a CHECK constraint, as in
 * {@code fertilizer_movement.currency} and {@code currency_rate}. A reform such as the announced
 * Eco then needs a new value here, not a database migration.
 *
 * <p>Each currency knows its minor unit (ISO 4217): the franc has no subunit, the euro has the
 * cent, the dollar the cent. A converted or computed amount is rounded to that unit, to the nearest with halves
 * rounded up.
 */
public enum CurrencyCode {
    XOF(0),
    EUR(2),
    USD(2);

    private final int fractionDigits;

    CurrencyCode(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    /** Number of decimals of the minor unit: 0 for the franc, 2 for the euro and the dollar (cent). */
    public int fractionDigits() {
        return fractionDigits;
    }

    /**
     * An amount rounded to the minor unit of this currency, halves rounded up.
     *
     * @param amount the amount, in this currency
     * @return the amount at the scale {@link #fractionDigits()}
     */
    public BigDecimal round(BigDecimal amount) {
        return amount.setScale(fractionDigits, RoundingMode.HALF_UP);
    }
}
