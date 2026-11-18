package com.infineonbit.sustainablefarm.core.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void keepsTheAmountAtScaleTwo() {
        assertEquals(new BigDecimal("1500.00"), Money.of("1500", CurrencyCode.XOF).amount());
        assertEquals(new BigDecimal("12.30"), Money.of("12.3", CurrencyCode.EUR).amount());
        assertEquals(2, Money.zero(CurrencyCode.EUR).amount().scale());
    }

    @Test
    void roundsHalvesUpAsThePlantsCostsDo() {
        assertEquals(new BigDecimal("10.01"), Money.of("10.005", CurrencyCode.EUR).amount());
        assertEquals(new BigDecimal("10.00"), Money.of("10.004", CurrencyCode.EUR).amount());
        assertEquals(new BigDecimal("-10.01"), Money.of("-10.005", CurrencyCode.EUR).amount());
        assertEquals(new BigDecimal("3.34"), Money.of("1.67", CurrencyCode.EUR).multiply(new BigDecimal("2.0001")).amount());
    }

    @Test
    void roundsToTheMinorUnitOfItsCurrency() {
        assertEquals(new BigDecimal("78715.00"), Money.of("78714.84", CurrencyCode.XOF).roundToMinorUnit().amount());
        assertEquals(new BigDecimal("78715.00"), Money.of("78714.50", CurrencyCode.XOF).roundToMinorUnit().amount());
        assertEquals(new BigDecimal("228.67"), Money.of("228.67", CurrencyCode.EUR).roundToMinorUnit().amount());
        assertEquals(new BigDecimal("1313"), CurrencyCode.XOF.round(new BigDecimal("1312.5")));
        assertEquals(new BigDecimal("0.13"), CurrencyCode.EUR.round(new BigDecimal("0.125")));
    }

    @Test
    void isEqualByCurrencyAndValueWhateverTheScaleGiven() {
        assertEquals(Money.of("120", CurrencyCode.EUR), Money.of(new BigDecimal("120.000"), CurrencyCode.EUR));
        assertEquals(Money.of("120", CurrencyCode.EUR).hashCode(), Money.of("120.00", CurrencyCode.EUR).hashCode());
        assertNotEquals(Money.of("120", CurrencyCode.EUR), Money.of("120", CurrencyCode.XOF));
        assertNotEquals(Money.of("120", CurrencyCode.EUR), Money.of("120.01", CurrencyCode.EUR));
    }

    @Test
    void addsAndSubtractsAmountsOfTheSameCurrency() {
        Money total = Money.of("100.10", CurrencyCode.EUR).add(Money.of("0.95", CurrencyCode.EUR));
        assertEquals(Money.of("101.05", CurrencyCode.EUR), total);
        assertEquals(Money.of("-0.85", CurrencyCode.XOF),
                Money.of("0.10", CurrencyCode.XOF).subtract(Money.of("0.95", CurrencyCode.XOF)));
        assertTrue(Money.zero(CurrencyCode.XOF).isZero());
        assertTrue(Money.of("-1", CurrencyCode.XOF).isNegative());
        assertFalse(Money.of("1", CurrencyCode.XOF).isNegative());
    }

    @Test
    void refusesToCombineTwoCurrencies() {
        Money eur = Money.of("10", CurrencyCode.EUR);
        Money xof = Money.of("10", CurrencyCode.XOF);
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> eur.add(xof));
        assertEquals("Cannot combine EUR and XOF amounts: convert one first", refused.getMessage());
        assertThrows(IllegalArgumentException.class, () -> eur.subtract(xof));
    }

    @Test
    void needsAnAmountAndACurrency() {
        assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null, CurrencyCode.EUR));
        assertThrows(NullPointerException.class, () -> Money.of("1", null));
    }

    @Test
    void keepsTheStoredCurrencyNames() {
        assertEquals("XOF", CurrencyCode.XOF.name());
        assertEquals("EUR", CurrencyCode.EUR.name());
        assertEquals(0, CurrencyCode.XOF.fractionDigits());
        assertEquals(2, CurrencyCode.EUR.fractionDigits());
        assertEquals(2, CurrencyCode.USD.fractionDigits());
        assertEquals("1500.00 XOF", Money.of("1500", CurrencyCode.XOF).toString());
    }
}
