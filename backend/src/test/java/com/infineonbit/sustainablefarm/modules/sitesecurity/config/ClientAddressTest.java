package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClientAddressTest {

    private static String of(String peer, String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        if (header != null) {
            request.addHeader(ClientAddress.HEADER, header);
        }
        return ClientAddress.of(request);
    }

    @Test
    void header_shouldBeBelieved_onlyFromNginxOrThisMachine() {
        assertEquals("198.51.100.7", of("172.18.0.5", "198.51.100.7"));
        assertEquals("198.51.100.7", of("127.0.0.1", "198.51.100.7"));
        assertEquals("2001:db8::1", of("10.0.0.2", "2001:db8::1"));
        assertEquals("203.0.113.9", of("203.0.113.9", "10.0.0.1"));
        assertEquals("172.18.0.5", of("172.18.0.5", null));
    }

    @Test
    void header_shouldBeALiteralAddress_neverAName() {
        assertEquals("172.18.0.5", of("172.18.0.5", "attacker.example"));
        assertEquals("172.18.0.5", of("172.18.0.5", "beef"));
        assertEquals("172.18.0.5", of("172.18.0.5", "1.2.3.4, 5.6.7.8"));
        assertFalse(ClientAddress.isLiteral("cafe"));
    }
}
