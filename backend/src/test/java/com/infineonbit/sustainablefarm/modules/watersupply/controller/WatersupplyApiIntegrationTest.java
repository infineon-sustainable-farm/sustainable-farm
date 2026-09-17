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
import com.infineonbit.sustainablefarm.modules.watersupply.entity.User;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class WatersupplyApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
        mockMvc.perform(get("/api/farms/{farmId}", UUID.randomUUID()))
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
        String farmId = JsonPath.read(createdFarm.getResponse().getContentAsString(), "$.id");

        MvcResult createdField = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/fields")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","name":"Integration Field","areaHectares":2.5}
                    """.formatted(farmId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.farmId").value(farmId))
            .andReturn();
        String fieldId = JsonPath.read(createdField.getResponse().getContentAsString(), "$.id");

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
                    {"farmId":"00000000-0000-0000-0000-000000000000","name":"Invalid Field","areaHectares":1.0}
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
        String sourceId = JsonPath.read(createdSource.getResponse().getContentAsString(), "$.id");

        MvcResult consumption = mockMvc.perform(post("/api/water/consumption")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","sourceId":"%s","consumptionLiters":450,"consumptionDate":"2026-09-16T09:00:00Z"}
                    """.formatted(farmId, sourceId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceId").value(sourceId))
            .andReturn();
        String consumptionId = JsonPath.read(consumption.getResponse().getContentAsString(), "$.id");

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
            .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mockMvc.perform(delete("/api/water/consumption/{consumptionId}", consumptionId))
            .andExpect(status().isNoContent());
        }

        @Test
        @Transactional
        void irrigationStartStopCompletesStartedLog() throws Exception {
        UUID userId = createUser();
        String farmId = createFarm("Irrigation Farm");
        String fieldId = createField(farmId, "Irrigation Field");
        String zoneId = createZone(fieldId, "Irrigation Zone");

        MvcResult schedule = mockMvc.perform(post("/api/irrigations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"zoneId":"%s","startTime":"2026-09-16T12:00:00Z","durationMinutes":30,"waterQuantityLiters":900,"createdBy":"%s"}
                    """.formatted(zoneId, userId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.zoneId").value(zoneId))
            .andReturn();
        String scheduleId = JsonPath.read(schedule.getResponse().getContentAsString(), "$.id");

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
        // Le compte technique IoT est cree par les migrations Flyway (V2/V4). Sans ce seed, la
        // creation des alertes automatiques echouait avec 404 "User not found".
        org.junit.jupiter.api.Assertions.assertTrue(
                userRepository.findById(SystemUsers.IOT_SYSTEM_USER_ID).isPresent(),
                "Le compte technique IoT doit etre seede par les migrations Flyway");

        MvcResult result = mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"%s","title":"Alerte capteur","message":"Test integration","type":"warning"}
                    """.formatted(SystemUsers.IOT_SYSTEM_USER_ID)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(SystemUsers.IOT_SYSTEM_USER_ID.toString()))
            .andReturn();

        String notificationId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId))
            .andExpect(status().isNoContent());
        }

        @Test
        void unknownUrlReturnsNotFoundInsteadOfInternalError() throws Exception {
        mockMvc.perform(get("/api/route-qui-nexiste-pas"))
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
            .andExpect(jsonPath("$[0].message").value("Champ 'device_id' manquant"));
        }

        @Test
        void invalidTelemetryDoesNotBreakFollowingItemsInBatch() throws Exception {
        // Pas de @Transactional ici : chaque item est ingere dans sa propre transaction, donc il
        // faut que la source existe reellement (commit) avant l'ingestion.
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

        private String createSource(String farmId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/water/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","name":"%s","type":"tank","capacityLiters":5000,"currentLevelLiters":2500}
                    """.formatted(farmId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        }

        private String createFarm(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/farms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s","areaHectares":12.5}
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        }

        private String createField(String farmId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/fields")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","name":"%s","areaHectares":3.5}
                    """.formatted(farmId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        }

        private String createZone(String fieldId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fieldId":"%s","name":"%s","areaHectares":1.5}
                    """.formatted(fieldId, name)))
            .andExpect(status().isCreated())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        }

        private UUID createUser() {
        User user = new User();
        user.setFirstName("Integration");
        user.setLastName("User");
        user.setEmail("integration-" + UUID.randomUUID() + "@example.test");
        user.setPasswordHash("not-used-in-test");
        user.setStatus(true);
        return userRepository.save(user).getId();
        }
}
