package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.infineonbit.sustainablefarm.modules.producttransformation.block.ProductTransformationBlocks;
import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.VarietyCatalogService;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto.PlantsHarvestResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.variety.MangoVarietyCodes;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the Plants -> Product Transformation harvest mapping.
 */
class PlantsIntegrationServiceTest {

    private final MangoVarietyCodes codes = codes();
    private final ProductTransformationBlocks blocks = mock(ProductTransformationBlocks.class);
    private final PlantsIntegrationService service = new PlantsIntegrationService(null, codes, blocks);

    private static MangoVarietyCodes codes() {
        VarietyReferenceRepository repository = mock(VarietyReferenceRepository.class);
        when(repository.findAll()).thenReturn(VarietyCatalogTestData.defaults());
        return new MangoVarietyCodes(new VarietyCatalogService(repository));
    }

    @Test
    void resolvesKeittCaseInsensitively() {
        assertEquals("KEITT", codes.keyOfPlantsName("Keitt"));
        assertEquals("KEITT", codes.keyOfPlantsName("keitt"));
        assertEquals("KEITT", codes.keyOfPlantsName("KEITT"));
    }

    @Test
    void resolvesAccentedAndMixedCaseVarieties() {
        assertEquals("AMELIE", codes.keyOfPlantsName("Amélie"));
        assertEquals("AMELIE", codes.keyOfPlantsName("Amelie"));
        assertEquals("KENT", codes.keyOfPlantsName("kent"));
        assertEquals("TOMMY", codes.keyOfPlantsName("Tommy Atkins"));
        assertEquals("TOMMY", codes.keyOfPlantsName("Tommy"));
    }

    @Test
    void unknownVarietyFallsBackToOther() {
        assertEquals("OTHER", codes.keyOfPlantsName(null));
        assertEquals("OTHER", codes.keyOfPlantsName("Alphonso"));
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

        when(blocks.resolve("7", "A")).thenReturn(11L);

        HarvestEvent event = service.toHarvestEvent(harvest, "LOT-2026-0001");

        assertEquals("LOT-2026-0001", event.getBatchId(), "original lot must be preserved");
        assertNull(event.getHarvestId(), "harvestId must stay a Product Transformation identifier");
        assertEquals(42L, event.getPlantsHarvestId());
        assertEquals("KEITT", event.getVarietyKey());
        assertEquals(LocalDate.of(2026, 11, 5), event.getHarvestDate());
        assertEquals(11L, event.getFarmBlockId(), "the Plants block code designates the same farm block");
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
