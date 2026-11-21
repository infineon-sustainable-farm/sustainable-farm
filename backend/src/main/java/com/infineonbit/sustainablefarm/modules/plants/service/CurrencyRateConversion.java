package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.CurrencyConversion;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyRate;
import com.infineonbit.sustainablefarm.modules.plants.exception.CurrencyRateNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CurrencyRateRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The {@link CurrencyConversion} of the application, on the rates of {@code currency_rate}.
 *
 * <p>Plants owns the exchange rates (ADR 0004): the other modules inject the core port and do
 * not depend on plants. The table keeps one rate per currency pair, without history, so the
 * rate recorded now is used whatever the date asked. A pair is read in both directions: with
 * 1 EUR = 655.957 XOF, an amount in EUR is multiplied by the rate and an amount in XOF divided
 * by it. The result is rounded to the minor unit of the target currency, halves up, exactly as
 * the costs of the fertilizer purchases and the scenario analyses ({@link CurrencyConverter}).
 * The rate is read on every call, so a rate entered or replaced counts at once.
 */
@Service
@AllArgsConstructor
public class CurrencyRateConversion implements CurrencyConversion {

    private final CurrencyRateRepository currencyRateRepository;

    @Override
    @Transactional(readOnly = true)
    public Money convert(Money money, CurrencyCode target, LocalDate date) {
        CurrencyCode source = money.currency();
        if (source == target) {
            return money;
        }
        Optional<BigDecimal> direct = rate(source, target);
        if (direct.isPresent()) {
            return Money.of(target.round(money.amount().multiply(direct.get())), target);
        }
        BigDecimal inverse = rate(target, source)
                .orElseThrow(() -> new CurrencyRateNotFoundException(source, target));
        return Money.of(money.amount().divide(inverse, target.fractionDigits(), RoundingMode.HALF_UP), target);
    }

    private Optional<BigDecimal> rate(CurrencyCode base, CurrencyCode quote) {
        return currencyRateRepository.findByBaseCurrencyAndQuoteCurrency(base.name(), quote.name())
                .map(CurrencyRate::getRate);
    }
}
