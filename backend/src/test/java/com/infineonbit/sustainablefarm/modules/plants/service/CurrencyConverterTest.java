package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CurrencyConverterTest {

    private static final BigDecimal PARITY = new BigDecimal("655.957");

    @Test
    void conversions_shouldGiveOnlyTheAmountInItsOwnCurrency_whenNoRateIsRecorded() {
        // Act & Assert: without a rate, a cost in FCFA has no amount in euros, and the reverse
        assertEquals(150000L, CurrencyConverter.toXof(new BigDecimal("150000"), CurrencyCode.XOF, null));
        assertNull(CurrencyConverter.toEur(new BigDecimal("150000"), CurrencyCode.XOF, null));
        assertEquals(new BigDecimal("120.00"), CurrencyConverter.toEur(new BigDecimal("120"), CurrencyCode.EUR, null));
        assertNull(CurrencyConverter.toXof(new BigDecimal("120"), CurrencyCode.EUR, null));
    }

    @Test
    void toEur_shouldDivideAnXofAmountByTheRate_roundedToTheCent() {
        // Act & Assert: 150000 / 655.957 = 228.6735...
        assertEquals(new BigDecimal("228.67"),
                CurrencyConverter.toEur(new BigDecimal("150000"), CurrencyCode.XOF, PARITY));
    }

    @Test
    void toXof_shouldMultiplyAnEurAmountByTheRate_roundedToTheFranc() {
        // Act & Assert: 120 x 655.957 = 78714.84
        assertEquals(78715L, CurrencyConverter.toXof(new BigDecimal("120"), CurrencyCode.EUR, PARITY));
    }

    @Test
    void conversions_shouldKeepAnAmountInItsOwnCurrency() {
        // Act & Assert
        assertEquals(150000L, CurrencyConverter.toXof(new BigDecimal("150000.00"), CurrencyCode.XOF, PARITY));
        assertEquals(new BigDecimal("120.00"), CurrencyConverter.toEur(new BigDecimal("120"), CurrencyCode.EUR, PARITY));
    }

    @Test
    void conversions_shouldFollowTheRateGiven() {
        // Arrange: the rate after an edit of currency_rate
        BigDecimal edited = new BigDecimal("656");
        // Act & Assert: 150000 / 656 = 228.6585..., 120 x 656 = 78720
        assertEquals(new BigDecimal("228.66"), CurrencyConverter.toEur(new BigDecimal("150000"), CurrencyCode.XOF, edited));
        assertEquals(78720L, CurrencyConverter.toXof(new BigDecimal("120"), CurrencyCode.EUR, edited));
    }

    @Test
    void conversions_shouldRoundHalvesUp() {
        // Act & Assert: 5 / 1000 = 0.005 gives 0.01; 0.5 x 1 = 0.5 gives 1
        assertEquals(new BigDecimal("0.01"), CurrencyConverter.toEur(new BigDecimal("5"), CurrencyCode.XOF, new BigDecimal("1000")));
        assertEquals(1L, CurrencyConverter.toXof(new BigDecimal("0.5"), CurrencyCode.EUR, BigDecimal.ONE));
        assertEquals(101L, CurrencyConverter.toXof(new BigDecimal("100.50"), CurrencyCode.XOF, PARITY));
    }
}
