package com.infineonbit.sustainablefarm.modules.watersupply.iot;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.IotCommandRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IotCommandResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.service.IotCommandService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Outbound device commands (the reverse of {@link IotTelemetryController}): 202 once the command is
 * handed to the gateway, 503 {@code VALVE_GATEWAY_UNAVAILABLE} while no gateway is configured.
 */
@RestController
@RequestMapping("/api/iot/devices")
public class IotCommandController {
    private final IotCommandService commandService;

    public IotCommandController(IotCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/{deviceId}/commands")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IotCommandResponse command(@PathVariable String deviceId, @Valid @RequestBody IotCommandRequest request) {
        return commandService.command(deviceId, request);
    }

    @GetMapping("/{deviceId}/commands")
    public List<IotCommandResponse> history(@PathVariable String deviceId) {
        return commandService.history(deviceId);
    }
}
