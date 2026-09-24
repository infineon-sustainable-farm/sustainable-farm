package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Loads the default exchange rates at startup, so the cost of a purchase can
 * be given in FCFA and in euros.
 *
 * <p>Runs in every profile, like {@link AgronomicReferenceLoader} and unlike
 * {@code PlantsDataSeeder}: a rate belongs to no farm, and every environment,
 * production included, needs it to convert a cost.
 *
 * <p>A rate is inserted only when its currency pair is missing. An existing row
 * is never overwritten, even when its value differs from the default below: a
 * rate changed in the database, after a reform for example, is the one that
 * counts. Restarting the application therefore never duplicates nor resets a rate.
 *
 * <p>The list below is the only place in the code where a rate appears. The
 * conversions read it from the table.
 */
@Component
public class CurrencyRateLoader implements CommandLineRunner {

    /** Default rate of a currency pair, with its source. */
    private record RateDefault(CurrencyCode baseCurrency, CurrencyCode quoteCurrency, BigDecimal rate,
                               String source) {
    }

    /** The fixed parity of the CFA franc to the euro, set by the BCEAO in 1999. */
    private static final List<RateDefault> RATE_DEFAULTS = List.of(
            new RateDefault(CurrencyCode.EUR, CurrencyCode.XOF, new BigDecimal("655.957"),
                    "BCEAO_fixed_parity_1999"));

    private final CurrencyRateRepository currencyRateRepository;

    public CurrencyRateLoader(CurrencyRateRepository currencyRateRepository) {
        this.currencyRateRepository = currencyRateRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        load(Instant.now());
    }

    /**
     * Inserts the missing default rates, at an explicit write time so the
     * {@code lastUpdated} values can be tested.
     *
     * @param now insertion time, stored as {@code lastUpdated} of each inserted row
     */
    void load(Instant now) {
        for (RateDefault rate : RATE_DEFAULTS) {
            String base = rate.baseCurrency().name();
            String quote = rate.quoteCurrency().name();
            if (currencyRateRepository.findByBaseCurrencyAndQuoteCurrency(base, quote).isEmpty()) {
                currencyRateRepository.save(new CurrencyRate(null, base, quote, rate.rate(), rate.source(), now));
            }
        }
    }
}
