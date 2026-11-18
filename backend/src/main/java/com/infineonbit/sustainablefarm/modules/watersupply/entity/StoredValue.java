package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * An enum of Water Supply whose constant is stored and sent in JSON as a lower-case value
 * ({@code "scheduled"}, {@code "farm"}...), the value the module has always used (docs/adr/0002).
 *
 * <p>Each such enum carries its value, exposes it with {@code @JsonValue}, reads it back
 * case-insensitively with {@link #parse} and maps it to its column through a
 * {@link StoredValueConverter}.</p>
 */
public interface StoredValue {

    /** The stored and JSON value. */
    String value();

    /**
     * The constant whose value matches {@code value}, ignoring case and surrounding spaces;
     * {@code null} for a null or blank value.
     *
     * @throws IllegalArgumentException (400) for an unknown value, listing the expected ones
     */
    static <E extends Enum<E> & StoredValue> E parse(Class<E> type, String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (E constant : type.getEnumConstants()) {
            if (constant.value().equals(normalized)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Invalid " + label + ": " + Arrays.stream(type.getEnumConstants())
                .map(StoredValue::value).collect(Collectors.joining(", ")) + " expected.");
    }

    /** The stored value of a constant, {@code null} for {@code null}: what the API sends. */
    static String valueOf(StoredValue constant) {
        return constant == null ? null : constant.value();
    }
}
