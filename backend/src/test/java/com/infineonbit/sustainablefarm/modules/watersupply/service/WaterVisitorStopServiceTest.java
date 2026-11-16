package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorStopRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorStopResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorStop;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterVisitorStopRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Visitor Management stops (name only) mapped by the water team to a zone and/or a source. */
@ExtendWith(MockitoExtension.class)
class WaterVisitorStopServiceTest {
    @Mock
    private WaterVisitorStopRepository stopRepository;

    @Mock
    private FieldZoneRepository zoneRepository;

    @Mock
    private WaterSourceRepository sourceRepository;

    @Test
    void createsAStopWithItsNameTrimmed() {
        UUID zoneId = UUID.randomUUID();
        when(zoneRepository.existsById(zoneId)).thenReturn(true);
        when(stopRepository.findByStopNameIgnoreCase("Smart drip irrigation")).thenReturn(Optional.empty());
        when(stopRepository.save(any(WaterVisitorStop.class))).thenAnswer(call -> call.getArgument(0));

        VisitorStopResponse created = service().create(new VisitorStopRequest("  Smart drip irrigation ", zoneId, null));

        assertEquals("Smart drip irrigation", created.stopName());
        assertEquals(zoneId, created.zoneId());
    }

    @Test
    void aStopShowsAZoneATankOrBoth() {
        assertThrows(IllegalArgumentException.class, () -> service().create(new VisitorStopRequest("Hall", null, null)));
        verify(stopRepository, never()).save(any());
    }

    @Test
    void anUnknownZoneOrTankIsNotFound() {
        UUID sourceId = UUID.randomUUID();
        when(sourceRepository.existsById(sourceId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service().create(new VisitorStopRequest("Rain tank", null, sourceId)));
    }

    @Test
    void aStopNameIsMappedOnceWhateverItsCase() {
        UUID zoneId = UUID.randomUUID();
        WaterVisitorStop existing = new WaterVisitorStop();
        existing.setId(UUID.randomUUID());
        existing.setStopName("Smart drip irrigation");
        when(zoneRepository.existsById(zoneId)).thenReturn(true);
        when(stopRepository.findByStopNameIgnoreCase("SMART DRIP IRRIGATION")).thenReturn(Optional.of(existing));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().create(new VisitorStopRequest("SMART DRIP IRRIGATION", zoneId, null)));

        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getReason().contains("already mapped"));
    }

    @Test
    void resolvingABlankNameFindsNothing() {
        assertTrue(service().resolve("  ").isEmpty());
        verify(stopRepository, never()).findByStopNameIgnoreCase(any());
    }

    private WaterVisitorStopService service() {
        return new WaterVisitorStopService(stopRepository, zoneRepository, sourceRepository);
    }
}
