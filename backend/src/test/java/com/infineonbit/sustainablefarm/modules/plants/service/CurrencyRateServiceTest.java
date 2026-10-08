package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.CurrencyRateRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.CurrencyRateResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.exception.CurrencyRateNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrencyRateServiceTest {

    private static final Instant ENTERED = Instant.parse("2026-10-01T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @InjectMocks
    private CurrencyRateService currencyRateService;

    private CurrencyRate recordedRate(String rate, String source) {
        CurrencyRate recorded = new CurrencyRate(1L, "EUR", "XOF", new BigDecimal(rate), source, ENTERED);
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF")).thenReturn(Optional.of(recorded));
        return recorded;
    }

    private void noRate() {
        when(currencyRateRepository.findByBaseCurrencyAndQuoteCurrency("EUR", "XOF")).thenReturn(Optional.empty());
    }

    private void insertAssignsId(long id) {
        when(currencyRateRepository.saveAndFlush(any(CurrencyRate.class))).thenAnswer(invocation -> {
            CurrencyRate rate = invocation.getArgument(0);
            rate.setId(id);
            return rate;
        });
    }

    private CurrencyRate insertedRate() {
        ArgumentCaptor<CurrencyRate> captor = ArgumentCaptor.forClass(CurrencyRate.class);
        verify(currencyRateRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private CurrencyRate replacedRate() {
        ArgumentCaptor<CurrencyRate> captor = ArgumentCaptor.forClass(CurrencyRate.class);
        verify(currencyRateRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void getEurToXof_shouldReturnTheRecordedRate() {
        // Arrange: the database keeps 6 decimals
        recordedRate("655.957000", "BCEAO_fixed_parity_1999");
        // Act & Assert
        assertEquals(new CurrencyRateResponse("EUR", "XOF", 655.957, "BCEAO_fixed_parity_1999", ENTERED),
                currencyRateService.getEurToXof());
    }

    @Test
    void getEurToXof_shouldThrowNotFound_whenNoRateIsRecorded() {
        // Arrange
        noRate();
        // Act & Assert
        CurrencyRateNotFoundException exception = assertThrows(CurrencyRateNotFoundException.class,
                () -> currencyRateService.getEurToXof());
        assertEquals("No EUR to XOF rate recorded yet", exception.getMessage());
    }

    @Test
    void saveEurToXof_shouldInsertTheFirstRate_withItsSourceTrimmed() {
        // Arrange
        noRate();
        insertAssignsId(1L);
        // Act
        CurrencyRateService.SavedRate saved = currencyRateService.saveEurToXof(
                new CurrencyRateRequest(655.957, " BCEAO_fixed_parity_1999 "), NOW);
        // Assert: the row, with the exact decimals of the rate
        CurrencyRate inserted = insertedRate();
        assertEquals("EUR", inserted.getBaseCurrency());
        assertEquals("XOF", inserted.getQuoteCurrency());
        assertEquals(new BigDecimal("655.957"), inserted.getRate());
        assertEquals("BCEAO_fixed_parity_1999", inserted.getSource());
        assertEquals(NOW, inserted.getLastUpdated());
        assertTrue(saved.created());
        assertEquals(new CurrencyRateResponse("EUR", "XOF", 655.957, "BCEAO_fixed_parity_1999", NOW), saved.rate());
    }

    @Test
    void saveEurToXof_shouldReplaceTheRecordedRate_inItsOwnRow() {
        // Arrange
        CurrencyRate recorded = recordedRate("655.957", "BCEAO_fixed_parity_1999");
        when(currencyRateRepository.save(recorded)).thenReturn(recorded);
        // Act
        CurrencyRateService.SavedRate saved = currencyRateService.saveEurToXof(
                new CurrencyRateRequest(656.0, "Ministry_notice_2027"), NOW);
        // Assert: the same row, rate, source and date changed together; no second row
        assertSame(recorded, replacedRate());
        assertEquals(1L, recorded.getId());
        assertEquals(0, new BigDecimal("656").compareTo(recorded.getRate()));
        assertEquals("Ministry_notice_2027", recorded.getSource());
        assertEquals(NOW, recorded.getLastUpdated());
        assertFalse(saved.created());
        assertEquals(new CurrencyRateResponse("EUR", "XOF", 656.0, "Ministry_notice_2027", NOW), saved.rate());
        verify(currencyRateRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveEurToXof_shouldRecordAUserEntry_whenTheSourceIsMissingOrBlank() {
        // Arrange
        CurrencyRate recorded = recordedRate("655.957", "BCEAO_fixed_parity_1999");
        when(currencyRateRepository.save(recorded)).thenReturn(recorded);
        // Act & Assert
        assertEquals("user_entry",
                currencyRateService.saveEurToXof(new CurrencyRateRequest(656.0, null), NOW).rate().source());
        assertEquals("user_entry",
                currencyRateService.saveEurToXof(new CurrencyRateRequest(656.0, "   "), NOW).rate().source());
    }

    @Test
    void saveEurToXof_shouldThrowConflict_whenAnotherFirstRateIsRecordedAtTheSameTime() {
        // Arrange: no rate when read, but the unique constraint refuses the insert
        noRate();
        when(currencyRateRepository.saveAndFlush(any(CurrencyRate.class)))
                .thenThrow(new DataIntegrityViolationException("uk_currency_rate_base_currency_quote_currency"));
        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> currencyRateService.saveEurToXof(new CurrencyRateRequest(655.957, null), NOW));
        assertEquals("Another EUR to XOF rate was being recorded at the same time. "
                + "Nothing was saved: please send the request again.", exception.getMessage());
    }
}
