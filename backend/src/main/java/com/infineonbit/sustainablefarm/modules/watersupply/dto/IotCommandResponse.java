package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotCommand;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.StoredValue;
import java.time.Instant;

/** Journal entry of a device command; {@code status}: queued, dispatched, rejected (acknowledged once a gateway exists). */
public record IotCommandResponse(Long commandId, String deviceId, String command, String status, String payload,
                                 Long requestedBy, Instant requestedAt, Instant acknowledgedAt) {
    public static IotCommandResponse from(IotCommand command) {
        return new IotCommandResponse(command.getId(), command.getDeviceId(), command.getCommandType().name(),
                StoredValue.valueOf(command.getStatus()), command.getPayload(), command.getRequestedBy(), command.getRequestedAt(),
                command.getAcknowledgedAt());
    }
}
