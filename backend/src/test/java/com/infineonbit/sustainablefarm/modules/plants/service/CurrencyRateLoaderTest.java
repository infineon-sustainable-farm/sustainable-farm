package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The loader runs against a real database. It is built by hand, not imported as
 * a bean: a bean would already run at context startup, before the test.
 */
@DataJpaTest
@ActiveProfiles("test")
public class CurrencyRateLoaderTest {

    private static final Instant FIRST_START = Instant.parse("2026-09-24T10:00:00Z");
    private static final Instant SECOND_START = Instant.parse("2026-10-01T08:00:00Z");

    @Autowired
    private CurrencyRateRepository currencyRateRepository;

    private CurrencyRateLoader loader;

    @BeforeEach
    void setUp() {
        loader = new CurrencyRateLoader(currencyRateRepository);
    }

    private CurrencyRate eurToXof() {
        return currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF").orElseThrow();
    }

    @Test
    void load_shouldInsertTheFixedParity_whenTableIsEmpty() {
        // Act
        loader.load(FIRST_START);
        // Assert
        List<CurrencyRate> rates = currencyRateRepository.findAll();
        assertEquals(1, rates.size());
        CurrencyRate rate = rates.get(0);
        assertEquals("EUR", rate.getBaseCurrency());
        assertEquals("XOF", rate.getQuoteCurrency());
        assertEquals(0, new BigDecimal("655.957").compareTo(rate.getRate()));
        assertEquals("BCEAO_fixed_parity_1999", rate.getSource());
        assertEquals(FIRST_START, rate.getLastUpdated());
    }

    @Test
    void load_shouldNotDuplicateTheRate_whenRunTwice() {
        // Act
        loader.load(FIRST_START);
        loader.load(SECOND_START);
        // Assert: one row, still dated from the first start
        assertEquals(1, currencyRateRepository.count());
        assertEquals(FIRST_START, eurToXof().getLastUpdated());
    }

    @Test
    void load_shouldKeepARateChangedInTheDatabase() {
        // Arrange: the rate is edited after a reform
        loader.load(FIRST_START);
        CurrencyRate rate = eurToXof();
        rate.setRate(new BigDecimal("656"));
        rate.setSource("manual_edit");
        currencyRateRepository.saveAndFlush(rate);
        // Act
        loader.load(SECOND_START);
        // Assert
        assertEquals(1, currencyRateRepository.count());
        CurrencyRate kept = eurToXof();
        assertEquals(0, new BigDecimal("656").compareTo(kept.getRate()));
        assertEquals("manual_edit", kept.getSource());
    }
}
