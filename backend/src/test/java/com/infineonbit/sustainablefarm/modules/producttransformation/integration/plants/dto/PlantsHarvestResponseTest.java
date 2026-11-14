package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the Plants harvest proxy payload.
 *
 * <p>These pin the two reasons the lot number used to disappear: the field was
 * not declared, so Jackson dropped it on read, and the four proxy routes could
 * not return what they never received.
 */
class PlantsHarvestResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("the Plants lot number is read from the payload")
    void readsLotNumber() throws Exception {
        String payload = """
                {
                  "id": 1,
                  "lotNumber": "LOT-2026-A-001",
                  "farmId": null,
                  "blockCode": "A",
                  "varietyId": 1,
                  "varietyName": "Keitt",
                  "harvestDate": "2026-06-20",
                  "quantityKg": 7500.0,
                  "source": "user_entry",
                  "lastUpdated": "2026-11-10T11:20:44Z"
                }
                """;

        PlantsHarvestResponse harvest = objectMapper.readValue(payload, PlantsHarvestResponse.class);

        assertEquals("LOT-2026-A-001", harvest.getLotNumber());
        assertEquals(1L, harvest.getId());
        assertEquals("A", harvest.getBlockCode());
        assertEquals("Keitt", harvest.getVarietyName());
    }

    @Test
    @DisplayName("the Plants lot number is returned by the proxy routes")
    void writesLotNumber() throws Exception {
        PlantsHarvestResponse harvest = PlantsHarvestResponse.builder()
                .id(1L)
                .lotNumber("LOT-2026-A-001")
                .blockCode("A")
                .build();

        String json = objectMapper.writeValueAsString(harvest);

        assertTrue(json.contains("\"lotNumber\":\"LOT-2026-A-001\""), json);
    }
}
