package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Water Supply on the conventions of docs/adr/0002 (T36): Long ids sent as JSON numbers, statuses
 * stored and sent as the same lower-case values, foreign keys between the module tables, and the
 * endpoints under {@code /api/water-supply}.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class WatersupplySchemaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private long farmId;
    private long zoneId;
    private long sourceId;

    @BeforeEach
    void createFarmZoneAndSource() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        farmId = idOf("/api/farm/farms", """
                {"name":"Schema farm %s","areaHectares":3}
                """.formatted(suffix));
        long fieldId = idOf("/api/farm/fields", """
                {"farmId":%d,"name":"Schema field","areaHectares":2}
                """.formatted(farmId));
        zoneId = idOf("/api/farm/zones", """
                {"fieldId":%d,"name":"Schema zone","areaHectares":1,"irrigationMethod":"drip"}
                """.formatted(fieldId));
        sourceId = idOf("/api/water-supply/sources", """
                {"farmId":%d,"name":"Schema tank","type":"tank","capacityLiters":1000,"currentLevelLiters":500}
                """.formatted(farmId));
    }

    @Test
    void scheduleAndLogUseNumericIdsAndKeepTheirLowerCaseStatuses() throws Exception {
        String created = mockMvc.perform(post("/api/water-supply/irrigations").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":%d,"startTime":"2026-06-01T06:00:00Z","durationMinutes":30,
                                 "waterQuantityLiters":200,"status":"Scheduled"}
                                """.formatted(zoneId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("scheduled"))
                .andReturn().getResponse().getContentAsString();
        long scheduleId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(post("/api/water-supply/irrigations/{id}/start", scheduleId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.status").value("started"));
        entityManager.flush();

        assertThat(jdbc.queryForObject("select status from irrigation_schedules where id = ?", String.class, scheduleId))
                .isEqualTo("running");
        assertThat(jdbc.queryForObject("select trigger_source from irrigation_schedules where id = ?", String.class,
                scheduleId)).isEqualTo("manual");
        assertThat(jdbc.queryForObject("select status from irrigation_logs where schedule_id = ?", String.class,
                scheduleId)).isEqualTo("started");
    }

    @Test
    void anUnknownStatusIsRefused() throws Exception {
        mockMvc.perform(post("/api/water-supply/irrigations").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":%d,"startTime":"2026-06-01T06:00:00Z","durationMinutes":30,
                                 "waterQuantityLiters":200,"status":"exploded"}
                                """.formatted(zoneId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anOutageKeepsItsScopeAndStatusValues() throws Exception {
        mockMvc.perform(post("/api/water-supply/outages").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"ZONE","scopeId":%d,"startAt":"2030-01-01T00:00:00Z",
                                 "endAt":"2030-01-01T04:00:00Z","reason":"pump"}
                                """.formatted(zoneId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.scope").value("zone"))
                .andExpect(jsonPath("$.status").value("planned"));
        entityManager.flush();

        assertThat(jdbc.queryForObject("select scope from water_unavailability where scope_id = ?", String.class, zoneId))
                .isEqualTo("zone");
    }

    @Test
    void aLogMustReferenceAnExistingSchedule() {
        assertThatThrownBy(() -> jdbc.update("insert into irrigation_logs (created_at, updated_at, schedule_id, "
                + "actual_start_time, water_used_liters, status) values (current_timestamp, current_timestamp, "
                + "987654321, current_timestamp, 0, 'started')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // One insert per test: on PostgreSQL a failed statement aborts the test transaction, so a second
    // insert in the same test would fail with "current transaction is aborted" instead of the FK error.
    @Test
    void aQualityTestMustReferenceAnExistingSource() {
        assertThatThrownBy(() -> jdbc.update("insert into water_quality_tests (created_at, updated_at, source_id) "
                + "values (current_timestamp, current_timestamp, 987654321)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aRainwaterHarvestMustReferenceAnExistingSource() {
        assertThatThrownBy(() -> jdbc.update("insert into rainwater_harvests (created_at, updated_at, source_id, "
                + "catchment_area_m2, rainfall_mm, runoff_coefficient, capture_date) values (current_timestamp, "
                + "current_timestamp, 987654321, 1, 1, 1, current_timestamp)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aConsumptionMustReferenceAnExistingSource() {
        assertThatThrownBy(() -> jdbc.update("insert into water_consumption (created_at, updated_at, farm_id, "
                + "source_id, consumption_liters, consumption_date) values (current_timestamp, current_timestamp, ?, "
                + "987654321, 1, current_timestamp)", farmId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aCommandMustTargetARegisteredDevice() {
        assertThatThrownBy(() -> jdbc.update("insert into iot_commands (created_at, updated_at, device_id, command_type, "
                + "status, requested_at) values (current_timestamp, current_timestamp, 'no-such-device', 'CLOSE_VALVE', "
                + "'queued', current_timestamp)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingASourceDeletesItsLevelHistory() throws Exception {
        jdbc.update("insert into water_level_history (created_at, updated_at, source_id, level_liters, measured_at) "
                + "values (current_timestamp, current_timestamp, ?, 400, current_timestamp)", sourceId);

        mockMvc.perform(delete("/api/water-supply/sources/{id}", sourceId)).andExpect(status().isNoContent());
        entityManager.flush();

        assertThat(jdbc.queryForObject("select count(*) from water_level_history where source_id = ?", Integer.class,
                sourceId)).isZero();
    }

    @Test
    void deletingAScheduleClearsTheIrrigationOfItsConsumptions() throws Exception {
        long scheduleId = idOf("/api/water-supply/irrigations", """
                {"zoneId":%d,"startTime":"2026-06-01T06:00:00Z","durationMinutes":30,"waterQuantityLiters":200}
                """.formatted(zoneId));
        long consumptionId = idOf("/api/water-supply/consumption", """
                {"farmId":%d,"sourceId":%d,"consumptionLiters":50,"consumptionDate":"2026-06-01T07:00:00Z",
                 "irrigationId":%d,"activity":"irrigation"}
                """.formatted(farmId, sourceId, scheduleId));
        mockMvc.perform(get("/api/water-supply/consumption/{id}", consumptionId))
                .andExpect(jsonPath("$.irrigationId").value(scheduleId))
                .andExpect(jsonPath("$.activity").value("IRRIGATION"));

        jdbc.update("delete from irrigation_schedules where id = ?", scheduleId);

        assertThat(jdbc.queryForObject("select irrigation_id from water_consumption where id = ?", Long.class,
                consumptionId)).isNull();
    }

    @Test
    void theFormerPathsAreGone() throws Exception {
        mockMvc.perform(get("/api/water/sources")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/irrigations")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/farms")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/notifications")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/water-supply/sources")).andExpect(status().isOk());
        mockMvc.perform(get("/api/farm/zones")).andExpect(status().isOk());
    }

    private long idOf(String path, String body) throws Exception {
        String response = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
