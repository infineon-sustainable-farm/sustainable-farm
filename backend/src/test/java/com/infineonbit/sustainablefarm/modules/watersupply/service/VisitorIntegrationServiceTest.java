package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.SafetyBriefingRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorEventRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSafetyBriefing;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorEvent;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.ExternalServiceException;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSafetyBriefingRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterSourceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterVisitorEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

/** Visitor Management read models and events (3.1-3.6). */
@ExtendWith(MockitoExtension.class)
class VisitorIntegrationServiceTest {
    @Mock
    private WaterVisitorEventRepository visitorEventRepository;

    @Mock
    private WaterSafetyBriefingRepository safetyRepository;

    @Mock
    private WaterConsumptionRepository consumptionRepository;

    @Mock
    private WaterSourceRepository sourceRepository;

    @Mock
    private FieldZoneRepository zoneRepository;

    @Mock
    private WaterService waterService;

    @Mock
    private IrrigationService irrigationService;

    @Mock
    private WaterEconomyService economyService;

    @Mock
    private WaterUnavailabilityService unavailabilityService;

    @Mock
    private AlertService alertService;

    @Test
    @SuppressWarnings("unchecked")
    void liveConsumptionSumsTheWindowAndReportsTheLastMeasurement() {
        UUID sourceId = UUID.randomUUID();
        when(sourceRepository.existsById(sourceId)).thenReturn(true);
        when(consumptionRepository.findAll(any(Specification.class))).thenReturn(List.of(
                consumption(12.5, Instant.parse("2026-10-13T10:01:00Z")),
                consumption(7.5, Instant.parse("2026-10-13T10:09:00Z"))));

        Map<String, Object> live = service().liveConsumption(sourceId, null, null);

        assertEquals(20.0, live.get("liters"));
        assertEquals(2, live.get("measurementCount"));
        assertEquals(15, live.get("windowMinutes"));
        assertEquals(Instant.parse("2026-10-13T10:09:00Z"), live.get("lastMeasurementAt"));
    }

    @Test
    void liveConsumptionWindowIsBoundedBetweenOneMinuteAndOneDay() {
        assertEquals(1, service().liveConsumption(null, null, 0).get("windowMinutes"));
        assertEquals(1440, service().liveConsumption(null, null, 100_000).get("windowMinutes"));
    }

    @Test
    void liveConsumptionRejectsAnUnknownZone() {
        UUID zoneId = UUID.randomUUID();
        when(zoneRepository.existsById(zoneId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service().liveConsumption(null, zoneId, null));
    }

    @Test
    void summaryStaysReadableWhenTheWeatherIsDown() {
        when(economyService.savings("month")).thenThrow(new ExternalServiceException("Weather service is unavailable", null));

        Map<String, Object> summary = service().summary(null, null);

        assertNull(summary.get("waterSavingsPercentage"));
        assertEquals(0d, ((Map<?, ?>) summary.get("liveConsumption")).get("liters"));
    }

    @Test
    void reSendingAVisitUpdatesItInsteadOfCreatingADuplicate() {
        WaterVisitorEvent existing = new WaterVisitorEvent();
        existing.setId(UUID.randomUUID());
        existing.setVisitId("VIS-1");
        when(visitorEventRepository.findByVisitId("VIS-1")).thenReturn(Optional.of(existing));
        when(visitorEventRepository.save(existing)).thenReturn(existing);

        Map<String, Object> response = service().visitorEvent(new VisitorEventRequest(
                " VIS-1 ", "School A", Instant.parse("2026-10-20T09:00:00Z"), List.of()));

        assertEquals(false, response.get("created"));
        assertEquals(existing.getId(), response.get("eventId"));
        assertEquals("School A", existing.getGroupName());
        verify(alertService).raiseOnce(eq("info"), eq("Visitor group near water facilities - School A"), anyString(), anyString());
    }

    @Test
    void aVisitOnAZoneUnderUnavailabilityIsFlaggedAsAConflict() {
        UUID zoneId = UUID.randomUUID();
        Instant expectedAt = Instant.parse("2026-10-20T09:00:00Z");
        when(visitorEventRepository.findByVisitId("VIS-2")).thenReturn(Optional.empty());
        when(visitorEventRepository.save(any(WaterVisitorEvent.class))).thenAnswer(call -> call.getArgument(0));
        when(unavailabilityService.isZoneUnavailable(zoneId, expectedAt)).thenReturn(true);

        Map<String, Object> response = service().visitorEvent(new VisitorEventRequest("VIS-2", null, expectedAt, List.of(zoneId, zoneId)));

        assertEquals(true, response.get("conflict"));
        assertEquals(List.of(zoneId), response.get("conflictingZones"));
        assertEquals(true, response.get("created"));
        verify(alertService).raiseOnce(eq("warning"), eq("Visitor group expected during water unavailability - VIS-2"),
                anyString(), anyString());
    }

    @Test
    void theDefaultBriefingIsServedButNeverStored() {
        when(safetyRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        Map<String, Object> briefing = service().safetyBriefing();

        assertEquals(false, briefing.get("validated"));
        assertEquals(10, briefing.get("maxCapacity"));
        assertTrue(((List<?>) briefing.get("forbiddenZones")).contains("electric pump room"));
        verify(safetyRepository, never()).save(any());
    }

    @Test
    void publishingABriefingCreatesANewVersionAndKeepsTheOmittedFields() {
        WaterSafetyBriefing current = new WaterSafetyBriefing();
        current.setId(UUID.randomUUID());
        current.setVersion("v1");
        current.setAllowedZones("demo path");
        current.setText("old text");
        current.setMaxCapacity(12);
        current.setGuideRequired(true);
        when(safetyRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(current));
        when(safetyRepository.save(any(WaterSafetyBriefing.class))).thenAnswer(call -> call.getArgument(0));

        Map<String, Object> published = service().updateSafetyBriefing(
                new SafetyBriefingRequest("v2", null, List.of("pump room", " "), null, "new text", null, false));

        ArgumentCaptor<WaterSafetyBriefing> saved = ArgumentCaptor.forClass(WaterSafetyBriefing.class);
        verify(safetyRepository).save(saved.capture());
        assertNotSame(current, saved.getValue());
        assertEquals("v2", published.get("version"));
        assertEquals(List.of("demo path"), published.get("allowedZones"));
        assertEquals(List.of("pump room"), published.get("forbiddenZones"));
        assertEquals("new text", published.get("text"));
        assertEquals(12, published.get("maxCapacity"));
        assertEquals(false, published.get("guideRequired"));
    }

    @Test
    void aBriefingCapacityBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service().updateSafetyBriefing(
                new SafetyBriefingRequest(null, null, null, null, null, 0, null)));
    }

    private static WaterConsumption consumption(double liters, Instant at) {
        WaterConsumption consumption = new WaterConsumption();
        consumption.setConsumptionLiters(liters);
        consumption.setConsumptionDate(at);
        return consumption;
    }

    private VisitorIntegrationService service() {
        return new VisitorIntegrationService(visitorEventRepository, safetyRepository, consumptionRepository,
                sourceRepository, zoneRepository, waterService, irrigationService, economyService,
                unavailabilityService, alertService, WaterIntegrationProperties.defaults());
    }
}
