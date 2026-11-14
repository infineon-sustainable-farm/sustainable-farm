package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Password rules (NIST SP 800-63B style): length matters, composition rules do
 * not. 12 to 128 characters, spaces allowed, not the email, not a very common
 * password, not one repeated character.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON = Set.of(
            "123456789012", "1234567890123", "12345678910111213", "000000000000", "111111111111",
            "123123123123", "qwertyuiopas", "qwertyuiop123", "qwerty123456", "1q2w3e4r5t6y",
            "1qaz2wsx3edc", "azertyuiopqs", "azerty123456", "password1234", "password12345",
            "password123456", "passwordpassword", "motdepasse123", "motdepasse1234", "iloveyou1234",
            "letmein12345", "welcome12345", "welcome123456", "administrator", "administrator1",
            "changeme1234", "abcdefghijkl", "abc123abc123", "trustno1trustno1", "football1234",
            "baseball1234", "sunshine1234", "princess1234", "dragon123456", "monkey123456",
            "superman1234", "starwars1234", "computer1234", "internet1234", "farmsecurity",
            "farmsecurity1", "farmsecurity123", "sustainablefarm", "fencingsecurity");

    private PasswordPolicy() {
    }

    /** The reason the password is refused, or empty when it is acceptable. */
    public static Optional<String> problem(String password, String email) {
        if (password == null) {
            return Optional.of("Choose a password.");
        }
        String normalized = Normalizer.normalize(password, Normalizer.Form.NFC);
        int length = normalized.codePointCount(0, normalized.length());
        if (length < MIN_LENGTH) {
            return Optional.of("Use at least " + MIN_LENGTH + " characters.");
        }
        if (length > MAX_LENGTH) {
            return Optional.of("Use at most " + MAX_LENGTH + " characters.");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (email != null && lower.trim().equals(email.trim().toLowerCase(Locale.ROOT))) {
            return Optional.of("The password must not be your email address.");
        }
        if (COMMON.contains(lower.replace(" ", ""))) {
            return Optional.of("This password is too common. Choose another one.");
        }
        if (normalized.codePoints().distinct().count() == 1) {
            return Optional.of("The password must not repeat a single character.");
        }
        return Optional.empty();
    }
}
