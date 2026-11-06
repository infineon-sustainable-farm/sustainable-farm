package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import java.util.Locale;

public enum LogFilter {
    ALL,
    APPROVED,
    DENIED,
    VISITORS,
    STAFF,
    SERVICE;

    /** Throws for an unknown value, which the API reports as a 400. */
    public static LogFilter fromParam(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
