package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Status of a planned water unavailability. */
public enum UnavailabilityStatus implements StoredValue {
    PLANNED("planned"),
    ACTIVE("active"),
    CANCELLED("cancelled");

    private final String value;

    UnavailabilityStatus(String value) {
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
    public static UnavailabilityStatus of(String value) {
        return StoredValue.parse(UnavailabilityStatus.class, value, "status");
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<UnavailabilityStatus> {
        public Converter() {
            super(UnavailabilityStatus.class);
        }
    }
}
