package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Visit announcement pushed by Visitor Management; {@code visitId} is its stable identifier (idempotency key). */
public record VisitorEventRequest(@NotBlank @Size(max = 128) String visitId,
                                  @Size(max = 255) String groupName,
                                  @NotNull Instant expectedAt,
                                  List<UUID> zones) {
}
