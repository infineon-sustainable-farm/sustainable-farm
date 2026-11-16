package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Visit announcement pushed by Visitor Management. {@code visitId} is its registration id (a number
 * is accepted and read as text): the idempotency key. {@code stops} are tour stop names as Visitor
 * Management displays them; {@code zones} stays available for a caller that knows the zone ids.
 */
public record VisitorEventRequest(@NotBlank @Size(max = 128) String visitId,
                                  @Size(max = 255) String groupName,
                                  @NotNull Instant expectedAt,
                                  List<UUID> zones,
                                  List<@Size(max = 255) String> stops) {
}
