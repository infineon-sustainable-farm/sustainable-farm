package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Plants harvests read by Product Transformation under
 * {@code /api/harvest-events/plants}, now in the same process: the same fields
 * as {@code GET /api/plants/harvests} without the lot number, and the same
 * errors as when Plants was called over HTTP.
 *
 * <p>Farm 31 and block P22 are used by no other test.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PlantsHarvestApiTest {

    private static final int FARM = 31;
    private static final String BLOCK = "P22";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private long harvestId;

    @BeforeEach
    void harvestOfFarm31() throws Exception {
        String existing = mockMvc.perform(get("/api/plants/harvests").param("farmId", String.valueOf(FARM)))
                .andReturn().getResponse().getContentAsString();
        JsonNode harvests = objectMapper.readTree(existing);
        if (!harvests.isEmpty()) {
            harvestId = harvests.get(0).get("id").asLong();
            return;
        }
        mockMvc.perform(post("/api/plants/plantings").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId": %d, "blockCode": "%s", "varietyName": "Keitt",
                                 "plantingDate": "2020-01-10", "treeCount": 10}
                                """.formatted(FARM, BLOCK)))
                .andExpect(status().isCreated());
        String created = mockMvc.perform(post("/api/plants/harvests").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId": %d, "blockCode": "%s", "varietyName": "keitt",
                                 "harvestDate": "2026-06-01", "quantityKg": 120.5}
                                """.formatted(FARM, BLOCK)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        harvestId = objectMapper.readTree(created).get("id").asLong();
    }

    @Test
    void oneHarvest_isSentBackWithTheFieldsOfPlants() throws Exception {
        mockMvc.perform(get("/api/harvest-events/plants/{id}", harvestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(harvestId))
                .andExpect(jsonPath("$.farmId").value(FARM))
                .andExpect(jsonPath("$.blockCode").value(BLOCK))
                .andExpect(jsonPath("$.varietyId").isNumber())
                .andExpect(jsonPath("$.varietyName").value("Keitt"))
                .andExpect(jsonPath("$.harvestDate").value("2026-06-01"))
                .andExpect(jsonPath("$.quantityKg").value(120.5))
                .andExpect(jsonPath("$.source").value("user_entry"))
                .andExpect(jsonPath("$.lastUpdated").isString())
                .andExpect(jsonPath("$.lotNumber").doesNotExist());
    }

    @Test
    void theLists_filterByFarmAndBlock() throws Exception {
        mockMvc.perform(get("/api/harvest-events/plants/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) harvestId)));
        mockMvc.perform(get("/api/harvest-events/plants/farm/{farmId}", FARM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(harvestId));
        mockMvc.perform(get("/api/harvest-events/plants/block/{blockCode}", BLOCK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].blockCode").value(BLOCK));
        mockMvc.perform(get("/api/harvest-events/plants/farm/{farmId}", 9_999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anUnknownHarvest_isStillA500_asWhenPlantsAnswered404OverHttp() throws Exception {
        mockMvc.perform(get("/api/harvest-events/plants/{id}", 987_654_321L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }

    @Test
    void aFarmIdBeyondTheRangeOfPlants_isStillA500() throws Exception {
        mockMvc.perform(get("/api/harvest-events/plants/farm/{farmId}", 99_999_999_999L))
                .andExpect(status().isInternalServerError());
    }
}
