package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IotCommandRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IotCommandResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotCommand;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotDevice;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.ValveGatewayUnavailableException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IotCommandRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IotDeviceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Physical valve commands (demo valve for Visitor Management, then field valves).
 *
 * <p>Safety rules, applied before anything leaves the platform: only {@code OPEN_VALVE} and
 * {@code CLOSE_VALVE} exist, the device must be known and seen recently, an opening is time-boxed by
 * {@code app.water.iot.valve-max-duration-seconds} and refused during a planned water
 * unavailability (closing is always allowed). Every accepted request is journaled, including the
 * ones the gateway could not take: the audit trail must show attempts, not only successes.</p>
 */
@Service
@Transactional(readOnly = true)
public class IotCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IotCommandService.class);
    static final String OPEN_VALVE = "OPEN_VALVE";
    static final String CLOSE_VALVE = "CLOSE_VALVE";

    private final IotDeviceRepository deviceRepository;
    private final IotCommandRepository commandRepository;
    private final WaterUnavailabilityService unavailabilityService;
    private final ValveCommandPort valveCommandPort;
    private final WaterAuthorizationService authorizationService;
    private final WaterIntegrationProperties properties;

    public IotCommandService(IotDeviceRepository deviceRepository, IotCommandRepository commandRepository,
                             WaterUnavailabilityService unavailabilityService, ValveCommandPort valveCommandPort,
                             WaterAuthorizationService authorizationService, WaterIntegrationProperties properties) {
        this.deviceRepository = deviceRepository;
        this.commandRepository = commandRepository;
        this.unavailabilityService = unavailabilityService;
        this.valveCommandPort = valveCommandPort;
        this.authorizationService = authorizationService;
        this.properties = properties;
    }

    /**
     * Validates, journals and dispatches a command. {@code noRollbackFor}: when the gateway refuses,
     * the journal entry (status {@code rejected}) is committed and the 503 still reaches the caller.
     */
    @Transactional(noRollbackFor = ValveGatewayUnavailableException.class)
    public IotCommandResponse command(String deviceId, IotCommandRequest request) {
        String commandType = request.command().trim().toUpperCase(Locale.ROOT);
        if (!OPEN_VALVE.equals(commandType) && !CLOSE_VALVE.equals(commandType)) {
            throw new IllegalArgumentException("command must be OPEN_VALVE or CLOSE_VALVE");
        }
        authorizationService.checkPhysicalCommandAllowed("iot.command." + commandType);
        IotDevice device = deviceRepository.findByDeviceUid(deviceId);
        if (device == null) {
            throw new NotFoundException("IoT device");
        }
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(properties.iotOfflineThresholdMinutes * 60L);
        if (device.getLastSeen() == null || device.getLastSeen().isBefore(cutoff)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "IoT device is offline");
        }
        Integer duration = null;
        if (OPEN_VALVE.equals(commandType)) {
            duration = request.durationSeconds() == null ? properties.valveMaxDurationSeconds : request.durationSeconds();
            if (duration < 1 || duration > properties.valveMaxDurationSeconds) {
                throw new IllegalArgumentException(
                        "durationSeconds must be between 1 and " + properties.valveMaxDurationSeconds);
            }
            if (!unavailabilityService.activeAt(now).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A planned water unavailability is in progress");
            }
        }

        IotCommand command = new IotCommand();
        command.setDeviceId(deviceId);
        command.setCommandType(commandType);
        command.setPayload(payload(duration, request.reason()));
        command.setStatus("queued");
        command.setRequestedBy(request.requestedBy());
        command.setRequestedAt(now);
        IotCommand saved = commandRepository.save(command);
        try {
            valveCommandPort.send(saved);
            saved.setStatus("dispatched");
        } catch (ValveGatewayUnavailableException ex) {
            saved.setStatus("rejected");
            commandRepository.save(saved);
            LOGGER.warn("Valve command {} {} on device {} rejected: {}", saved.getId(), commandType, deviceId, ex.getMessage());
            throw ex;
        }
        return IotCommandResponse.from(commandRepository.save(saved));
    }

    /** Command journal of a device, most recent first. */
    public List<IotCommandResponse> history(String deviceId) {
        return commandRepository.findByDeviceIdOrderByRequestedAtDesc(deviceId).stream()
                .map(IotCommandResponse::from)
                .toList();
    }

    private static String payload(Integer durationSeconds, String reason) {
        String safeReason = reason == null ? "" : reason.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"durationSeconds\":" + durationSeconds + ",\"reason\":\"" + safeReason + "\"}";
    }
}
