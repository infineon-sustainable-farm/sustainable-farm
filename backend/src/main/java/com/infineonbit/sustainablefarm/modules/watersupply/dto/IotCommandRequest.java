package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Valve command: {@code OPEN_VALVE} (opened for {@code durationSeconds}, default and maximum
 * {@code app.water.iot.valve-max-duration-seconds}) or {@code CLOSE_VALVE}.
 */
public record IotCommandRequest(@NotBlank String command, Integer durationSeconds,
                                @Size(max = 200) String reason, UUID requestedBy) {
}
