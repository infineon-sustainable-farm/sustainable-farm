package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.ExchangeRateNotFoundException;

/** No rate recorded yet for a currency pair. Answered as a 404 ApiError by the core exception handler. */
public class CurrencyRateNotFoundException extends ExchangeRateNotFoundException {
    public CurrencyRateNotFoundException(CurrencyCode baseCurrency, CurrencyCode quoteCurrency) {
        super(baseCurrency, quoteCurrency);
    }
}
