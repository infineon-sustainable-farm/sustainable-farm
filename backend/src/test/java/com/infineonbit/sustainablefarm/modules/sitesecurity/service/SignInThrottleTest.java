package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignInThrottleTest {

    @Test
    void delay_shouldDoubleAfterTheThirdFailure_upToFifteenMinutes() {
        assertEquals(30, SignInThrottle.delayAfter(3).toSeconds());
        assertEquals(60, SignInThrottle.delayAfter(4).toSeconds());
        assertEquals(120, SignInThrottle.delayAfter(5).toSeconds());
        assertEquals(240, SignInThrottle.delayAfter(6).toSeconds());
        assertEquals(480, SignInThrottle.delayAfter(7).toSeconds());
        assertEquals(900, SignInThrottle.delayAfter(8).toSeconds());
        assertEquals(900, SignInThrottle.delayAfter(1_000).toSeconds());
    }
}
