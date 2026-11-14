package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    private static final String EMAIL = "ada@example.test";

    @Test
    void acceptsLongPasswords_withoutCompositionRules() {
        assertTrue(PasswordPolicy.problem("correct horse battery staple", EMAIL).isEmpty());
        assertTrue(PasswordPolicy.problem("abcdefghijkm", EMAIL).isEmpty());
        assertTrue(PasswordPolicy.problem("x".repeat(127) + "y", EMAIL).isEmpty());
    }

    @Test
    void refusesTooShortOrTooLong() {
        assertFalse(PasswordPolicy.problem("short pass1", EMAIL).isEmpty());
        assertFalse(PasswordPolicy.problem("x".repeat(128) + "y", EMAIL).isEmpty());
        assertFalse(PasswordPolicy.problem(null, EMAIL).isEmpty());
    }

    @Test
    void countsCharacters_notBytes() {
        assertTrue(PasswordPolicy.problem("éééééééééééa", EMAIL).isEmpty());
    }

    @Test
    void refusesTheEmailAndCommonPasswords() {
        assertFalse(PasswordPolicy.problem("ADA@example.test", EMAIL).isEmpty());
        assertFalse(PasswordPolicy.problem("password1234", EMAIL).isEmpty());
        assertFalse(PasswordPolicy.problem("Password 1234", EMAIL).isEmpty());
        assertFalse(PasswordPolicy.problem("aaaaaaaaaaaaaaa", EMAIL).isEmpty());
    }
}
