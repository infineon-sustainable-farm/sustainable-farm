package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/** Scenario of a demand forecast, check constraint {@code demand_forecasts_scenario_check}. */
public enum ForecastScenario implements StoredValue {
    PESSIMISTIC("pessimistic"),
    BASE("base"),
    OPTIMISTIC("optimistic");

    private final String value;

    ForecastScenario(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<ForecastScenario> {
        public Converter() {
            super(ForecastScenario.class);
        }
    }
}
