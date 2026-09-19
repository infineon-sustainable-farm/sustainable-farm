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
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Farm;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Field;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQuota;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FarmRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterConsumptionRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterQuotaRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.ZoneRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
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
    private FarmRepository farmRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private WaterConsumptionRepository consumptionRepository;

    @Mock
    private AlertService alertService;

    @Test
    void createQuotaRejectsUnknownFarm() {
        UUID farmId = UUID.randomUUID();
        WaterQuotaCreateRequest request = new WaterQuotaCreateRequest(
                "farm", farmId, LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1), 1000d, null);
        when(farmRepository.findById(farmId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().createQuota(request));
        verify(quotaRepository, never()).save(any());
    }

    @Test
    void createQuotaRejectsInvalidTargetType() {
        WaterQuotaCreateRequest request = new WaterQuotaCreateRequest(
                "river", UUID.randomUUID(), LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1), 1000d, null);

        assertThrows(IllegalArgumentException.class, () -> service().createQuota(request));
        verify(quotaRepository, never()).save(any());
    }

    @Test
    void createQuotaRejectsMonthNotStartingOnFirstDay() {
        UUID farmId = UUID.randomUUID();
        WaterQuotaCreateRequest request = new WaterQuotaCreateRequest(
                "farm", farmId, LocalDate.of(2026, 9, 15), 1000d, null);
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm("Ferme Nord")));

        assertThrows(IllegalArgumentException.class, () -> service().createQuota(request));
        verify(quotaRepository, never()).save(any());
    }

    @Test
    void usageMarksQuotaAsWarningAtEightyPercent() {
        UUID farmId = UUID.randomUUID();
        stubQuotaForUsage(farmId, 1000d, 800d);

        List<Map<String, Object>> usage = service().currentUsage();

        assertEquals(1, usage.size());
        assertEquals("warning", usage.get(0).get("status"));
        assertEquals(80L, usage.get(0).get("usage_percentage"));
        assertEquals("Ferme Nord", usage.get(0).get("target_name"));
    }

    @Test
    void usageMarksQuotaAsExceededAtHundredPercent() {
        UUID farmId = UUID.randomUUID();
        stubQuotaForUsage(farmId, 1000d, 1250d);

        List<Map<String, Object>> usage = service().currentUsage();

        assertEquals("exceeded", usage.get(0).get("status"));
        assertEquals(125L, usage.get(0).get("usage_percentage"));
        assertEquals(-250L, usage.get(0).get("remaining_liters"));
    }

    @Test
    void usageKeepsQuotaOkBelowEightyPercent() {
        UUID farmId = UUID.randomUUID();
        stubQuotaForUsage(farmId, 1000d, 420d);

        List<Map<String, Object>> usage = service().currentUsage();

        assertEquals("ok", usage.get(0).get("status"));
        assertEquals(42L, usage.get(0).get("usage_percentage"));
    }

    @Test
    void usageForZoneResolvesFieldFarmAndSplitsBetweenZones() {
        UUID farmId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();

        when(quotaRepository.findAll()).thenReturn(List.of(zoneQuota(zoneId, 1000d)));
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(zone(fieldId, "Zone Goutte A")));
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field(farmId)));
        when(zoneRepository.findByFieldId(fieldId))
                .thenReturn(List.of(zone(fieldId, "Zone Goutte A"), zone(fieldId, "Zone Nord")));
        when(consumptionRepository.sumConsumptionByFarmIdBetween(any(), any(), eq(farmId))).thenReturn(500d);

        List<Map<String, Object>> usage = service().currentUsage();

        // 500 L consommes sur le farm, repartis sur les 2 zones du champ => 250 L pour la zone.
        assertEquals("ok", usage.get(0).get("status"));
        assertEquals(25L, usage.get(0).get("usage_percentage"));
    }

    @Test
    void checkThresholdsRaisesCriticalAlertWhenQuotaReached() {
        UUID farmId = UUID.randomUUID();
        stubQuotaForThreshold(farmId, 1000d, 1000d);

        service().checkThresholds(farmId, null, Instant.now());

        verify(alertService).raiseOnce(eq("critical"), anyString(), anyString(), anyString());
    }

    @Test
    void checkThresholdsRaisesWarningAlertAtEightyPercent() {
        UUID farmId = UUID.randomUUID();
        stubQuotaForThreshold(farmId, 1000d, 850d);

        service().checkThresholds(farmId, null, Instant.now());

        verify(alertService).raiseOnce(eq("warning"), anyString(), anyString(), anyString());
    }

    @Test
    void checkThresholdsStaysSilentBelowEightyPercent() {
        UUID farmId = UUID.randomUUID();
        stubQuotaForThreshold(farmId, 1000d, 300d);

        service().checkThresholds(farmId, null, Instant.now());

        verify(alertService, never()).raiseOnce(anyString(), anyString(), anyString(), anyString());
    }

    /** Stubs necessaires au suivi de consommation (endpoint usage). */
    private void stubQuotaForUsage(UUID farmId, double quotaLiters, double usedLiters) {
        when(quotaRepository.findAll()).thenReturn(List.of(quota(farmId, quotaLiters)));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm("Ferme Nord")));
        when(consumptionRepository.sumConsumptionByFarmIdBetween(any(), any(), eq(farmId))).thenReturn(usedLiters);
    }

    /** Stubs necessaires au declenchement des alertes de seuil. */
    private void stubQuotaForThreshold(UUID farmId, double quotaLiters, double usedLiters) {
        when(quotaRepository.findFirstByTargetTypeAndTargetIdAndQuotaMonthBetweenOrderByQuotaMonthDesc(
                eq("farm"), eq(farmId), any(), any())).thenReturn(Optional.of(quota(farmId, quotaLiters)));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm("Ferme Nord")));
        when(consumptionRepository.sumConsumptionByFarmIdBetween(any(), any(), eq(farmId))).thenReturn(usedLiters);
    }

    private WaterQuota zoneQuota(UUID zoneId, double quotaLiters) {
        WaterQuota quota = new WaterQuota();
        quota.setId(UUID.randomUUID());
        quota.setTargetType("zone");
        quota.setTargetId(zoneId);
        quota.setQuotaMonth(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1));
        quota.setQuotaLiters(quotaLiters);
        return quota;
    }

    private Zone zone(UUID fieldId, String name) {
        Zone zone = new Zone();
        zone.setId(UUID.randomUUID());
        zone.setFieldId(fieldId);
        zone.setName(name);
        return zone;
    }

    private Field field(UUID farmId) {
        Field field = new Field();
        field.setId(UUID.randomUUID());
        field.setFarmId(farmId);
        return field;
    }

    private WaterQuota quota(UUID farmId, double quotaLiters) {
        WaterQuota quota = new WaterQuota();
        quota.setId(UUID.randomUUID());
        quota.setTargetType("farm");
        quota.setTargetId(farmId);
        quota.setQuotaMonth(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1));
        quota.setQuotaLiters(quotaLiters);
        return quota;
    }

    private Farm farm(String name) {
        Farm farm = new Farm();
        farm.setName(name);
        return farm;
    }

    private WaterQuotaService service() {
        return new WaterQuotaService(quotaRepository, farmRepository, zoneRepository, fieldRepository,
                consumptionRepository, alertService);
    }
}
