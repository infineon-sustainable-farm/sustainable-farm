package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.DripMaintenanceLogRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DripMaintenanceServiceTest {
    @Mock
    private DripMaintenanceLogRepository logRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Test
    void createRejectsUnknownZone() {
        UUID zoneId = UUID.randomUUID();
        DripMaintenanceLog log = new DripMaintenanceLog();
        log.setZoneId(zoneId);
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().create(log));
        verify(logRepository, never()).save(log);
    }

    private DripMaintenanceService service() {
        return new DripMaintenanceService(logRepository, zoneRepository);
    }
}