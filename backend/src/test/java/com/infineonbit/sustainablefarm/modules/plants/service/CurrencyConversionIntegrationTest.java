package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.CurrencyConversion;
import com.infineonbit.sustainablefarm.core.money.ExchangeRateNotFoundException;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.CurrencyRateRequest;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.aop.support.AopUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The core {@link CurrencyConversion} port, as another module receives it: a single bean,
 * implemented by plants on the rate the user enters.
 */
@SpringBootTest
@ActiveProfiles("test")
class CurrencyConversionIntegrationTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 17);

    @Autowired
    private CurrencyConversion currencyConversion;

    @Autowired
    private CurrencyRateService currencyRateService;

    @Autowired
    private CurrencyRateRepository currencyRateRepository;

    @BeforeEach
    @AfterEach
    void removeTheRate() {
        currencyRateRepository.deleteAll();
    }

    @Test
    void isImplementedByThePlantsRates() {
        assertEquals(CurrencyRateConversion.class, AopUtils.getTargetClass(currencyConversion));
    }

    @Test
    void convertsWithTheRateEnteredThroughPlants() {
        assertThrows(ExchangeRateNotFoundException.class,
                () -> currencyConversion.convert(Money.of("120", CurrencyCode.EUR), CurrencyCode.XOF, DAY));

        currencyRateService.saveEurToXof(new CurrencyRateRequest(655.957, null));

        assertEquals(Money.of("78715", CurrencyCode.XOF),
                currencyConversion.convert(Money.of("120", CurrencyCode.EUR), CurrencyCode.XOF, DAY));
        assertEquals(Money.of("228.67", CurrencyCode.EUR),
                currencyConversion.convert(Money.of("150000", CurrencyCode.XOF), CurrencyCode.EUR, DAY));
    }
}
