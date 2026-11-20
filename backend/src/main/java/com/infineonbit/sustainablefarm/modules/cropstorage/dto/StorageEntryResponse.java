package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * API-005 answer for a storage entry: identifiers as strings and quantities as
 * numbers. entry_timestamp is an ISO 8601 instant with an explicit UTC offset,
 * converted from the server-local creation time (never just relabelled).
 *
 * <p>available_capacity_kg is the room left in the zone right after the entry on
 * creation, and the current room left on a later read.
 */
public record StorageEntryResponse(

        @JsonProperty("storage_entry_id")
        String storageEntryId,

        @JsonProperty("storage_location_id")
        String storageLocationId,

        @JsonProperty("entry_timestamp")
        OffsetDateTime entryTimestamp,

        @JsonProperty("available_capacity_kg")
        BigDecimal availableCapacityKg,

        @JsonProperty("status")
        String status) {
}
