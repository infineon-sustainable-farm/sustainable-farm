package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/** Status of a campaign, check constraint {@code campaigns_status_check}. */
public enum CampaignStatus implements StoredValue {
    ACTIVE("active"),
    SCHEDULED("scheduled"),
    ENDED("ended"),
    DRAFT("draft");

    private final String value;

    CampaignStatus(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    /** Stores the lower-case value. */
    @jakarta.persistence.Converter
    public static class Converter extends StoredValueConverter<CampaignStatus> {
        public Converter() {
            super(CampaignStatus.class);
        }
    }
}
