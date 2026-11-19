package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Status of an irrigation log: {@code started} when the schedule starts, {@code completed} when it stops. */
public enum IrrigationLogStatus implements StoredValue {
    STARTED("started"),
    COMPLETED("completed");

    private final String value;

    IrrigationLogStatus(String value) {
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
    public static IrrigationLogStatus of(String value) {
        return StoredValue.parse(IrrigationLogStatus.class, value, "irrigation log status");
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<IrrigationLogStatus> {
        public Converter() {
            super(IrrigationLogStatus.class);
        }
    }
}
