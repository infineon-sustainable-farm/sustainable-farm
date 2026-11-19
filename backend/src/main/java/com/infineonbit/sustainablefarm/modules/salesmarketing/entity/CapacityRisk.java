package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/** Capacity risk of a demand forecast, check constraint {@code demand_forecasts_capacity_risk_check}. */
public enum CapacityRisk implements StoredValue {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String value;

    CapacityRisk(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<CapacityRisk> {
        public Converter() {
            super(CapacityRisk.class);
        }
    }
}
