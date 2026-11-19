package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/** Status of a customer, check constraint {@code customers_status_check}. */
public enum CustomerStatus implements StoredValue {
    ACTIVE("active"),
    INACTIVE("inactive"),
    PROSPECT("prospect");

    private final String value;

    CustomerStatus(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<CustomerStatus> {
        public Converter() {
            super(CustomerStatus.class);
        }
    }
}
