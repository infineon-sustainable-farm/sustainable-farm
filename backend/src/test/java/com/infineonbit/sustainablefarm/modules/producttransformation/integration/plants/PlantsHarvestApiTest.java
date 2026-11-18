package com.infineonbit.sustainablefarm.modules.producttransformation.integration.plants;

import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import java.util.Optional;
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
 * <p>Block P22 is used by no other test; its farm is created by the first test
 * that runs, and found again through the farm referential by the next ones.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PlantsHarvestApiTest {

    private static final String BLOCK = "P22";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FarmDirectory farmDirectory;

    private long farm;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private long harvestId;

    @BeforeEach
    void harvestOfBlockP22() throws Exception {
        Optional<BlockDto> block = farmDirectory.findBlockByCode(BLOCK);
        if (block.isPresent()) {
            farm = block.get().farmId();
            String existing = mockMvc.perform(get("/api/plants/harvests").param("blockCode", BLOCK))
                    .andReturn().getResponse().getContentAsString();
            JsonNode harvests = objectMapper.readTree(existing);
            harvestId = harvests.get(0).get("id").asLong();
            return;
        }
        String createdFarm = mockMvc.perform(post("/api/farm/farms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plants Harvest API Farm\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        farm = objectMapper.readTree(createdFarm).get("id").asLong();
        mockMvc.perform(post("/api/plants/plantings").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId": %d, "blockCode": "%s", "varietyName": "Keitt",
                                 "plantingDate": "2020-01-10", "treeCount": 10}
                                """.formatted(farm, BLOCK)))
                .andExpect(status().isCreated());
        String created = mockMvc.perform(post("/api/plants/harvests").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId": %d, "blockCode": "%s", "varietyName": "keitt",
                                 "harvestDate": "2026-06-01", "quantityKg": 120.5}
                                """.formatted(farm, BLOCK)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        harvestId = objectMapper.readTree(created).get("id").asLong();
    }

    @Test
    void oneHarvest_isSentBackWithTheFieldsOfPlants() throws Exception {
        mockMvc.perform(get("/api/harvest-events/plants/{id}", harvestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(harvestId))
                .andExpect(jsonPath("$.farmId").value(farm))
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
        mockMvc.perform(get("/api/harvest-events/plants/farm/{farmId}", farm))
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
    void anUnknownHarvest_isA404() throws Exception {
        mockMvc.perform(get("/api/harvest-events/plants/{id}", 987_654_321L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Plants harvest 987654321 not found"));
    }

    @Test
    void anUnknownLongFarmId_hasNoHarvest() throws Exception {
        mockMvc.perform(get("/api/harvest-events/plants/farm/{farmId}", 99_999_999_999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
