package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IotDevice;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.IotDeviceRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.service.AgroWeatherService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.PlantsGrowthPort;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * HTTP contract of the inter-module integrations (Plants, Visitor Management, Product
 * Transformation, IoT commands), on the full application context: it also guards the context
 * startup, which broke when services had two constructors.
 *
 * <p>The weather API and the Plants calendar are replaced by mocks so the results are deterministic.
 * Each test runs in a rolled-back transaction; block codes and lot ids are unique per run because
 * the in-memory database is shared with the other integration tests.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class WatersupplyIntegrationsApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IotDeviceRepository deviceRepository;

    @MockitoBean
    private AgroWeatherService agroWeatherService;

    @MockitoBean
    private PlantsGrowthPort plantsGrowthPort;

    // --- Plants ---------------------------------------------------------------------------------

    @Test
    void blockCodeFiltersConsumptionAndIrrigationWithConsistentPagination() throws Exception {
        String farmId = createFarm("Block Filter Farm");
        String fieldId = createField(farmId);
        String zoneA = createZone(fieldId, "Zone A", 1.0);
        String zoneB = createZone(fieldId, "Zone B", 1.0);
        String sourceId = createSource(farmId, "Block Filter Tank");
        String block = uniqueBlock();
        mapZone(zoneA, block.toLowerCase(), farmId);
        Instant at = Instant.now().minus(1, ChronoUnit.HOURS);
        createConsumption(farmId, sourceId, zoneA, 10.0, at, null, null);
        createConsumption(farmId, sourceId, zoneA, 20.0, at.plusSeconds(60), null, null);
        createConsumption(farmId, sourceId, zoneB, 40.0, at.plusSeconds(120), null, null);

        // Filtered in the database: the total counts the 2 matching rows, not the page.
        mockMvc.perform(get("/api/water/consumption").param("blockCode", block).param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].blockCode").value(block))
                .andExpect(jsonPath("$.content[0].multipleBlockCodes").value(false));
        mockMvc.perform(get("/api/water/consumption").param("zoneId", zoneB))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].blockCode").doesNotExist());

        createIrrigation(zoneA, 300.0);
        createIrrigation(zoneB, 300.0);
        mockMvc.perform(get("/api/irrigations").param("blockCode", block))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].zoneId").value(zoneA))
                .andExpect(jsonPath("$.content[0].blockCode").value(block));
        mockMvc.perform(get("/api/irrigations").param("blockCode", uniqueBlock()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rainfallByBlockDoesNotDoubleCountTwoGaugesOfTheSameFarm() throws Exception {
        String farmId = createFarm("Rain Farm");
        String zoneId = createZone(createField(farmId), "Rain Zone", 1.0);
        String block = uniqueBlock();
        mapZone(zoneId, block, farmId);
        String gaugeA = createSource(farmId, "Gauge A");
        String gaugeB = createSource(farmId, "Gauge B");
        createHarvest(gaugeA, 12.0, Instant.parse("2026-10-05T06:00:00Z"));
        createHarvest(gaugeB, 12.0, Instant.parse("2026-10-05T06:05:00Z"));
        createHarvest(gaugeA, 3.0, Instant.parse("2026-10-06T06:00:00Z"));
        createHarvest(gaugeA, 50.0, Instant.parse("2026-11-01T06:00:00Z"));

        mockMvc.perform(get("/api/water/plants/rainfall").param("blockCode", block)
                        .param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rainfallMm").value(15.0))
                .andExpect(jsonPath("$.gaugeCount").value(2))
                .andExpect(jsonPath("$.readings", hasSize(3)))
                .andExpect(jsonPath("$.spatialResolution").value("farm"));

        mockMvc.perform(get("/api/water/plants/rainfall").param("blockCode", uniqueBlock())
                        .param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(get("/api/water/plants/rainfall").param("blockCode", block))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void irrigationNeedOfABlockUsesTheZoneAreaAndThePlantsPhase() throws Exception {
        String farmId = createFarm("Need Farm");
        String zoneId = createZone(createField(farmId), "Need Zone", 1.5);
        String block = uniqueBlock();
        mapZone(zoneId, block, farmId);
        when(agroWeatherService.et0Between(any(), any())).thenReturn(5.0);
        when(plantsGrowthPort.growthForBlock(anyString()))
                .thenReturn(List.of(new PlantsGrowthPort.BlockGrowth(block, "full production", 7)));

        // 1.5 ha x 10 000 m2 x 5 mm x Kc 1.0 / efficiency 0.80 (no irrigation method) = 93 750 L.
        mockMvc.perform(post("/api/water/plants/irrigation-need").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"blockCode":"%s","treeCount":120,"date":"2026-10-13"}
                                """.formatted(block.toLowerCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blockCode").value(block))
                .andExpect(jsonPath("$.baseNeedLiters").value(93750))
                .andExpect(jsonPath("$.growthPhase").value("full production"))
                .andExpect(jsonPath("$.growthPhaseSource").value("plants_calendar"))
                .andExpect(jsonPath("$.needLiters").value(93750))
                .andExpect(jsonPath("$.matchedZones[0]").value(zoneId));

        mockMvc.perform(post("/api/water/plants/irrigation-need").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"blockCode":"%s","growthPhase":"flowering"}
                                """.formatted(block)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mockMvc.perform(post("/api/water/plants/irrigation-need").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"treeCount\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- Product Transformation -----------------------------------------------------------------

    @Test
    void lotUsageThenConsumptionByLotThenQualityCompliance() throws Exception {
        String farmId = createFarm("Lot Farm");
        String sourceId = createSource(farmId, "Washing Tank");
        String lotId = "LOT-" + UUID.randomUUID();
        Instant start = Instant.now().minus(3, ChronoUnit.HOURS);
        createConsumption(farmId, sourceId, null, 100.0, start.plusSeconds(600), null, null);
        createConsumption(farmId, sourceId, null, 50.0, start.plusSeconds(1200), null, null);
        createConsumption(farmId, sourceId, null, 999.0, start.minusSeconds(600), null, null);

        mockMvc.perform(post("/api/water/lot-usage").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lotId":"%s","activity":"washing","startAt":"%s","endAt":"%s","sourceId":"%s"}
                                """.formatted(lotId, start, start.plus(1, ChronoUnit.HOURS), sourceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachedCount").value(2))
                .andExpect(jsonPath("$.attachedLiters").value(150.0))
                .andExpect(jsonPath("$.warnings", hasSize(0)));

        mockMvc.perform(get("/api/water/consumption/by-lot").param("lotId", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLiters").value(150.0))
                .andExpect(jsonPath("$.byActivity.WASHING").value(150.0))
                .andExpect(jsonPath("$.items", hasSize(2)));
        mockMvc.perform(get("/api/water/consumption").param("lotId", lotId))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].activity").value("WASHING"));

        // No test yet in the lot window: no proof of compliance.
        mockMvc.perform(get("/api/water/quality/compliance").param("lotId", lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("no_test"))
                .andExpect(jsonPath("$.compliant").value(false));
        createQualityTest(sourceId, 7.0, start.plusSeconds(900));
        mockMvc.perform(get("/api/water/quality/compliance").param("lotId", lotId))
                .andExpect(jsonPath("$.status").value("compliant"))
                .andExpect(jsonPath("$.sourceIds[0]").value(sourceId));
        createQualityTest(sourceId, 9.1, start.plusSeconds(1000));
        mockMvc.perform(get("/api/water/quality/compliance").param("lotId", lotId))
                .andExpect(jsonPath("$.status").value("non_compliant"))
                .andExpect(jsonPath("$.tests", hasSize(2)));
    }

    @Test
    void lotEndpointsValidateTheirInput() throws Exception {
        mockMvc.perform(post("/api/water/lot-usage").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startAt\":\"2026-10-13T08:00:00Z\",\"endAt\":\"2026-10-13T09:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/water/consumption").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"farmId":"%s","sourceId":"%s","consumptionLiters":1,"consumptionDate":"2026-10-13T08:00:00Z","activity":"painting"}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mockMvc.perform(get("/api/water/consumption/by-lot"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availabilityForecastReportsATightBuffer() throws Exception {
        String sourceId = createSource(createFarm("Forecast Farm"), "Forecast Tank");

        // 2 500 L in the tank, washing 1 100 kg x 2 L/kg = 2 200 L: 300 L left, under the 500 L buffer.
        mockMvc.perform(post("/api/water/product-transformation/availability-forecast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceId":"%s","horizonDays":7,"includeIrrigation":false,
                                 "batches":[{"lotId":"LOT-F","activity":"washing","sizeKg":1100}]}
                                """.formatted(sourceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchDemandLiters").value(2200))
                .andExpect(jsonPath("$.availableAfterForecastLiters").value(300))
                .andExpect(jsonPath("$.bufferStatus").value("tight"));

        mockMvc.perform(post("/api/water/product-transformation/availability-forecast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/water/product-transformation/availability-forecast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceId\":\"%s\",\"batches\":[{\"activity\":\"painting\"}]}".formatted(sourceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    // --- Visitor Management and planned unavailability ------------------------------------------

    @Test
    void plannedUnavailabilityLifecycleAndVisitConflicts() throws Exception {
        String farmId = createFarm("Outage Farm");
        String zoneId = createZone(createField(farmId), "Outage Zone", 1.0);
        Instant now = Instant.now();
        String outageId = createOutage("zone", zoneId, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        String farmOutageId = createOutage("farm", farmId, now.plus(2, ChronoUnit.DAYS), now.plus(3, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/water/outages").contentType(MediaType.APPLICATION_JSON)
                        .content(outageJson("zone", zoneId, now, now.plus(30, ChronoUnit.MINUTES), "planned")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mockMvc.perform(post("/api/water/outages").contentType(MediaType.APPLICATION_JSON)
                        .content(outageJson("zone", UUID.randomUUID().toString(), now, now.plusSeconds(60), "planned")))
                .andExpect(status().isNotFound());

        // A farm-wide period also concerns the zones of that farm.
        mockMvc.perform(get("/api/water/outages").param("zoneId", zoneId))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/water/outages").param("zoneId", zoneId).param("activeOn", now.toString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(outageId));

        String visitId = "VIS-" + UUID.randomUUID();
        String visit = """
                {"visitId":"%s","groupName":"School A","expectedAt":"%s","zones":["%s"]}
                """.formatted(visitId, now, zoneId);
        mockMvc.perform(post("/api/water/visitor-events").contentType(MediaType.APPLICATION_JSON).content(visit))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.conflict").value(true))
                .andExpect(jsonPath("$.conflictingZones[0]").value(zoneId));
        mockMvc.perform(post("/api/water/visitor-events").contentType(MediaType.APPLICATION_JSON).content(visit))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(false));

        mockMvc.perform(get("/api/water/visitor/summary").param("zoneId", zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeUnavailability", hasSize(1)))
                .andExpect(jsonPath("$.irrigationStatus.status").value("none"))
                .andExpect(jsonPath("$", hasKey("waterSavingsPercentage")));

        mockMvc.perform(put("/api/water/outages/{id}", outageId).contentType(MediaType.APPLICATION_JSON)
                        .content(outageJson("zone", zoneId, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), "cancelled")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
        mockMvc.perform(get("/api/water/outages").param("zoneId", zoneId).param("activeOn", now.toString()))
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(delete("/api/water/outages/{id}", farmOutageId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/water/outages/{id}", farmOutageId)).andExpect(status().isNotFound());
    }

    @Test
    void visitorIrrigationStatusAndLiveConsumption() throws Exception {
        String farmId = createFarm("Visitor Farm");
        String zoneId = createZone(createField(farmId), "Visitor Zone", 1.0);
        String sourceId = createSource(farmId, "Visitor Tank");

        mockMvc.perform(get("/api/water/visitor/irrigation-status").param("zoneId", zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.status").value("none"));
        String scheduleId = createIrrigation(zoneId, 250.0);
        mockMvc.perform(post("/api/irrigations/{id}/start", scheduleId)).andExpect(status().isCreated());
        mockMvc.perform(get("/api/water/visitor/irrigation-status").param("zoneId", zoneId))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.plannedLiters").value(250.0))
                .andExpect(jsonPath("$.lastActivationAt").isNotEmpty());
        mockMvc.perform(get("/api/water/visitor/irrigation-status").param("zoneId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/water/visitor/irrigation-status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        createConsumption(farmId, sourceId, zoneId, 12.5, Instant.now().minus(2, ChronoUnit.MINUTES), null, null);
        createConsumption(farmId, sourceId, zoneId, 99.0, Instant.now().minus(2, ChronoUnit.HOURS), null, null);
        mockMvc.perform(get("/api/water/visitor/live-consumption").param("sourceId", sourceId).param("windowMinutes", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liters").value(12.5))
                .andExpect(jsonPath("$.measurementCount").value(1))
                .andExpect(jsonPath("$.windowMinutes").value(10));
    }

    @Test
    void safetyBriefingIsVersionedOnPublication() throws Exception {
        mockMvc.perform(get("/api/water/visitor/safety-briefing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.forbiddenZones").isArray());

        mockMvc.perform(patch("/api/water/visitor/safety-briefing").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":"2026-10-22","text":"Stay on the marked path.","maxCapacity":8}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(true))
                .andExpect(jsonPath("$.maxCapacity").value(8));
        mockMvc.perform(get("/api/water/visitor/safety-briefing"))
                .andExpect(jsonPath("$.version").value("2026-10-22"))
                .andExpect(jsonPath("$.text").value("Stay on the marked path."));
        mockMvc.perform(patch("/api/water/visitor/safety-briefing").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxCapacity\":0}"))
                .andExpect(status().isBadRequest());
    }

    // --- Internal logic -------------------------------------------------------------------------

    @Test
    void levelHistoryAndWaterBalanceForecast() throws Exception {
        String sourceId = createSource(createFarm("History Farm"), "History Tank");
        mockMvc.perform(put("/api/water/sources/{id}", sourceId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentLevelLiters\":1800}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/water/sources/{id}/level-history", sourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].levelLiters").value(2500.0))
                .andExpect(jsonPath("$[1].levelLiters").value(1800.0));
        mockMvc.perform(get("/api/water/sources/{id}/level-history", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/dashboard/water-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("demand_forecast_liters")))
                .andExpect(jsonPath("$", hasKey("available_after_forecast_liters")));
    }

    // --- IoT valve commands ---------------------------------------------------------------------

    @Test
    void valveCommandsAreCheckedAndJournaledEvenWithoutGateway() throws Exception {
        String deviceId = "valve-" + UUID.randomUUID().toString().substring(0, 8);
        registerDevice(deviceId, Instant.now());
        String offline = "valve-" + UUID.randomUUID().toString().substring(0, 8);
        registerDevice(offline, Instant.now().minus(2, ChronoUnit.HOURS));

        mockMvc.perform(command(deviceId, "{\"command\":\"OPEN_VALVE\",\"durationSeconds\":10,\"reason\":\"visitor_demo\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("VALVE_GATEWAY_UNAVAILABLE"));
        mockMvc.perform(get("/api/iot/devices/{id}/commands", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].command").value("OPEN_VALVE"))
                .andExpect(jsonPath("$[0].status").value("rejected"));

        mockMvc.perform(command(offline, "{\"command\":\"CLOSE_VALVE\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(command(deviceId, "{\"command\":\"OPEN_VALVE\",\"durationSeconds\":600}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(command(deviceId, "{\"command\":\"SELF_DESTRUCT\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(command("unknown-" + UUID.randomUUID(), "{\"command\":\"CLOSE_VALVE\"}"))
                .andExpect(status().isNotFound());
        // The duplicate route that lived under /api/water is gone: one way to command a valve.
        mockMvc.perform(post("/api/water/iot/devices/{id}/commands", deviceId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"CLOSE_VALVE\"}"))
                .andExpect(status().isNotFound());
    }

    // --- Helpers --------------------------------------------------------------------------------

    private MockHttpServletRequestBuilder command(String deviceId, String body) {
        return post("/api/iot/devices/{id}/commands", deviceId).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void registerDevice(String deviceId, Instant lastSeen) {
        IotDevice device = new IotDevice();
        device.setDeviceUid(deviceId);
        device.setType("valve");
        device.setLastSeen(lastSeen);
        deviceRepository.save(device);
    }

    private static String uniqueBlock() {
        return "B" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String createFarm(String name) throws Exception {
        return idOf(post("/api/farms"), "{\"name\":\"%s\",\"areaHectares\":12.5}".formatted(name));
    }

    private String createField(String farmId) throws Exception {
        return idOf(post("/api/fields"), "{\"farmId\":\"%s\",\"name\":\"Field\",\"areaHectares\":5}".formatted(farmId));
    }

    private String createZone(String fieldId, String name, double hectares) throws Exception {
        return idOf(post("/api/zones"), "{\"fieldId\":\"%s\",\"name\":\"%s\",\"areaHectares\":%s}"
                .formatted(fieldId, name, hectares));
    }

    private String createSource(String farmId, String name) throws Exception {
        return idOf(post("/api/water/sources"), """
                {"farmId":"%s","name":"%s","type":"tank","capacityLiters":5000,"currentLevelLiters":2500}
                """.formatted(farmId, name));
    }

    private void mapZone(String zoneId, String blockCode, String farmId) throws Exception {
        idOf(post("/api/water/zone-block-mappings"), "{\"zoneId\":\"%s\",\"blockCode\":\"%s\",\"farmId\":\"%s\"}"
                .formatted(zoneId, blockCode, farmId));
    }

    private void createConsumption(String farmId, String sourceId, String zoneId, double liters, Instant at,
                                   String lotId, String activity) throws Exception {
        idOf(post("/api/water/consumption"), """
                {"farmId":"%s","sourceId":"%s","zoneId":%s,"consumptionLiters":%s,"consumptionDate":"%s","lotId":%s,"activity":%s}
                """.formatted(farmId, sourceId, quoted(zoneId), liters, at, quoted(lotId), quoted(activity)));
    }

    private String createIrrigation(String zoneId, double liters) throws Exception {
        return idOf(post("/api/irrigations"), """
                {"zoneId":"%s","startTime":"%s","durationMinutes":30,"waterQuantityLiters":%s}
                """.formatted(zoneId, Instant.now().plus(1, ChronoUnit.HOURS), liters));
    }

    private void createHarvest(String sourceId, double rainfallMm, Instant at) throws Exception {
        idOf(post("/api/rainwater-harvests"), """
                {"sourceId":"%s","catchmentAreaM2":100,"rainfallMm":%s,"runoffCoefficient":0.8,"harvestedLiters":100,"captureDate":"%s"}
                """.formatted(sourceId, rainfallMm, at));
    }

    private void createQualityTest(String sourceId, double ph, Instant at) throws Exception {
        idOf(post("/api/water/quality"), "{\"sourceId\":\"%s\",\"ph\":%s,\"turbidityNtu\":1.0,\"testDate\":\"%s\"}"
                .formatted(sourceId, ph, at));
    }

    private String createOutage(String scope, String scopeId, Instant start, Instant end) throws Exception {
        return idOf(post("/api/water/outages"), outageJson(scope, scopeId, start, end, "planned"));
    }

    private static String outageJson(String scope, String scopeId, Instant start, Instant end, String status) {
        return """
                {"scope":"%s","scopeId":"%s","startAt":"%s","endAt":"%s","reason":"filter cleaning","status":"%s"}
                """.formatted(scope, scopeId, start, end, status);
    }

    private String idOf(MockHttpServletRequestBuilder request, String body) throws Exception {
        MvcResult result = mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
