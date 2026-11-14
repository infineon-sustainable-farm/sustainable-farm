package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the Plants -> Product Transformation harvest mapping.
 */
class PlantsIntegrationServiceTest {

    private final PlantsIntegrationService service = new PlantsIntegrationService(null);

    @Test
    void resolvesKeittCaseInsensitively() {
        assertEquals(MangoVariety.KEITT, MangoVariety.fromPlants("Keitt"));
        assertEquals(MangoVariety.KEITT, MangoVariety.fromPlants("keitt"));
        assertEquals(MangoVariety.KEITT, MangoVariety.fromPlants("KEITT"));
    }

    @Test
    void resolvesAccentedAndMixedCaseVarieties() {
        assertEquals(MangoVariety.AMÉLIE, MangoVariety.fromPlants("Amélie"));
        assertEquals(MangoVariety.AMÉLIE, MangoVariety.fromPlants("Amelie"));
        assertEquals(MangoVariety.KENT, MangoVariety.fromPlants("kent"));
        assertEquals(MangoVariety.TOMMY, MangoVariety.fromPlants("Tommy Atkins"));
    }

    @Test
    void unknownVarietyFallsBackToOther() {
        assertEquals(MangoVariety.OTHER, MangoVariety.fromPlants(null));
        assertEquals(MangoVariety.OTHER, MangoVariety.fromPlants("Alphonso"));
    }

    @Test
    void mappingKeepsPlantsLotSeparateFromProductTransformationBatch() {
        PlantsHarvestResponse harvest = PlantsHarvestResponse.builder()
                .id(42L)
                .lotNumber("LOT-2026-A-001")
                .farmId(7L)
                .blockCode("A")
                .varietyId(3L)
                .varietyName("Keitt")
                .harvestDate("2026-11-05")
                .quantityKg(125.5)
                .source("user_entry")
                .build();

        HarvestEvent event = service.toHarvestEvent(harvest, "PT-BATCH-42");

        assertEquals("PT-BATCH-42", event.getBatchId(),
                "option B: the transformation lot keeps its own identifier");
        assertEquals("LOT-2026-A-001", event.getLotNumber(),
                "option B: the Plants lot number is stored in its own field");
        assertNull(event.getHarvestId(), "harvestId must stay a Product Transformation identifier");
        assertEquals(42L, event.getPlantsHarvestId());
        assertEquals(MangoVariety.KEITT, event.getMangoVariety());
        assertEquals(LocalDate.of(2026, 11, 5), event.getHarvestDate());
        assertEquals("7", event.getFarmId());
        assertEquals("A", event.getBlockId());
        assertEquals(0, event.getHarvestQuantityKg().compareTo(java.math.BigDecimal.valueOf(125.5)));
    }

    @Test
    void mappingRejectsMissingBatchId() {
        PlantsHarvestResponse harvest = PlantsHarvestResponse.builder()
                .id(42L)
                .harvestDate("2026-11-05")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.toHarvestEvent(harvest, "  "));
        assertThrows(IllegalArgumentException.class, () -> service.toHarvestEvent(harvest, null));
    }

    @Test
    void httpBindingAcceptsThePlantsSpelling() {
        assertEquals(MangoVariety.KEITT, MangoVariety.fromJson("Keitt"));
        assertEquals(MangoVariety.KEITT, MangoVariety.fromJson("keitt"));
        assertEquals(MangoVariety.KEITT, MangoVariety.fromJson("KEITT"));
        assertEquals(MangoVariety.AMÉLIE, MangoVariety.fromJson("Amélie"));
        assertEquals(MangoVariety.AMÉLIE, MangoVariety.fromJson("Amelie"));
        assertEquals(MangoVariety.TOMMY, MangoVariety.fromJson("Tommy Atkins"));
        assertEquals(MangoVariety.OTHER, MangoVariety.fromJson("other"));
        assertNull(MangoVariety.fromJson(null));
    }

    @Test
    void httpBindingRejectsAnUnknownVariety() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> MangoVariety.fromJson("Alphonso"));

        assertEquals("Unknown mango variety: 'Alphonso'. Expected one of KEITT, KENT, TOMMY, AMÉLIE, OTHER",
                error.getMessage());
    }

    @Test
    @DisplayName("an unknown Plants harvest is a 404, not a 500")
    void unknownPlantsHarvestIsNotFound() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(anyString(), eq(PlantsHarvestResponse.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

        PlantsIntegrationService remote = new PlantsIntegrationService(restTemplate);
        ReflectionTestUtils.setField(remote, "plantsApiBaseUrl", "http://plants.test");

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> remote.getHarvestById(999_999L));

        assertEquals("Plants harvest not found: 999999", error.getMessage());
    }
}
