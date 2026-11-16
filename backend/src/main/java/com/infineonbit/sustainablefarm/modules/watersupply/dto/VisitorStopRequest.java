package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Name of a Visitor Management tour stop, exactly as that module displays it, and what it shows. */
public record VisitorStopRequest(@NotBlank @Size(max = 255) String stopName, UUID zoneId, UUID sourceId) {
}
