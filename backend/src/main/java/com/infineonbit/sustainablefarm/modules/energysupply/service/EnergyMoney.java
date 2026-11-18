package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.Money;

import java.math.BigDecimal;

/**
 * The currency of every energy supply cost (component unit costs, generator runs) and its
 * conversion from and to the plain numbers of the API.
 *
 * <p>The Energy App has always entered and shown its costs in euros (the former {@code *_eur}
 * columns): they are stored as {@link Money} in {@link CurrencyCode#EUR}, and the API keeps
 * giving and taking the amount alone, as a JSON number under its {@code ...Eur} name.
 */
public final class EnergyMoney {

    /** The currency of the energy supply costs. */
    public static final CurrencyCode CURRENCY = CurrencyCode.EUR;

    private EnergyMoney() {
    }

    /** An amount of the API as a cost, or {@code null} for no amount. */
    public static Money of(BigDecimal amount) {
        return amount == null ? null : Money.of(amount, CURRENCY);
    }

    /** An amount written in decimal as a cost, for example {@code EnergyMoney.of("6.80")}. */
    public static Money of(String amount) {
        return Money.of(amount, CURRENCY);
    }

    /** The amount of a cost for the API, or {@code null} for no cost. */
    public static BigDecimal amountOf(Money cost) {
        return cost == null ? null : cost.amount();
    }
}
