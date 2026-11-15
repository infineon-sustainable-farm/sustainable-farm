package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import java.util.Set;

public final class AccountValues {

    private AccountValues() {
    }

    public enum Permission {
        VIEW_DASHBOARD, VIEW_ZONES, VIEW_ACCESS_LOG, VIEW_ACCESS_REGISTER, LEGACY_READ_ALL;

        public static final Set<Permission> GRANTABLE =
                Set.of(VIEW_DASHBOARD, VIEW_ZONES, VIEW_ACCESS_LOG, VIEW_ACCESS_REGISTER);
    }
}
