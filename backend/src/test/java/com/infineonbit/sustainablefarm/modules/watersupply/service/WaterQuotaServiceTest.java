package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQuotaCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDto;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQuota;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQuotaRepository;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaterQuotaServiceTest {

    @Mock
    private WaterQuotaRepository quotaRepository;

    @Mock
    private FarmDirectory farmDirectory;

    @Mock
    private WaterConsumptionRepository consumptionRepository;

    @Mock
    private AlertService alertService;

    @Test
    void createQuotaRejectsUnknownFarm() {
        Long farmId = 125L;
        WaterQuotaCreateRequest request = new WaterQuotaCreateRequest(
                "farm", farmId, LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1), 1000d, null);
        when(farmDirectory.farmExists(farmId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service().createQuota(request));
        verify(quotaRepository, never()).save(any());
    }

    @Test
    void createQuotaRejectsInvalidTargetType() {
        WaterQuotaCreateRequest request = new WaterQuotaCreateRequest(
                "river", 1L, LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1), 1000d, null);

        assertThrows(IllegalArgumentException.class, () -> service().createQuota(request));
        verify(quotaRepository, never()).save(any());
    }

    @Test
    void createQuotaRejectsMonthNotStartingOnFirstDay() {
        Long farmId = 126L;
        WaterQuotaCreateRequest request = new WaterQuotaCreateRequest(
                "farm", farmId, LocalDate.of(2026, 9, 15), 1000d, null);
        org.mockito.Mockito.lenient().when(farmDirectory.farmExists(farmId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service().createQuota(request));
        verify(quotaRepository, never()).save(any());
    }

    @Test
    void usageMarksQuotaAsWarningAtEightyPercent() {
        Long farmId = 127L;
        stubQuotaForUsage(farmId, 1000d, 800d);

        List<Map<String, Object>> usage = service().currentUsage();

        assertEquals(1, usage.size());
        assertEquals("warning", usage.get(0).get("status"));
        assertEquals(80L, usage.get(0).get("usage_percentage"));
        assertEquals("Ferme Nord", usage.get(0).get("target_name"));
    }

    @Test
    void usageMarksQuotaAsExceededAtHundredPercent() {
        Long farmId = 128L;
        stubQuotaForUsage(farmId, 1000d, 1250d);

        List<Map<String, Object>> usage = service().currentUsage();

        assertEquals("exceeded", usage.get(0).get("status"));
        assertEquals(125L, usage.get(0).get("usage_percentage"));
        assertEquals(-250L, usage.get(0).get("remaining_liters"));
    }

    @Test
    void usageKeepsQuotaOkBelowEightyPercent() {
        Long farmId = 129L;
        stubQuotaForUsage(farmId, 1000d, 420d);

        List<Map<String, Object>> usage = service().currentUsage();

        assertEquals("ok", usage.get(0).get("status"));
        assertEquals(42L, usage.get(0).get("usage_percentage"));
    }

    @Test
    void usageForZoneUsesConsumptionMeasuredOnTheZone() {
        Long zoneId = 130L;

        when(quotaRepository.findAll()).thenReturn(List.of(zoneQuota(zoneId, 1000d)));
        when(farmDirectory.findZone(zoneId)).thenReturn(Optional.of(zone(1L, "Zone Goutte A")));
        when(consumptionRepository.sumConsumptionByZoneIdBetween(any(), any(), eq(zoneId))).thenReturn(250d);

        List<Map<String, Object>> usage = service().currentUsage();

        // Consumption now carries its zone: no more split of the farm volume between zones.
        assertEquals("ok", usage.get(0).get("status"));
        assertEquals(25L, usage.get(0).get("usage_percentage"));
        assertEquals("Zone Goutte A", usage.get(0).get("target_name"));
        verify(consumptionRepository, never()).sumConsumptionByFarmIdBetween(any(), any(), any());
    }

    @Test
    void checkThresholdsRaisesCriticalAlertWhenQuotaReached() {
        Long farmId = 131L;
        stubQuotaForThreshold(farmId, 1000d, 1000d);

        service().checkThresholds(farmId, null, Instant.now());

        verify(alertService).raiseOnce(eq("critical"), anyString(), anyString(), anyString());
    }

    @Test
    void checkThresholdsRaisesWarningAlertAtEightyPercent() {
        Long farmId = 132L;
        stubQuotaForThreshold(farmId, 1000d, 850d);

        service().checkThresholds(farmId, null, Instant.now());

        verify(alertService).raiseOnce(eq("warning"), anyString(), anyString(), anyString());
    }

    @Test
    void checkThresholdsStaysSilentBelowEightyPercent() {
        Long farmId = 133L;
        stubQuotaForThreshold(farmId, 1000d, 300d);

        service().checkThresholds(farmId, null, Instant.now());

        verify(alertService, never()).raiseOnce(anyString(), anyString(), anyString(), anyString());
    }

    /** Stubs necessaires au suivi de consommation (endpoint usage). */
    private void stubQuotaForUsage(Long farmId, double quotaLiters, double usedLiters) {
        when(quotaRepository.findAll()).thenReturn(List.of(quota(farmId, quotaLiters)));
        when(farmDirectory.findFarm(farmId)).thenReturn(Optional.of(farm("Ferme Nord")));
        when(consumptionRepository.sumConsumptionByFarmIdBetween(any(), any(), eq(farmId))).thenReturn(usedLiters);
    }

    /** Stubs necessaires au declenchement des alertes de seuil. */
    private void stubQuotaForThreshold(Long farmId, double quotaLiters, double usedLiters) {
        when(quotaRepository.findFirstByTargetTypeAndTargetIdAndQuotaMonthBetweenOrderByQuotaMonthDesc(
                eq("farm"), eq(farmId), any(), any())).thenReturn(Optional.of(quota(farmId, quotaLiters)));
        when(farmDirectory.findFarm(farmId)).thenReturn(Optional.of(farm("Ferme Nord")));
        when(consumptionRepository.sumConsumptionByFarmIdBetween(any(), any(), eq(farmId))).thenReturn(usedLiters);
    }

    private WaterQuota zoneQuota(Long zoneId, double quotaLiters) {
        WaterQuota quota = new WaterQuota();
        quota.setId(UUID.randomUUID());
        quota.setTargetType("zone");
        quota.setTargetId(zoneId);
        quota.setQuotaMonth(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1));
        quota.setQuotaLiters(quotaLiters);
        return quota;
    }

    private ZoneDto zone(Long fieldId, String name) {
        return TestZones.zone().id(2L).blockId(fieldId).name(name).build();
    }

    private WaterQuota quota(Long farmId, double quotaLiters) {
        WaterQuota quota = new WaterQuota();
        quota.setId(UUID.randomUUID());
        quota.setTargetType("farm");
        quota.setTargetId(farmId);
        quota.setQuotaMonth(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1));
        quota.setQuotaLiters(quotaLiters);
        return quota;
    }

    private FarmDto farm(String name) {
        return new FarmDto(1L, name, null, null, null, null, null, null);
    }

    private WaterQuotaService service() {
        return new WaterQuotaService(quotaRepository, farmDirectory,
                consumptionRepository, alertService, 0);
    }
}
