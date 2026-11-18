package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/** Status of a demand report, check constraint {@code demand_reports_status_check}. */
public enum DemandReportStatus implements StoredValue {
    READY("ready"),
    PROCESSING("processing"),
    SCHEDULED("scheduled"),
    FAILED("failed");

    private final String value;

    DemandReportStatus(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<DemandReportStatus> {
        public Converter() {
            super(DemandReportStatus.class);
        }
    }
}
