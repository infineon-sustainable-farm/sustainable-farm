package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDto;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WatersupplyApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonDirectory personDirectory;

    @Autowired
    private SystemUsers systemUsers;

    @Test
    void healthEndpointReturnsStableContract() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void farmsPaginationReturnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/farms").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void missingFarmReturnsNormalizedNotFoundError() throws Exception {
        mockMvc.perform(get("/api/farms/{farmId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Farm not found"));
    }

        @Test
        @Transactional
        void farmFieldZoneCrudPreservesParentRelations() throws Exception {
        MvcResult farmResult = mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andReturn();
        org.junit.jupiter.api.Assertions.assertNotNull(farmResult.getResponse().getContentAsString());

        MvcResult createdFarm = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/farms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Integration Farm","description":"API test","areaHectares":12.5}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Integration Farm"))
            .andReturn();
        String farmId = String.valueOf((Object) JsonPath.read(createdFarm.getResponse().getContentAsString(), "$.id"));

        MvcResult createdField = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/fields")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","name":"Integration Field","areaHectares":2.5}
                    """.formatted(farmId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.farmId").value(farmId))
            .andReturn();
        String fieldId = String.valueOf((Object) JsonPath.read(createdField.getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fieldId":"%s","name":"Integration Zone","areaHectares":1.0}
                    """.formatted(fieldId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fieldId").value(fieldId));
        }

        @Test
        void creatingFieldWithUnknownFarmReturnsNotFound() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/fields")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"999999","name":"Invalid Field","areaHectares":1.0}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @Transactional
        void waterCrudRejectsSourceFromAnotherFarm() throws Exception {
        String farmId = createFarm("Water Farm");
        String otherFarmId = createFarm("Other Water Farm");

        MvcResult createdSource = mockMvc.perform(post("/api/water/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","name":"Integration Tank","type":"tank","capacityLiters":10000,"currentLevelLiters":7500}
                    """.formatted(farmId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.farmId").value(farmId))
            .andReturn();
        String sourceId = String.valueOf((Object) JsonPath.read(createdSource.getResponse().getContentAsString(), "$.id"));

        MvcResult consumption = mockMvc.perform(post("/api/water/consumption")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","sourceId":"%s","consumptionLiters":450,"consumptionDate":"2026-09-16T09:00:00Z"}
                    """.formatted(farmId, sourceId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceId").value(sourceId))
            .andReturn();
        String consumptionId = String.valueOf((Object) JsonPath.read(consumption.getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(put("/api/water/consumption/{consumptionId}", consumptionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"consumptionLiters":500}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consumptionLiters").value(500.0));

        mockMvc.perform(post("/api/water/consumption")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","sourceId":"%s","consumptionLiters":250,"consumptionDate":"2026-09-16T10:00:00Z"}
                    """.formatted(otherFarmId, sourceId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(delete("/api/water/consumption/{consumptionId}", consumptionId))
            .andExpect(status().isNoContent());
        }

        @Test
        @Transactional
        void irrigationStartStopCompletesStartedLog() throws Exception {
        Long userId = createUser();
        String farmId = createFarm("Irrigation Farm");
        String fieldId = createField(farmId, "Irrigation Field");
        String zoneId = createZone(fieldId, "Irrigation Zone");

        MvcResult schedule = mockMvc.perform(post("/api/irrigations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"zoneId":"%s","startTime":"2026-09-16T12:00:00Z","durationMinutes":30,"waterQuantityLiters":900,"createdBy":%d}
                    """.formatted(zoneId, userId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.zoneId").value(zoneId))
            .andReturn();
        String scheduleId = String.valueOf((Object) JsonPath.read(schedule.getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(post("/api/irrigations/{scheduleId}/start", scheduleId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.scheduleId").value(scheduleId))
            .andExpect(jsonPath("$.status").value("started"));

        mockMvc.perform(post("/api/irrigations/{scheduleId}/stop", scheduleId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.scheduleId").value(scheduleId))
            .andExpect(jsonPath("$.status").value("completed"))
            .andExpect(jsonPath("$.waterUsedLiters").value(900.0));
        }

        @Test
        void systemUserIsSeededSoAutomaticNotificationsCanBeCreated() throws Exception {
        // The IoT technical person is created at startup by SystemUserSeeder, in every profile.
        // Without it, creating automatic alerts failed with 404 "User not found".
        PersonDto iot = personDirectory.findByEmail(SystemUsers.IOT_SYSTEM_EMAIL).orElseThrow(
                () -> new AssertionError("The IoT technical person must be seeded by SystemUserSeeder at startup"));
        org.junit.jupiter.api.Assertions.assertEquals(iot.id(), systemUsers.iotSystemUserId());
        org.junit.jupiter.api.Assertions.assertFalse(iot.active());

        MvcResult result = mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":%d,"title":"Sensor alert","message":"Integration test","type":"warning"}
                    """.formatted(iot.id())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(iot.id()))
            .andReturn();

        String notificationId = String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId))
            .andExpect(status().isNoContent());
        }

        @Test
        void unknownUrlReturnsNotFoundInsteadOfInternalError() throws Exception {
        mockMvc.perform(get("/api/route-that-does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        void unsupportedHttpMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(patch("/api/farms"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        }

        @Test
        void telemetryWithoutDeviceIdIsRejectedWithoutFailingTheRequest() throws Exception {
        mockMvc.perform(post("/api/iot/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$[0].status").value("rejected"))
            .andExpect(jsonPath("$[0].message").value("Missing 'device_id' field"));
        }

        @Test
        void invalidTelemetryDoesNotBreakFollowingItemsInBatch() throws Exception {
        // No @Transactional here: each item is ingested in its own transaction, so the
        // source must really exist (commit) before the ingestion.
        String farmId = createFarm("Telemetry Batch Farm");
        String sourceId = createSource(farmId, "Telemetry Batch Tank");
        try {
            mockMvc.perform(post("/api/iot/telemetry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        [{},
                         {"device_id":"integration-node","type":"level","source_id":"%s","values":{"level_liters":1234}}]
                        """.formatted(sourceId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$[0].status").value("rejected"))
                .andExpect(jsonPath("$[1].status").value("processed"))
                .andExpect(jsonPath("$[1].sourceId").value(sourceId));
        } finally {
            mockMvc.perform(delete("/api/water/sources/{sourceId}", sourceId));
            mockMvc.perform(delete("/api/farms/{farmId}", farmId));
        }
        }

        @Test
        @Transactional
        void zoneBlockMappingCrudExposesResolverData() throws Exception {
        String farmId = createFarm("Block Mapping Farm");
        String fieldId = createField(farmId, "Block Mapping Field");
        String zoneId = createZone(fieldId, "Block Mapping Zone");

        MvcResult created = mockMvc.perform(post("/api/water/zone-block-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"zoneId":"%s","blockCode":"a","farmId":"%s"}
                    """.formatted(zoneId, farmId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.zoneId").value(zoneId))
            .andExpect(jsonPath("$.blockCode").value("A"))
            .andExpect(jsonPath("$.farmId").value(farmId))
            .andReturn();
        String mappingId = String.valueOf((Object) JsonPath.read(created.getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(get("/api/water/zone-block-mappings")
                .param("farmId", farmId)
                .param("blockCode", "A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(mappingId));

        mockMvc.perform(get("/api/water/zone-block-mappings/{mappingId}", mappingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zoneId").value(zoneId));

        mockMvc.perform(put("/api/water/zone-block-mappings/{mappingId}", mappingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"blockCode":"B"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.blockCode").value("B"));

        mockMvc.perform(delete("/api/water/zone-block-mappings/{mappingId}", mappingId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/water/zone-block-mappings/{mappingId}", mappingId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @Transactional
        void zoneBlockMappingRejectsInvalidReferencesAndDuplicates() throws Exception {
        String farmId = createFarm("Invalid Mapping Farm");
        String fieldId = createField(farmId, "Invalid Mapping Field");
        String zoneId = createZone(fieldId, "Invalid Mapping Zone");
        String otherFarmId = createFarm("Other Mapping Farm");

        mockMvc.perform(post("/api/water/zone-block-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"zoneId":"%s","blockCode":"C","farmId":"%s"}
                    """.formatted(zoneId, otherFarmId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(post("/api/water/zone-block-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"zoneId":"999999","blockCode":"C","farmId":"%s"}
                    """.formatted(farmId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(post("/api/water/zone-block-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"zoneId":"%s","blockCode":"C","farmId":"%s"}
                    """.formatted(zoneId, farmId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/water/zone-block-mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"zoneId":"%s","blockCode":"C","farmId":"%s"}
                    """.formatted(zoneId, farmId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
        }

        private String createSource(String farmId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/water/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","name":"%s","type":"tank","capacityLiters":5000,"currentLevelLiters":2500}
                    """.formatted(farmId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        }

        private String createFarm(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/farms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s","areaHectares":12.5}
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        }

        private String createField(String farmId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/fields")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","name":"%s","areaHectares":3.5}
                    """.formatted(farmId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        }

        private String createZone(String fieldId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fieldId":"%s","name":"%s","areaHectares":1.5}
                    """.formatted(fieldId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        }

        private Long createUser() {
        return personDirectory.findOrCreate(new PersonDetails("Integration User",
                "integration-" + UUID.randomUUID() + "@example.test", null, null), id -> true).id();
        }
}
