package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IotCommandRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.IotCommandResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotCommand;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotDevice;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.ValveGatewayUnavailableException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IotCommandRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IotDeviceRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Valve commands (6.1): safety checks before anything leaves the platform, and an audit trail. */
@ExtendWith(MockitoExtension.class)
class IotCommandServiceTest {
    private static final String DEVICE = "valve-demo-01";

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private IotCommandRepository commandRepository;

    @Mock
    private WaterUnavailabilityService unavailabilityService;

    @Mock
    private ValveCommandPort valveCommandPort;

    @Mock
    private WaterAuthorizationService authorizationService;

    @Test
    void onlyOpenAndCloseValveExist() {
        assertThrows(IllegalArgumentException.class, () -> service().command(DEVICE, request("REBOOT", null)));
        verify(deviceRepository, never()).findByDeviceUid(any());
    }

    @Test
    void anUnknownDeviceIsNotFound() {
        when(deviceRepository.findByDeviceUid(DEVICE)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service().command(DEVICE, request("OPEN_VALVE", 10)));
    }

    @Test
    void anOfflineDeviceIsAConflict() {
        stubDeviceSeen(Instant.now().minusSeconds(3600));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().command(DEVICE, request("OPEN_VALVE", 10)));
        assertEquals(HttpStatus.CONFLICT.value(), error.getStatusCode().value());
    }

    @Test
    void anOpeningLongerThanTheConfiguredMaximumIsRejected() {
        stubDeviceSeen(Instant.now());

        assertThrows(IllegalArgumentException.class, () -> service().command(DEVICE, request("open_valve", 31)));
        assertThrows(IllegalArgumentException.class, () -> service().command(DEVICE, request("open_valve", 0)));
        verify(commandRepository, never()).save(any());
    }

    @Test
    void anOpeningIsRefusedDuringAWaterUnavailabilityButAClosingIsNot() {
        stubDeviceSeen(Instant.now());
        when(unavailabilityService.activeAt(any())).thenReturn(List.of(new WaterUnavailabilityResponse(
                UUID.randomUUID(), "farm", UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(60), null, "planned", null)));
        when(commandRepository.save(any(IotCommand.class))).thenAnswer(call -> call.getArgument(0));

        assertThrows(ResponseStatusException.class, () -> service().command(DEVICE, request("OPEN_VALVE", 10)));
        IotCommandResponse closed = service().command(DEVICE, request("CLOSE_VALVE", null));

        assertEquals("CLOSE_VALVE", closed.command());
        assertEquals("dispatched", closed.status());
    }

    @Test
    void anOpeningWithoutDurationUsesTheMaximumAndIsDispatched() {
        stubDeviceSeen(Instant.now());
        when(commandRepository.save(any(IotCommand.class))).thenAnswer(call -> call.getArgument(0));

        IotCommandResponse response = service().command(DEVICE, request("OPEN_VALVE", null));

        assertEquals("dispatched", response.status());
        assertEquals("{\"durationSeconds\":30,\"reason\":\"visitor \\\"demo\\\"\"}", response.payload());
        verify(authorizationService).checkPhysicalCommandAllowed("iot.command.OPEN_VALVE");
    }

    @Test
    void aGatewayRefusalIsJournaledAsRejectedAndPropagated() {
        stubDeviceSeen(Instant.now());
        List<IotCommand> journal = new java.util.ArrayList<>();
        when(commandRepository.save(any(IotCommand.class))).thenAnswer(call -> {
            IotCommand command = call.getArgument(0);
            journal.add(command);
            return command;
        });
        doThrow(new ValveGatewayUnavailableException("not configured")).when(valveCommandPort).send(any());

        assertThrows(ValveGatewayUnavailableException.class, () -> service().command(DEVICE, request("OPEN_VALVE", 5)));

        // Saved once as queued before dispatch, then again with its final status.
        assertEquals(2, journal.size());
        assertEquals("rejected", journal.get(1).getStatus());
        assertEquals("OPEN_VALVE", journal.get(1).getCommandType());
    }

    private void stubDeviceSeen(Instant lastSeen) {
        IotDevice device = new IotDevice();
        device.setDeviceUid(DEVICE);
        device.setLastSeen(lastSeen);
        when(deviceRepository.findByDeviceUid(DEVICE)).thenReturn(device);
    }

    private static IotCommandRequest request(String command, Integer durationSeconds) {
        return new IotCommandRequest(command, durationSeconds, "visitor \"demo\"", UUID.randomUUID());
    }

    private IotCommandService service() {
        return new IotCommandService(deviceRepository, commandRepository, unavailabilityService, valveCommandPort,
                authorizationService, WaterIntegrationProperties.defaults());
    }
}
