package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.SustainableFarmApplication;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Supplier;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.AlertRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.LossRecordRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.QualityCheckRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorReadingRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SustainableFarmApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("crop-storage-http-test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.open-in-view=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import(CropStorageHttpTestConfiguration.class)
class CropStorageHttpJpaIntegrationTest {

    private static final AtomicInteger NEXT_CODE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private StorageZoneRepository storageZoneRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private LossRecordRepository lossRecordRepository;

    @Autowired
    private QualityCheckRepository qualityCheckRepository;

    @BeforeEach
    void verifyIsolationAndClearCropStorageData() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:crop_storage_http_isolated"));
        }
        assertEquals("false", environment.getProperty("spring.jpa.open-in-view"));
        assertEquals("false", environment.getProperty("spring.flyway.enabled"));

        alertRepository.deleteAll();
        sensorReadingRepository.deleteAll();
        stockMovementRepository.deleteAll();
        lossRecordRepository.deleteAll();
        qualityCheckRepository.deleteAll();
        batchRepository.deleteAll();
        sensorRepository.deleteAll();
        storageZoneRepository.deleteAll();
        supplierRepository.deleteAll();
    }

    @Test
    void createAlertWithLinkedSensorAndZoneReturnsHttp200AndSavesExactlyOnce() throws Exception {
        StorageZone zone = createZone();
        Sensor sensor = createSensor(zone);

        mockMvc.perform(post("/api/crop-storage/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"DAMAGE","severity":"HIGH","message":"Observed",
                                 "status":"ACTIVE","sensor":{"id":%d},"storageZone":{"id":%d}}
                                """.formatted(sensor.getId(), zone.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensor.storageZone.zoneCode").value(zone.getZoneCode()))
                .andExpect(jsonPath("$.storageZone.id").value(zone.getId()));

        assertEquals(1, alertRepository.count());
    }

    @Test
    void dashboardListsSerializeLinkedBatchesSensorsReadingsAndActiveAlerts() throws Exception {
        Supplier supplier = createSupplier();
        StorageZone zone = createZone();
        Batch batch = createBatch(supplier, zone, "12");
        Sensor sensor = createSensor(zone);
        createReading(sensor);
        createAlert(sensor, zone, batch);

        mockMvc.perform(get("/api/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplier.name").value(supplier.getName()))
                .andExpect(jsonPath("$[0].storageZone.zoneCode").value(zone.getZoneCode()));
        mockMvc.perform(get("/api/sensors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storageZone.zoneCode").value(zone.getZoneCode()));
        mockMvc.perform(get("/api/sensor-readings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensor.storageZone.zoneCode").value(zone.getZoneCode()));
        mockMvc.perform(get("/api/crop-storage/alerts/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sensor.storageZone.zoneCode").value(zone.getZoneCode()))
                .andExpect(jsonPath("$[0].batch.supplier.name").value(supplier.getName()));
    }

    @Test
    void createsAndListsMovementsLossesAndQualityChecksWithLinkedBatch() throws Exception {
        Supplier supplier = createSupplier();
        StorageZone zone = createZone();
        Batch batch = createBatch(supplier, zone, "10");

        mockMvc.perform(post("/api/stock-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"OUT","quantityKg":2,"batch":{"id":%d},
                                 "fromZone":{"id":%d},"reason":"Dispatch","movedAt":"2026-10-30T10:00:00"}
                                """.formatted(batch.getId(), zone.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.supplier.name").value(supplier.getName()))
                .andExpect(jsonPath("$.fromZone.zoneCode").value(zone.getZoneCode()));

        assertEquals(0, new BigDecimal("8.00").compareTo(
                batchRepository.findById(batch.getId()).orElseThrow().getCurrentQuantityKg()));
        assertEquals(1, stockMovementRepository.count());
        mockMvc.perform(get("/api/stock-movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batch.storageZone.zoneCode").value(zone.getZoneCode()));
        mockMvc.perform(get("/api/stock-movements/batch/{id}", batch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batch.supplier.name").value(supplier.getName()));

        mockMvc.perform(post("/api/loss-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batch":{"id":%d},"quantityKg":1,"cause":"Damage",
                                 "recordedAt":"2026-10-30T10:00:00"}
                                """.formatted(batch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.supplier.name").value(supplier.getName()));
        mockMvc.perform(get("/api/loss-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batch.storageZone.zoneCode").value(zone.getZoneCode()));
        mockMvc.perform(get("/api/loss-records/batch/{id}", batch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batch.supplier.name").value(supplier.getName()));

        mockMvc.perform(post("/api/quality-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batch":{"id":%d},"moisturePercent":12,"qualityGrade":"A",
                                 "passed":true,"checkedAt":"2026-10-30T10:00:00"}
                                """.formatted(batch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.supplier.name").value(supplier.getName()));
        mockMvc.perform(get("/api/quality-checks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batch.storageZone.zoneCode").value(zone.getZoneCode()));
        mockMvc.perform(get("/api/quality-checks/batch/{id}", batch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batch.supplier.name").value(supplier.getName()));
        assertEquals(1, stockMovementRepository.count());
    }

    @Test
    void alertAcknowledgeAndResolveResponsesKeepLinkedReferencesSerializable() throws Exception {
        StorageZone zone = createZone();
        Sensor sensor = createSensor(zone);
        Alert alert = createAlert(sensor, zone, null);

        mockMvc.perform(patch("/api/crop-storage/alerts/{id}/acknowledge", alert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.sensor.storageZone.zoneCode").value(zone.getZoneCode()));
        mockMvc.perform(patch("/api/crop-storage/alerts/{id}/resolve", alert.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.sensor.storageZone.zoneCode").value(zone.getZoneCode()));
    }

    @Test
    void nestedIdRequestsPreserveStoredReferencesAndIgnoreOverpostedEntityFields() throws Exception {
        Supplier supplier = createSupplier();
        StorageZone zone = createZone();

        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":999,"version":55,"batchCode":"NESTED-%d","productType":"MANGO",
                                 "initialQuantityKg":10,"status":"STORED","supplier":{"id":%d,"name":"forged"},
                                 "storageZone":{"id":%d,"zoneCode":"forged"}}
                                """.formatted(NEXT_CODE.incrementAndGet(), supplier.getId(), zone.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(999)))
                .andExpect(jsonPath("$.version").value(org.hamcrest.Matchers.not(55)))
                .andExpect(jsonPath("$.supplier.name").value(supplier.getName()))
                .andExpect(jsonPath("$.storageZone.zoneCode").value(zone.getZoneCode()));
    }

    @Test
    void unknownNestedReferencesReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/crop-storage/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"DAMAGE","status":"ACTIVE","sensor":{"id":999999}}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/stock-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"OUT","quantityKg":1,"batch":{"id":999999},
                                 "movedAt":"2026-10-30T10:00:00"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/loss-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batch":{"id":999999},"quantityKg":1,"recordedAt":"2026-10-30T10:00:00"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/quality-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batch":{"id":999999},"passed":true,"checkedAt":"2026-10-30T10:00:00"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void api005StorageEntryCreatesBatchAndMovement() throws Exception {
        StorageZone zone = createZone();

        mockMvc.perform(post("/api/crop-storage/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batch_id":"API005-%d","packaging_id":"PKG-1","dried_quantity_kg":25,
                                 "waste_quantity_kg":1,"packaging_date":"2026-10-30T10:00:00Z",
                                 "packaging_operator_id":"operator-1","packaging_location":"line-1",
                                 "target_storage_location_id":"%d","product_grade":"A","harvest_id":"42",
                                 "farm_id":"farm-1","block_id":"block-1","variety_id":"variety-1"}
                                """.formatted(NEXT_CODE.incrementAndGet(), zone.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storage_entry_id").isNotEmpty())
                .andExpect(jsonPath("$.storage_location_id").value(zone.getId().toString()))
                .andExpect(jsonPath("$.status").value("STORED"));

        assertEquals(1, batchRepository.count());
        assertEquals(1, stockMovementRepository.count());
    }

    private Supplier createSupplier() {
        int suffix = NEXT_CODE.incrementAndGet();
        Supplier supplier = new Supplier();
        supplier.setSupplierCode("SUP-" + suffix);
        supplier.setName("Supplier " + suffix);
        return supplierRepository.save(supplier);
    }

    private StorageZone createZone() {
        int suffix = NEXT_CODE.incrementAndGet();
        StorageZone zone = new StorageZone();
        zone.setZoneCode("ZONE-" + suffix);
        zone.setName("Zone " + suffix);
        zone.setCapacityKg(new BigDecimal("1000"));
        zone.setStatus("ACTIVE");
        return storageZoneRepository.save(zone);
    }

    private Batch createBatch(Supplier supplier, StorageZone zone, String quantity) {
        int suffix = NEXT_CODE.incrementAndGet();
        Batch batch = new Batch();
        batch.setBatchCode("BATCH-" + suffix);
        batch.setProductType("DRIED_MANGO");
        batch.setInitialQuantityKg(new BigDecimal(quantity));
        batch.setCurrentQuantityKg(new BigDecimal(quantity));
        batch.setStatus("STORED");
        batch.setCustomerPriority(0);
        batch.setSupplier(supplier);
        batch.setStorageZone(zone);
        return batchRepository.save(batch);
    }

    private Sensor createSensor(StorageZone zone) {
        int suffix = NEXT_CODE.incrementAndGet();
        Sensor sensor = new Sensor();
        sensor.setSensorCode("SENSOR-" + suffix);
        sensor.setName("Sensor " + suffix);
        sensor.setStatus("ACTIVE");
        sensor.setStorageZone(zone);
        return sensorRepository.save(sensor);
    }

    private SensorReading createReading(Sensor sensor) {
        SensorReading reading = new SensorReading();
        reading.setSensor(sensor);
        reading.setTemperature(new BigDecimal("21"));
        reading.setHumidity(new BigDecimal("45"));
        reading.setMeasuredAt(LocalDateTime.now());
        return sensorReadingRepository.save(reading);
    }

    private Alert createAlert(Sensor sensor, StorageZone zone, Batch batch) {
        Alert alert = new Alert();
        alert.setType("DAMAGE");
        alert.setSeverity("HIGH");
        alert.setMessage("Observed");
        alert.setStatus("ACTIVE");
        alert.setSensor(sensor);
        alert.setStorageZone(zone);
        alert.setBatch(batch);
        return alertRepository.save(alert);
    }
}
