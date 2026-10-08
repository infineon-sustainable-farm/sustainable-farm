package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
public class CurrencyRateRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private CurrencyRateRepository currencyRateRepository;

    @Test
    void saveAndFlush_shouldRefuseASecondRowForTheSamePair() {
        // Arrange
        currencyRateRepository.saveAndFlush(
                new CurrencyRate(null, "EUR", "XOF", new BigDecimal("655.957"), "BCEAO_fixed_parity_1999", NOW));
        // Act & Assert: the constraint behind the 409 of two first rates sent at the same time
        assertThrows(DataIntegrityViolationException.class, () -> currencyRateRepository.saveAndFlush(
                new CurrencyRate(null, "EUR", "XOF", new BigDecimal("656"), "user_entry", NOW)));
    }
}
