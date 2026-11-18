package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/** Type of a customer, check constraint {@code customers_customer_type_check}. */
public enum CustomerType implements StoredValue {
    DISTRIBUTOR("distributor"),
    RETAILER("retailer"),
    DIRECT_EXPORT("direct_export"),
    OTHER("other");

    private final String value;

    CustomerType(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<CustomerType> {
        public Converter() {
            super(CustomerType.class);
        }
    }
}
