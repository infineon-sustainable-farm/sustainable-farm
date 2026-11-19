package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Origin of an irrigation schedule: {@code manual} (entered by a user) or {@code auto} (soil moisture rule, IrrigationAutomationService). */
public enum IrrigationTrigger implements StoredValue {
    MANUAL("manual"),
    AUTO("auto");

    private final String value;

    IrrigationTrigger(String value) {
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
    public static IrrigationTrigger of(String value) {
        return StoredValue.parse(IrrigationTrigger.class, value, "trigger source");
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<IrrigationTrigger> {
        public Converter() {
            super(IrrigationTrigger.class);
        }
    }
}
