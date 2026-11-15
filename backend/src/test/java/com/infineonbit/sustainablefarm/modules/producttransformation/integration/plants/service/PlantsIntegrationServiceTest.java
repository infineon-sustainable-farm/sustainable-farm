package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void mappingPreservesOriginalLotAndPlantsId() {
        PlantsHarvestResponse harvest = PlantsHarvestResponse.builder()
                .id(42L)
                .farmId(7L)
                .blockCode("A")
                .varietyId(3L)
                .varietyName("Keitt")
                .harvestDate("2026-11-05")
                .quantityKg(125.5)
                .source("user_entry")
                .build();

        HarvestEvent event = service.toHarvestEvent(harvest, "LOT-2026-0001");

        assertEquals("LOT-2026-0001", event.getBatchId(), "original lot must be preserved");
        assertNull(event.getHarvestId(), "harvestId must stay a Product Transformation identifier");
        assertEquals(42L, event.getPlantsHarvestId());
        assertEquals(MangoVariety.KEITT, event.getMangoVariety());
        assertEquals(LocalDate.of(2026, 11, 5), event.getHarvestDate());
        assertEquals("7", event.getFarmId());
        assertEquals("A", event.getBlockId());
        assertEquals(0, event.getHarvestQuantityKg().compareTo(java.math.BigDecimal.valueOf(125.5)));
    }

    @Test
    void mappingRejectsMissingOriginalLot() {
        PlantsHarvestResponse harvest = PlantsHarvestResponse.builder()
                .id(42L)
                .harvestDate("2026-11-05")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.toHarvestEvent(harvest, "  "));
        assertThrows(IllegalArgumentException.class, () -> service.toHarvestEvent(harvest, null));
    }
}
