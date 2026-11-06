package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotCommand;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.ValveGatewayUnavailableException;
import org.springframework.stereotype.Component;

/** Default port while no device gateway exists: no physical command can leave the platform. */
@Component
public class NotConfiguredValveCommandPort implements ValveCommandPort {
    @Override
    public void send(IotCommand command) {
        throw new ValveGatewayUnavailableException(
                "Valve command gateway is not configured: MQTT/HTTP gateway, authentication and acknowledgement are required.");
    }
}
