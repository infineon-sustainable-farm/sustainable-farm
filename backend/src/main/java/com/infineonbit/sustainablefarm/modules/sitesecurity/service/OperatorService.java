package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import java.util.Locale;

/**
 * Account email normalization, shared by sign-in, invitations and the physical
 * access register. Account management itself lives in {@link AccountService}.
 */
public final class OperatorService {

    private OperatorService() {
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
