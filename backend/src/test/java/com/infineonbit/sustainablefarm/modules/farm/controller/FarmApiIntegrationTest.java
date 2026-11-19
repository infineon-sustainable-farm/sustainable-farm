package com.infineonbit.sustainablefarm.modules.farm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.infineonbit.sustainablefarm.modules.farm.api.FarmDirectory;
import com.infineonbit.sustainablefarm.modules.farm.api.ZoneDto;
import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Site referential: numeric ids, Zone -> Block -> Farm read through farm.api, and the foreign keys. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FarmApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FarmDirectory farmDirectory;

    @Autowired
    private SystemUsers systemUsers;

    @Test
    void zoneIsReadWithItsBlockAndFarmAndTheLinkSetsTheBlockCode() throws Exception {
        long farmId = idOf(post("/api/farm/farms"), "{\"name\":\"Referential Farm\"}");
        long fieldId = idOf(post("/api/farm/fields"),
                "{\"farmId\":%d,\"name\":\"Referential Field\",\"areaHectares\":2}".formatted(farmId));
        long zoneId = idOf(post("/api/farm/zones"),
                "{\"fieldId\":%d,\"name\":\"Referential Zone\",\"areaHectares\":1}".formatted(fieldId));
        String code = "R" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        mockMvc.perform(post("/api/farm/zone-block-mappings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\":%d,\"blockCode\":\"%s\",\"farmId\":%d}".formatted(zoneId, code.toLowerCase(), farmId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(zoneId))
                .andExpect(jsonPath("$.blockCode").value(code));

        ZoneDto zone = farmDirectory.findZone(zoneId).orElseThrow();
        assertEquals(fieldId, zone.blockId());
        assertEquals(farmId, zone.farmId());
        assertEquals(code, zone.blockCode());
        assertTrue(farmDirectory.findBlockByCode(code).isPresent());

        mockMvc.perform(get("/api/farm/zones/{id}", zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(fieldId));

        mockMvc.perform(delete("/api/farm/zone-block-mappings/{id}", zoneId)).andExpect(status().isNoContent());
        assertTrue(farmDirectory.findBlockByCode(code).isEmpty());
    }

    @Test
    void aReferencedZoneOrFarmCannotBeDeleted() throws Exception {
        long farmId = idOf(post("/api/farm/farms"), "{\"name\":\"Referenced Farm\"}");
        long fieldId = idOf(post("/api/farm/fields"),
                "{\"farmId\":%d,\"name\":\"Referenced Field\",\"areaHectares\":2}".formatted(farmId));
        long zoneId = idOf(post("/api/farm/zones"),
                "{\"fieldId\":%d,\"name\":\"Referenced Zone\",\"areaHectares\":1}".formatted(fieldId));
        mockMvc.perform(post("/api/water-supply/irrigations").contentType(MediaType.APPLICATION_JSON).content("""
                {"zoneId":%d,"startTime":"2026-09-16T12:00:00Z","durationMinutes":30,"waterQuantityLiters":900,"createdBy":%d}
                """.formatted(zoneId, systemUsers.iotSystemUserId()))).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/farm/zones/{id}", zoneId)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/farm/fields/{id}", fieldId)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/farm/farms/{id}", farmId)).andExpect(status().isConflict());
    }

    @Test
    void aPlantingOnAnUnknownCodeIsRefusedAndCreatesNoBlock() throws Exception {
        long farmId = idOf(post("/api/farm/farms"), "{\"name\":\"Planting Farm\"}");
        String code = "N" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        mockMvc.perform(post("/api/plants/plantings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"farmId":%d,"blockCode":"%s","varietyName":"Keitt","plantingDate":"2023-09-15","treeCount":10}
                        """.formatted(farmId, code.toLowerCase())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/product-transformation/batches").contentType(MediaType.APPLICATION_JSON).content("""
                        {"batchId":"LOT-%s","farmId":"%d","blockId":"%s","harvestDate":"2026-01-02","mangoVariety":"KEITT","harvestQuantityKg":10}
                        """.formatted(code, farmId, code)))
                .andExpect(status().isBadRequest());

        assertTrue(farmDirectory.findBlockByCode(code).isEmpty());
        assertTrue(farmDirectory.listBlocksOfFarm(farmId).isEmpty());
    }

    @Test
    void aFieldGetsItsPlantsCodeFromTheFieldFormAndPlantsPicksIt() throws Exception {
        long farmId = idOf(post("/api/farm/farms"), "{\"name\":\"Coded Farm\"}");
        String code = "F" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long fieldId = idOf(post("/api/farm/fields"),
                "{\"farmId\":%d,\"name\":\"Coded Field\",\"areaHectares\":2,\"code\":\" %s \"}"
                        .formatted(farmId, code.toLowerCase()));

        mockMvc.perform(get("/api/farm/fields/{id}", fieldId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code));
        mockMvc.perform(post("/api/farm/fields").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"farmId\":%d,\"name\":\"Twin\",\"areaHectares\":1,\"code\":\"%s\"}".formatted(farmId, code)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/farm/fields").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"farmId\":%d,\"name\":\"Bad\",\"areaHectares\":1,\"code\":\"A-1\"}".formatted(farmId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/plants/plantings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"farmId":%d,"blockCode":"%s","varietyName":"Keitt","plantingDate":"2023-09-15","treeCount":10}
                        """.formatted(farmId, code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockCode").value(code));

        mockMvc.perform(put("/api/farm/fields/{id}", fieldId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Coded Field\",\"code\":\"\"}"))
                .andExpect(status().isConflict());
        assertEquals(code, farmDirectory.findBlock(fieldId).orElseThrow().code());
    }

    @Test
    void aFieldCodeIsSetAndClearedByAnUpdate() throws Exception {
        long farmId = idOf(post("/api/farm/farms"), "{\"name\":\"Recoded Farm\"}");
        long fieldId = idOf(post("/api/farm/fields"),
                "{\"farmId\":%d,\"name\":\"Plain Field\",\"areaHectares\":2}".formatted(farmId));
        String code = "G" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        mockMvc.perform(put("/api/farm/fields/{id}", fieldId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plain Field\",\"code\":\"%s\"}".formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code));
        mockMvc.perform(put("/api/farm/fields/{id}", fieldId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plain Field\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code));
        mockMvc.perform(put("/api/farm/fields/{id}", fieldId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plain Field\",\"code\":\" \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void unlinkingAZoneWhoseBlockIsUsedByPlantsIsRefusedAndKeepsTheCode() throws Exception {
        long farmId = idOf(post("/api/farm/farms"), "{\"name\":\"Used Block Farm\"}");
        long fieldId = idOf(post("/api/farm/fields"),
                "{\"farmId\":%d,\"name\":\"Used Field\",\"areaHectares\":2}".formatted(farmId));
        long zoneId = idOf(post("/api/farm/zones"),
                "{\"fieldId\":%d,\"name\":\"Used Zone\",\"areaHectares\":1}".formatted(fieldId));
        String code = "U" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        mockMvc.perform(post("/api/farm/zone-block-mappings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\":%d,\"blockCode\":\"%s\",\"farmId\":%d}".formatted(zoneId, code, farmId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/plants/plantings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"blockCode":"%s","varietyName":"Kent","plantingDate":"2023-09-15","treeCount":10}
                        """.formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.farmId").value(farmId));

        mockMvc.perform(delete("/api/farm/zone-block-mappings/{id}", zoneId)).andExpect(status().isConflict());

        assertEquals(fieldId, farmDirectory.findBlockByCode(code).orElseThrow().id());
        assertEquals(code, farmDirectory.findZone(zoneId).orElseThrow().blockCode());
    }

    private long idOf(MockHttpServletRequestBuilder request, String body) throws Exception {
        MvcResult result = mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Object id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return ((Number) id).longValue();
    }
}
