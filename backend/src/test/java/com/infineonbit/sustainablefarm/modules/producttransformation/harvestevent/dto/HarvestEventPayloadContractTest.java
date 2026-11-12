package com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service.DtoMapper;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto.request.HarvestEventCreateRequest;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto.response.HarvestEventResponse;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.HarvestEvent.MangoVariety;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the harvest event API payload.
 *
 * <p>Three defects reported by the Plants team are pinned here: the variety
 * spelling accepted on write, the 400 (not 500) for an unusable payload, and the
 * {@code plantsHarvestId} / {@code lotNumber} round-trip.
 */
class HarvestEventPayloadContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final DtoMapper dtoMapper = new DtoMapper();

    private static final String BODY = """
            {
              "harvestId": "H-T1",
              "batchId": "PT-BATCH-1",
              "harvestDate": "2026-06-20",
              "mangoVariety": "%s",
              "farmId": "1",
              "blockId": "A",
              "harvestQuantityKg": 7500,
              "qualityGrade": "A",
              "lotNumber": "LOT-2026-A-001",
              "plantsHarvestId": 1
            }
            """;

    @Test
    @DisplayName("Keitt, keitt and Amélie are accepted on write")
    void acceptsPlantsVarietySpelling() throws Exception {
        assertEquals(MangoVariety.KEITT,
                objectMapper.readValue(BODY.formatted("Keitt"), HarvestEventCreateRequest.class).getMangoVariety());
        assertEquals(MangoVariety.KEITT,
                objectMapper.readValue(BODY.formatted("keitt"), HarvestEventCreateRequest.class).getMangoVariety());
        assertEquals(MangoVariety.AMÉLIE,
                objectMapper.readValue(BODY.formatted("Amélie"), HarvestEventCreateRequest.class).getMangoVariety());
        assertEquals(MangoVariety.KEITT,
                objectMapper.readValue(BODY.formatted("KEITT"), HarvestEventCreateRequest.class).getMangoVariety());
    }

    @Test
    @DisplayName("an unknown variety is rejected, not silently mapped to OTHER")
    void rejectsUnknownVariety() {
        JsonMappingException error = assertThrows(JsonMappingException.class,
                () -> objectMapper.readValue(BODY.formatted("Alphonso"), HarvestEventCreateRequest.class));

        assertTrue(rootCauseMessage(error).contains("Unknown mango variety: 'Alphonso'"),
                rootCauseMessage(error));
    }

    @Test
    @DisplayName("lotNumber and plantsHarvestId survive entity -> response mapping")
    void mapsPlantsIdentityBothWays() throws Exception {
        HarvestEventCreateRequest request =
                objectMapper.readValue(BODY.formatted("Keitt"), HarvestEventCreateRequest.class);

        HarvestEvent entity = dtoMapper.toEntity(request);
        assertEquals("LOT-2026-A-001", entity.getLotNumber());
        assertEquals(1L, entity.getPlantsHarvestId());

        HarvestEventResponse response = dtoMapper.toResponse(entity);
        assertEquals("LOT-2026-A-001", response.getLotNumber());
        assertEquals(1L, response.getPlantsHarvestId());
    }

    @Test
    @DisplayName("a payload without Plants identity still maps to null, not empty values")
    void keepsPlantsIdentityOptional() throws Exception {
        String body = """
                {
                  "harvestId": "H-T2",
                  "batchId": "PT-BATCH-2",
                  "harvestDate": "2026-06-21",
                  "mangoVariety": "KEITT",
                  "farmId": "1",
                  "blockId": "A",
                  "harvestQuantityKg": 100,
                  "qualityGrade": "A"
                }
                """;

        HarvestEventCreateRequest request = objectMapper.readValue(body, HarvestEventCreateRequest.class);
        HarvestEventResponse response = dtoMapper.toResponse(dtoMapper.toEntity(request));

        assertNull(response.getLotNumber());
        assertNull(response.getPlantsHarvestId());
    }

    private static String rootCauseMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }
}
