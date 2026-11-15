package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API-005 answer after a storage entry: identifiers as strings and
 * quantities as numbers, exactly as the contract documents.
 */
public record StorageEntryResponse(

        @JsonProperty("storage_entry_id")
        String storageEntryId,

        @JsonProperty("storage_location_id")
        String storageLocationId,

        @JsonProperty("entry_timestamp")
        LocalDateTime entryTimestamp,

        @JsonProperty("available_capacity_kg")
        BigDecimal availableCapacityKg,

        @JsonProperty("status")
        String status) {
}