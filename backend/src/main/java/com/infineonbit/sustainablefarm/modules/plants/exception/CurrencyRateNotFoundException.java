package com.infineonbit.sustainablefarm.modules.plants.exception;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;

/** No rate recorded yet for a currency pair. Answered as a 404 ApiError by the core exception handler. */
public class CurrencyRateNotFoundException extends ResourceNotFoundException {
    public CurrencyRateNotFoundException(CurrencyCode baseCurrency, CurrencyCode quoteCurrency) {
        super("No " + baseCurrency + " to " + quoteCurrency + " rate recorded yet");
    }
}
