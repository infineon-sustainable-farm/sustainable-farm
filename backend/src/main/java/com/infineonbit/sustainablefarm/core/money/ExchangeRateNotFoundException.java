package com.infineonbit.sustainablefarm.core.money;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

/** No exchange rate recorded yet for a currency pair. Answered as a 404 ApiError by the core exception handler. */
public class ExchangeRateNotFoundException extends ResourceNotFoundException {

    public ExchangeRateNotFoundException(CurrencyCode baseCurrency, CurrencyCode quoteCurrency) {
        super("No " + baseCurrency + " to " + quoteCurrency + " rate recorded yet");
    }
}
