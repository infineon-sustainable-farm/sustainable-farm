package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

/** Lets tests store a code whose digits they choose, such as one with leading zeros. */
public final class LoginCodeTestSupport {

    private LoginCodeTestSupport() {
    }

    public static String hash(LoginCodeService service, String id, String code) {
        return service.hash(id, code);
    }
}
