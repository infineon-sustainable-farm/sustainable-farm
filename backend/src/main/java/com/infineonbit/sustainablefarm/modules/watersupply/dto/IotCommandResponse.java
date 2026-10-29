package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotCommand;
import java.time.Instant;
import java.util.UUID;

/** Journal entry of a device command; {@code status}: queued, dispatched, rejected (acknowledged once a gateway exists). */
public record IotCommandResponse(UUID commandId, String deviceId, String command, String status, String payload,
                                 UUID requestedBy, Instant requestedAt, Instant acknowledgedAt) {
    public static IotCommandResponse from(IotCommand command) {
        return new IotCommandResponse(command.getId(), command.getDeviceId(), command.getCommandType(),
                command.getStatus(), command.getPayload(), command.getRequestedBy(), command.getRequestedAt(),
                command.getAcknowledgedAt());
    }
}
