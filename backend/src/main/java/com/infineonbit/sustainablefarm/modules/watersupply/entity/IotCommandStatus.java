package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Status of an outbound valve command: {@code queued} when journaled, then {@code dispatched} or {@code rejected} by the gateway. */
public enum IotCommandStatus implements StoredValue {
    QUEUED("queued"),
    DISPATCHED("dispatched"),
    REJECTED("rejected");

    private final String value;

    IotCommandStatus(String value) {
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
    public static IotCommandStatus of(String value) {
        return StoredValue.parse(IotCommandStatus.class, value, "command status");
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<IotCommandStatus> {
        public Converter() {
            super(IotCommandStatus.class);
        }
    }
}
