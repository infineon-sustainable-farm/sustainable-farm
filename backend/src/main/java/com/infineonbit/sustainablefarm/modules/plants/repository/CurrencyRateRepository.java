package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {

    /**
     * The rate of one currency pair, unique by construction of the table.
     *
     * @param baseCurrency  code of the currency converted from, for example {@code "EUR"}
     * @param quoteCurrency code of the currency converted to, for example {@code "XOF"}
     * @return the rate, or empty when the pair is not in the table
     */
    Optional<CurrencyRate> findByBaseCurrencyAndQuoteCurrency(String baseCurrency, String quoteCurrency);
}
