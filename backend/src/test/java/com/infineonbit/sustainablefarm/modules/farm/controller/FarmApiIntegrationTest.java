package com.infineonbit.sustainablefarm.modules.farm.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void zoneIsReadWithItsBlockAndFarmAndTheLinkSetsTheBlockCode() throws Exception {
        long farmId = idOf(post("/api/farms"), "{\"name\":\"Referential Farm\"}");
        long fieldId = idOf(post("/api/fields"),
                "{\"farmId\":%d,\"name\":\"Referential Field\",\"areaHectares\":2}".formatted(farmId));
        long zoneId = idOf(post("/api/zones"),
                "{\"fieldId\":%d,\"name\":\"Referential Zone\",\"areaHectares\":1}".formatted(fieldId));
        String code = "R" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        mockMvc.perform(post("/api/water/zone-block-mappings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\":%d,\"blockCode\":\"%s\",\"farmId\":%d}".formatted(zoneId, code.toLowerCase(), farmId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(zoneId))
                .andExpect(jsonPath("$.blockCode").value(code));

        ZoneDto zone = farmDirectory.findZone(zoneId).orElseThrow();
        assertEquals(fieldId, zone.blockId());
        assertEquals(farmId, zone.farmId());
        assertEquals(code, zone.blockCode());
        assertTrue(farmDirectory.findBlockByCode(code).isPresent());

        mockMvc.perform(get("/api/zones/{id}", zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fieldId").value(fieldId));

        mockMvc.perform(delete("/api/water/zone-block-mappings/{id}", zoneId)).andExpect(status().isNoContent());
        assertTrue(farmDirectory.findBlockByCode(code).isEmpty());
    }

    @Test
    void aReferencedZoneOrFarmCannotBeDeleted() throws Exception {
        long farmId = idOf(post("/api/farms"), "{\"name\":\"Referenced Farm\"}");
        long fieldId = idOf(post("/api/fields"),
                "{\"farmId\":%d,\"name\":\"Referenced Field\",\"areaHectares\":2}".formatted(farmId));
        long zoneId = idOf(post("/api/zones"),
                "{\"fieldId\":%d,\"name\":\"Referenced Zone\",\"areaHectares\":1}".formatted(fieldId));
        mockMvc.perform(post("/api/irrigations").contentType(MediaType.APPLICATION_JSON).content("""
                {"zoneId":%d,"startTime":"2026-09-16T12:00:00Z","durationMinutes":30,"waterQuantityLiters":900,"createdBy":"%s"}
                """.formatted(zoneId, SystemUsers.IOT_SYSTEM_USER_ID))).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/zones/{id}", zoneId)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/fields/{id}", fieldId)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/farms/{id}", farmId)).andExpect(status().isConflict());
    }

    private long idOf(MockHttpServletRequestBuilder request, String body) throws Exception {
        MvcResult result = mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Object id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return ((Number) id).longValue();
    }
}
