package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Status of an irrigation schedule: {@code scheduled} until started, {@code running}, then {@code completed}; {@code postponed} by a weather suggestion or a planned unavailability. */
public enum IrrigationStatus implements StoredValue {
    SCHEDULED("scheduled"),
    RUNNING("running"),
    COMPLETED("completed"),
    POSTPONED("postponed");

    /** The statuses of an irrigation still to deliver: scheduled or running. */
    public static final java.util.List<IrrigationStatus> OPEN = java.util.List.of(SCHEDULED, RUNNING);

    private final String value;

    IrrigationStatus(String value) {
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
    public static IrrigationStatus of(String value) {
        return StoredValue.parse(IrrigationStatus.class, value, "irrigation status");
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<IrrigationStatus> {
        public Converter() {
            super(IrrigationStatus.class);
        }
    }
}
