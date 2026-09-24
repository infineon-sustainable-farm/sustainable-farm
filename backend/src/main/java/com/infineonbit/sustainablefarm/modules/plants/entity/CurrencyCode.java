package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Currencies in which a fertilizer purchase can be paid: the CFA franc of the
 * farm (XOF) and the euro.
 *
 * <p>Unlike the other Plants enums, a currency is stored as plain text, without
 * a CHECK constraint, in {@code fertilizer_movement.currency} and in
 * {@code currency_rate}. The codes are checked by the API. A reform such as the
 * announced Eco then needs a new value here and in the request pattern, not a
 * database migration.
 */
public enum CurrencyCode {
    XOF,
    EUR
}
