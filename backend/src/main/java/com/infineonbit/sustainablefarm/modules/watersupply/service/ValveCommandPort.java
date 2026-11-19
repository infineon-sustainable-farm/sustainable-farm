package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotCommand;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.ValveGatewayUnavailableException;

/**
 * Outbound channel to the field devices (MQTT, HTTP gateway...). The protocol is not chosen yet:
 * the only implementation refuses every command ({@link NotConfiguredValveCommandPort}). A real
 * gateway adapter replaces it as a {@code @Primary} bean, without touching {@link IotCommandService}.
 */
public interface ValveCommandPort {
    /**
     * Hands the command to the device gateway; the acknowledgement arrives later.
     *
     * @throws ValveGatewayUnavailableException when the command could not be sent
     */
    void send(IotCommand command);
}
