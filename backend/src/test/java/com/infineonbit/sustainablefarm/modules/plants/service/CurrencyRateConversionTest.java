package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.ExchangeRateNotFoundException;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The core conversion port on the plants rates, with the rounding of {@link CurrencyConverter}. */
@ExtendWith(MockitoExtension.class)
class CurrencyRateConversionTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 17);

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @InjectMocks
    private CurrencyRateConversion conversion;

    private void recordParity() {
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF"))
                .thenReturn(Optional.of(new CurrencyRate(1L, "EUR", "XOF", new BigDecimal("655.957"),
                        "BCEAO_fixed_parity_1999", Instant.EPOCH)));
    }

    @Test
    void keepsAnAmountAlreadyInTheTargetCurrencyWithoutReadingARate() {
        Money eur = Money.of("120", CurrencyCode.EUR);
        assertSame(eur, conversion.convert(eur, CurrencyCode.EUR, DAY));
        verifyNoInteractions(currencyRateRepository);
    }

    @Test
    void convertsEurosToFrancsRoundedToTheFrancAsThePurchasesAre() {
        recordParity();
        Money xof = conversion.convert(Money.of("120", CurrencyCode.EUR), CurrencyCode.XOF, DAY);

        assertEquals(Money.of("78715", CurrencyCode.XOF), xof);
        assertEquals(CurrencyConverter.toXof(new BigDecimal("120"), CurrencyCode.EUR, new BigDecimal("655.957")),
                xof.amount().longValueExact());
    }

    @Test
    void convertsFrancsToEurosThroughTheInverseRateRoundedToTheCent() {
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("XOF", "EUR")).thenReturn(Optional.empty());
        recordParity();
        Money eur = conversion.convert(Money.of("150000", CurrencyCode.XOF), CurrencyCode.EUR, DAY);

        assertEquals(Money.of("228.67", CurrencyCode.EUR), eur);
        assertEquals(CurrencyConverter.toEur(new BigDecimal("150000"), CurrencyCode.XOF, new BigDecimal("655.957")),
                eur.amount());
    }

    @Test
    void refusesWithA404WhenNoRateIsRecorded() {
        ExchangeRateNotFoundException refused = assertThrows(ExchangeRateNotFoundException.class,
                () -> conversion.convert(Money.of("120", CurrencyCode.EUR), CurrencyCode.XOF, DAY));

        assertInstanceOf(ResourceNotFoundException.class, refused);
        assertEquals("No EUR to XOF rate recorded yet", refused.getMessage());
    }
}
