package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/** Status of a shipment, check constraint {@code shipments_status_check}. */
public enum ShipmentStatus implements StoredValue {
    PACKED("packed"),
    SHIPPED("shipped"),
    PORT("port"),
    IN_TRANSIT("in_transit"),
    DELIVERED("delivered"),
    DELAYED("delayed");

    private final String value;

    ShipmentStatus(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<ShipmentStatus> {
        public Converter() {
            super(ShipmentStatus.class);
        }
    }
}
