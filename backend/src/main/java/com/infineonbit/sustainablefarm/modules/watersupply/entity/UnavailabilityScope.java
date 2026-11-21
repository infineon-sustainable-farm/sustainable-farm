package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** What a planned water unavailability covers: a whole farm, a water source or a zone. */
public enum UnavailabilityScope implements StoredValue {
    FARM("farm"),
    SOURCE("source"),
    ZONE("zone");

    private final String value;

    UnavailabilityScope(String value) {
        this.value = value;
    }

    @JsonValue
    @Override
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /** The constant of a value, case-insensitive; {@code null} for a blank value, 400 for an unknown one. */
    @JsonCreator
    public static UnavailabilityScope of(String value) {
        return StoredValue.parse(UnavailabilityScope.class, value, "scope");
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<UnavailabilityScope> {
        public Converter() {
            super(UnavailabilityScope.class);
        }
    }
}
