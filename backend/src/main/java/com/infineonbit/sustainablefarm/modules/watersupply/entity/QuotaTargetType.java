package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** What a monthly water quota applies to: a farm or a zone. */
public enum QuotaTargetType implements StoredValue {
    FARM("farm"),
    ZONE("zone");

    private final String value;

    QuotaTargetType(String value) {
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
    public static QuotaTargetType of(String value) {
        return StoredValue.parse(QuotaTargetType.class, value, "target type");
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<QuotaTargetType> {
        public Converter() {
            super(QuotaTargetType.class);
        }
    }
}
