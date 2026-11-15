package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.CurrencyRateRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.CurrencyRateResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.exception.CurrencyRateNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * The EUR to XOF rate that converts the cost of a fertilizer purchase, as the
 * user enters it.
 *
 * <p>Outside the dev profile no rate is loaded at startup: the first one comes
 * from {@link #saveEurToXof(CurrencyRateRequest)}, and each later call replaces
 * it in the same row. The conversions read the table on every call, so a new
 * rate counts at once, for the purchases already recorded too.
 */
@Service
@AllArgsConstructor
public class CurrencyRateService {

    /** {@code source} of a rate entered without one. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private static final String EUR = CurrencyCode.EUR.name();
    private static final String XOF = CurrencyCode.XOF.name();

    private final CurrencyRateRepository currencyRateRepository;

    /**
     * A rate saved through the API.
     *
     * @param rate    the rate as now recorded
     * @param created {@code true} when it is the first rate of its pair,
     *                {@code false} when it replaced the one recorded
     */
    public record SavedRate(CurrencyRateResponse rate, boolean created) {
    }

    static CurrencyRateResponse toResponse(CurrencyRate rate) {
        return new CurrencyRateResponse(
                rate.getBaseCurrency(),
                rate.getQuoteCurrency(),
                rate.getRate().doubleValue(),
                rate.getSource(),
                rate.getLastUpdated());
    }

    /**
     * Retrieves how many FCFA one euro is worth.
     *
     * @return the rate, with its source
     * @throws CurrencyRateNotFoundException if no rate is recorded yet
     */
    public CurrencyRateResponse getEurToXof() {
        return currencyRateRepository.findByBaseCurrencyAndQuoteCurrency(EUR, XOF)
                .map(CurrencyRateService::toResponse)
                .orElseThrow(() -> new CurrencyRateNotFoundException(CurrencyCode.EUR, CurrencyCode.XOF));
    }

    /**
     * Enters the EUR to XOF rate, or replaces the one recorded.
     *
     * <p>There is one row per currency pair: a replacement updates it, so the
     * rate, its source and its {@code lastUpdated} change together. A missing or
     * blank source is recorded as {@code user_entry}.
     *
     * @param request the rate, already validated
     * @return the rate as now recorded, and whether it is the first one
     * @throws ConflictException if another first rate was being recorded at the
     *                           same time; nothing is saved
     */
    @Transactional
    public SavedRate saveEurToXof(CurrencyRateRequest request) {
        return saveEurToXof(request, Instant.now());
    }

    /**
     * Same as {@link #saveEurToXof(CurrencyRateRequest)}, at an explicit write
     * time so the {@code lastUpdated} value can be tested.
     */
    SavedRate saveEurToXof(CurrencyRateRequest request, Instant now) {
        // At most 6 decimals, checked by the request, so the decimal form is exact: 655.957 stays 655.957.
        BigDecimal rate = BigDecimal.valueOf(request.rate());
        String source = request.source() == null || request.source().isBlank()
                ? USER_ENTRY_SOURCE
                : request.source().trim();

        Optional<CurrencyRate> recorded = currencyRateRepository.findByBaseCurrencyAndQuoteCurrency(EUR, XOF);
        if (recorded.isPresent()) {
            CurrencyRate current = recorded.get();
            current.setRate(rate);
            current.setSource(source);
            current.setLastUpdated(now);
            return new SavedRate(toResponse(currencyRateRepository.save(current)), false);
        }
        try {
            // Flushed here, so the unique constraint on the pair answers inside this method.
            CurrencyRate first = currencyRateRepository.saveAndFlush(
                    new CurrencyRate(null, EUR, XOF, rate, source, now));
            return new SavedRate(toResponse(first), true);
        } catch (DataIntegrityViolationException concurrentFirstRate) {
            throw new ConflictException("Another EUR to XOF rate was being recorded at the same time. "
                    + "Nothing was saved: please send the request again.");
        }
    }
}
