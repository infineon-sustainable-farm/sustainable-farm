package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionServiceTest {

    @Test
    void cookie_shouldBeSecureByDefault_andPlainOnlyWhenAskedFor() {
        assertTrue(new SessionService(null, null, null, null, null, true).sessionCookie(new SessionService.SignIn("v", null, java.time.Instant.now().plusSeconds(60))).isSecure());

        var plain = new SessionService(null, null, null, null, null, false).sessionCookie(new SessionService.SignIn("v", null, java.time.Instant.now().plusSeconds(60)));
        assertFalse(plain.isSecure());
        assertTrue(plain.isHttpOnly());
        assertEquals("Strict", plain.getSameSite());
    }

    @Test
    void resolve_shouldIgnoreAMissingCookie() {
        SessionService service = new SessionService(null, null, null, null, null, true);

        assertTrue(service.resolve(null).isEmpty());
        assertTrue(service.resolve(" ").isEmpty());
    }

    @Test
    void hash_shouldBeHexSha256() {
        assertEquals(64, SessionService.hash("value").length());
        assertEquals(SessionService.hash("value"), SessionService.hash("value"));
    }
}
