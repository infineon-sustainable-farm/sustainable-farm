package com.infineonbit.sustainablefarm.modules.plants.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecimalsTest {

    @Test
    void of_shouldKeepTheNumber_atTheScaleOfTheColumn() {
        assertEquals(new BigDecimal("12.500"), Decimals.of(12.5, 3));
        assertEquals(new BigDecimal("0.125"), Decimals.of(0.125, 3));
    }

    @Test
    void of_shouldRoundHalfUp_whenTheNumberHasMoreDecimalsThanTheColumn() {
        assertEquals(new BigDecimal("12.346"), Decimals.of(12.3455, 3));
    }

    @Test
    void toDouble_shouldDropTheTrailingZerosOfTheColumn() {
        assertEquals(12.5, Decimals.toDouble(new BigDecimal("12.500")));
        assertEquals(8.0, Decimals.toDouble(new BigDecimal("8.0")));
    }

    @Test
    void sameValue_shouldIgnoreTheScale() {
        assertTrue(Decimals.sameValue(12.5, new BigDecimal("12.500")));
        assertFalse(Decimals.sameValue(12.5, new BigDecimal("12.501")));
        assertTrue(Decimals.sameValue(null, null));
        assertFalse(Decimals.sameValue(null, BigDecimal.ONE));
        assertFalse(Decimals.sameValue(1.0, null));
    }

    @Test
    void nullStaysNull() {
        assertNull(Decimals.of(null, 3));
        assertNull(Decimals.toDouble(null));
    }
}
