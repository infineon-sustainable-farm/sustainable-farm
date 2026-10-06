package com.infineonbit.sustainablefarm.modules.watersupply.iot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class IotTelemetryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void outOfRangeReservoirLevelsAreRejectedWithoutChangingTheSource() throws Exception {
        String farmId = createFarm();
        String sourceId = createSource(farmId);
        try {
            mockMvc.perform(post("/api/iot/telemetry")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [
                                      {"device_id":"level-node","type":"level","source_id":"%s","values":{"level_percent":120}},
                                      {"device_id":"level-node","type":"level","source_id":"%s","values":{"level_liters":-10}}
                                    ]
                                    """.formatted(sourceId, sourceId)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$[0].status").value("rejected"))
                    .andExpect(jsonPath("$[1].status").value("rejected"));

            mockMvc.perform(get("/api/water/sources/{sourceId}", sourceId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentLevelLiters").value(2500.0));
        } finally {
            mockMvc.perform(delete("/api/water/sources/{sourceId}", sourceId));
            mockMvc.perform(delete("/api/farms/{farmId}", farmId));
        }
    }

    private String createFarm() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"IoT Level Farm","areaHectares":2.0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createSource(String farmId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/water/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId":"%s","name":"IoT Level Tank","type":"tank","capacityLiters":5000,"currentLevelLiters":2500}
                                """.formatted(farmId)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}