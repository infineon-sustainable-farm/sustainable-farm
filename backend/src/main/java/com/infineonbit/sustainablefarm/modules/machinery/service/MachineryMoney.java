package com.infineonbit.sustainablefarm.modules.machinery.service;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.Money;

import java.math.BigDecimal;

/**
 * The currency of every machinery cost (fuel, repairs, spare parts) and its conversion from and
 * to the plain numbers of the API.
 *
 * <p>Machinery has always entered and shown its costs in US dollars: they are stored as
 * {@link Money} in {@link CurrencyCode#USD}, and the API keeps giving and taking the amount alone,
 * as a JSON number.
 */
public final class MachineryMoney {

    /** The currency of the machinery costs. */
    public static final CurrencyCode CURRENCY = CurrencyCode.USD;

    private MachineryMoney() {
    }

    /** An amount of the API as a cost, or {@code null} for no amount. */
    public static Money of(BigDecimal amount) {
        return amount == null ? null : Money.of(amount, CURRENCY);
    }

    /** The amount of a cost for the API, or {@code null} for no cost. */
    public static BigDecimal amountOf(Money cost) {
        return cost == null ? null : cost.amount();
    }
}
