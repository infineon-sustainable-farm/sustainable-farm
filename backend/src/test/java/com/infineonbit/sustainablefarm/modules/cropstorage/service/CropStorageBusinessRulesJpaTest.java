package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.ValuationSummary;
import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.MovementType;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropProductType;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.BatchStatus;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.OperationalStatus;
import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.cropstorage.CropStorageH2IsolationGuard;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropStorageBatch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.AlertRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.CropStorageBatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorReadingRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Crop Storage business rules against a real JPA stack on an isolated,
 * in-memory H2 database (asserted before every test). Services run their own
 * transactions (the test itself is not transactional), so commits and
 * rollbacks are real. H2 is not PostgreSQL: this is no proof of the Flyway
 * schema or of PostgreSQL locking behaviour.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:crop_storage_rules_isolated;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never"
})
@Import({StorageEntryService.class, StockMovementService.class, CropStorageBatchService.class,
        SensorReadingService.class, AlertService.class})
@ContextConfiguration(initializers = CropStorageH2IsolationGuard.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CropStorageBusinessRulesJpaTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private StorageEntryService entryService;
    @Autowired private StockMovementService movementService;
    @Autowired private CropStorageBatchService batchService;
    @Autowired private SensorReadingService readingService;
    @Autowired private AlertService alertService;
    @Autowired private CropStorageBatchRepository batches;
    @Autowired private StorageZoneRepository zones;
    @Autowired private StockMovementRepository movements;
    @Autowired private SensorRepository sensors;
    @Autowired private SensorReadingRepository readings;
    @Autowired private AlertRepository alerts;
    // Plants and staff are other modules: their lookups are mocked here.
    @MockitoBean private SharedReferences sharedReferences;

    @BeforeEach
    void assertIsolatedDatabase() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:crop_storage_rules_isolated"),
                    "refusing to run against a non-isolated database");
        }
    }

    @AfterEach
    void clearCropStorageTables() {
        alerts.deleteAll();
        readings.deleteAll();
        movements.deleteAll();
        batches.deleteAll();
        sensors.deleteAll();
        zones.deleteAll();
    }

    // ---------- API-005 storage entry ----------

    @Test
    void entryPersistsBatchAndInMovementWithUtcDatesAndTraceability() {
        StorageZone zone = zone("1000");

        StorageEntryResponse response = entryService.registerEntry(
                entry("PT-1", "25.50", String.valueOf(zone.getId()), "42",
                        OffsetDateTime.of(2026, 10, 30, 23, 30, 0, 0, ZoneOffset.ofHours(-2))));

        CropStorageBatch batch = batches.findByBatchCode("PT-1").orElseThrow();
        assertEquals(String.valueOf(batch.getId()), response.storageEntryId());
        assertEquals(String.valueOf(zone.getId()), response.storageLocationId());
        assertEquals(ZoneOffset.UTC, response.entryTimestamp().getOffset());
        assertEquals(0, new BigDecimal("974.50").compareTo(response.availableCapacityKg()));
        assertEquals("STORED", response.status());
        assertEquals(0, new BigDecimal("25.50").compareTo(batch.getCurrentQuantityKg()));
        assertEquals(LocalDate.of(2026, 10, 31), batch.getPackagingDate(), "packaging date is the UTC date");
        assertEquals(42L, batch.getHarvestId());

        List<StockMovement> moves = movements.findByBatchIdOrderByMovedAtDesc(batch.getId());
        assertEquals(1, moves.size());
        assertEquals(MovementType.IN, moves.get(0).getType());
        assertEquals(0, new BigDecimal("25.50").compareTo(moves.get(0).getQuantityKg()));
        assertEquals(zone.getId(), moves.get(0).getToZone().getId());
        assertTrue(moves.get(0).getReason().contains("PKG-PT-1"));
    }

    @Test
    void absentHarvestIdStaysNull() {
        StorageZone zone = zone("100");
        entryService.registerEntry(entry("PT-NOHARV", "1", String.valueOf(zone.getId()), null, utcNow()));
        assertNull(batches.findByBatchCode("PT-NOHARV").orElseThrow().getHarvestId());
    }

    @Test
    void unknownOrNonNumericZoneIs404AndWritesNothing() {
        zone("100");
        assertThrows(ResourceNotFoundException.class,
                () -> entryService.registerEntry(entry("PT-2", "1", "999999", null, utcNow())));
        assertThrows(ResourceNotFoundException.class,
                () -> entryService.registerEntry(entry("PT-3", "1", "ZONE-A1", null, utcNow())));
        assertEquals(0, batches.count());
        assertEquals(0, movements.count());
    }

    @Test
    void zoneCapacityAcceptsExactFitAndRefusesOverflow() {
        StorageZone zone = zone("100");
        String zoneId = String.valueOf(zone.getId());

        StorageEntryResponse fit = entryService.registerEntry(entry("PT-FIT", "100", zoneId, null, utcNow()));
        assertEquals(0, BigDecimal.ZERO.compareTo(fit.availableCapacityKg()));

        assertThrows(ConflictException.class,
                () -> entryService.registerEntry(entry("PT-OVER", "0.01", zoneId, null, utcNow())));
        assertEquals(1, batches.count());
        assertEquals(1, movements.count());
    }

    @Test
    void automaticAssignmentPicksTheZoneWithMostRoomOrRefuses() {
        zone("50");
        StorageZone large = zone("500");

        StorageEntryResponse response = entryService.registerEntry(entry("PT-AUTO", "60", null, null, utcNow()));
        assertEquals(String.valueOf(large.getId()), response.storageLocationId());

        assertThrows(ConflictException.class,
                () -> entryService.registerEntry(entry("PT-TOO-BIG", "1000", null, null, utcNow())));
    }

    @Test
    void duplicateBatchIdIsRefusedWith409() {
        StorageZone zone = zone("1000");
        String zoneId = String.valueOf(zone.getId());
        entryService.registerEntry(entry("PT-DUP", "1", zoneId, null, utcNow()));

        assertThrows(ConflictException.class,
                () -> entryService.registerEntry(entry("PT-DUP", "1", zoneId, null, utcNow())));
        assertEquals(1, batches.count());
    }

    @Test
    void failureWhileWritingTheMovementRollsBackTheBatch() throws Exception {
        StorageZone zone = zone("1000");
        StockMovementRepository failing = mock(StockMovementRepository.class);
        when(failing.save(any())).thenThrow(new IllegalStateException("simulated failure"));
        StorageEntryService service = new StorageEntryService(batches, zones, failing, sharedReferences);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status ->
                service.registerEntry(entry("PT-RB", "5", String.valueOf(zone.getId()), null, utcNow()))));

        assertEquals(0, batches.count(), "no half-written entry");
        assertTrue(StorageEntryService.class.getMethod("registerEntry", StorageEntryRequest.class)
                .isAnnotationPresent(Transactional.class));
    }

    @Test
    void readsEntryByIdAndByBatchId() {
        StorageZone zone = zone("100");
        StorageEntryResponse created = entryService.registerEntry(
                entry("PT-READ", "10", String.valueOf(zone.getId()), null, utcNow()));

        StorageEntryResponse read = entryService.findEntry(Long.valueOf(created.storageEntryId()));
        assertEquals(created.storageEntryId(), read.storageEntryId());
        assertEquals(0, new BigDecimal("90").compareTo(read.availableCapacityKg()));
        assertEquals(created.entryTimestamp().toInstant(), read.entryTimestamp().toInstant());
        assertEquals(1, entryService.findEntriesByBatchId("PT-READ").size());
        assertTrue(entryService.findEntriesByBatchId("PT-NONE").isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> entryService.findEntry(987654L));
    }

    @Test
    void concurrentEntriesCannotOverfillAZone() throws Exception {
        StorageZone zone = zone("100");
        String zoneId = String.valueOf(zone.getId());
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String code = "PT-RACE-" + i;
            tasks.add(() -> entryService.registerEntry(entry(code, "40", zoneId, null, utcNow())));
        }

        Map<String, Integer> outcomes = runConcurrently(tasks);

        assertEquals(2, outcomes.getOrDefault("ok", 0), "only 2 x 40 kg fit in 100 kg: " + outcomes);
        assertTrue(zones.findById(zone.getId()).isPresent());
        BigDecimal stored = batches.sumCurrentQuantityKgByStorageZoneId(zone.getId());
        assertTrue(stored.compareTo(new BigDecimal("100")) <= 0, "stored " + stored);
    }

    @Test
    void concurrentDuplicateBatchIdStoresOnlyOneEntry() throws Exception {
        StorageZone zone = zone("1000");
        String zoneId = String.valueOf(zone.getId());
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(() -> entryService.registerEntry(entry("PT-SAME", "1", zoneId, null, utcNow())));
        }

        Map<String, Integer> outcomes = runConcurrently(tasks);

        assertEquals(1, outcomes.getOrDefault("ok", 0), outcomes.toString());
        assertEquals(1, batches.count());
    }

    // ---------- Stock movements ----------

    @Test
    void outMovementCannotGoBelowZeroStockAndExactOutEmptiesTheBatch() {
        StorageZone zone = zone("100");
        CropStorageBatch batch = batch(zone, "10", 0, LocalDate.now());

        assertThrows(BusinessRuleException.class, () -> movementService.create(move("OUT", "10.01", batch, null)));
        assertEquals(0, new BigDecimal("10").compareTo(batches.findById(batch.getId()).orElseThrow().getCurrentQuantityKg()));

        movementService.create(move("OUT", "10", batch, null));
        assertEquals(0, BigDecimal.ZERO.compareTo(batches.findById(batch.getId()).orElseThrow().getCurrentQuantityKg()));
    }

    @Test
    void inAndTransferRespectDestinationCapacity() {
        StorageZone source = zone("100");
        StorageZone target = zone("50");
        batch(target, "45", 0, LocalDate.now());
        CropStorageBatch moving = batch(source, "10", 0, LocalDate.now());

        assertThrows(ConflictException.class, () -> movementService.create(move("TRANSFER", "10", moving, target)));
        assertThrows(ConflictException.class, () -> movementService.create(move("IN", "6", moving, target)));
        assertEquals(source.getId(), batches.findWithRelationshipsById(moving.getId()).orElseThrow()
                .getStorageZone().getId());

        movementService.create(move("IN", "40", moving, source));
        assertEquals(0, new BigDecimal("50").compareTo(batches.findById(moving.getId()).orElseThrow().getCurrentQuantityKg()));
        assertThrows(ConflictException.class, () -> movementService.create(move("IN", "50.01", moving, source)));
    }

    @Test
    void movementWithoutTypeAndTransferWithoutDestinationAreRefused() {
        // An unknown type name never reaches the service: the enum makes it a 400
        // (CropStorageCreateControllerTest); a missing one is refused here.
        CropStorageBatch batch = batch(zone("100"), "10", 0, LocalDate.now());
        StockMovement untyped = move("OUT", "1", batch, null);
        untyped.setType(null);
        assertThrows(BusinessRuleException.class, () -> movementService.create(untyped));
        assertThrows(BusinessRuleException.class, () -> movementService.create(move("TRANSFER", "1", batch, null)));
        assertEquals(0, movements.count());
    }

    // ---------- FIFO and valuation ----------

    @Test
    void fifoServesHigherPriorityFirstThenOldestEntryAndSkipsEmptyBatches() {
        StorageZone zone = zone("10000");
        CropStorageBatch oldLow = batch(zone, "5", 0, LocalDate.of(2026, 1, 1));
        CropStorageBatch newLow = batch(zone, "5", 0, LocalDate.of(2026, 3, 1));
        CropStorageBatch newHigh = batch(zone, "5", 2, LocalDate.of(2026, 4, 1));
        batch(zone, "0", 5, LocalDate.of(2025, 1, 1));

        List<Long> order = batchService.fifo().stream().map(CropStorageBatch::getId).toList();

        assertEquals(List.of(newHigh.getId(), oldLow.getId(), newLow.getId()), order);
    }

    @Test
    void summaryValuesCurrentStockInEveryReportingUnit() {
        StorageZone zone = zone("10000");
        CropStorageBatch valued = batch(zone, "1000", 0, LocalDate.now());
        // The value per kg is stored as Money with 2 decimals, in XOF.
        valued.setValuePerKg(Money.of("600.00", CurrencyCode.XOF));
        batches.save(valued);
        batch(zone, "500", 0, LocalDate.now()); // no value: contributes kg, not FCFA

        ValuationSummary summary = batchService.summary();

        assertEquals(0, new BigDecimal("1500").compareTo(summary.totalKg()));
        assertEquals(0, new BigDecimal("1.500").compareTo(summary.totalTonnes()));
        assertEquals(0, new BigDecimal("3306.937").compareTo(summary.totalLb()));
        assertEquals(0, new BigDecimal("2.143").compareTo(summary.totalM3()));
        // EUR: fixed peg 655.957; USD: HYPOTHESIS rate 600 FCFA, as documented in the service.
        assertEquals(0, new BigDecimal("600000").compareTo(summary.totalValueFcfa()));
        assertEquals(0, new BigDecimal("914.69").compareTo(summary.totalValueEur()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(summary.totalValueUsd()));
    }

    // ---------- Alerts ----------

    @Test
    void repeatedOutOfRangeReadingsRaiseOneActiveAlertUntilResolved() {
        StorageZone zone = zone("100");
        zone.setTemperatureMaxCelsius(new BigDecimal("25"));
        zones.save(zone);
        Sensor sensor = sensor(zone);

        readingService.create(reading(sensor, "30"));
        readingService.create(reading(sensor, "31"));
        readingService.create(reading(sensor, "20"));

        List<Alert> active = alerts.findByStatus("ACTIVE");
        assertEquals(1, active.size());
        assertEquals("TEMPERATURE", active.get(0).getType());

        alertService.updateStatus(active.get(0).getId(), "RESOLVED");
        readingService.create(reading(sensor, "32"));
        assertEquals(1, alerts.findByStatus("ACTIVE").size());
        assertEquals(2, alerts.count());
    }

    // ---------- helpers (synthetic fixtures, not farm data) ----------

    private Map<String, Integer> runConcurrently(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            Map<String, Integer> outcomes = new java.util.TreeMap<>();
            for (Future<Object> future : futures) {
                String key;
                try {
                    future.get(30, TimeUnit.SECONDS);
                    key = "ok";
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    // Only the service's ConflictException counts: a raw constraint
                    // violation leaking out of the service would fail the test.
                    key = cause instanceof ConflictException ? "conflict" : cause.getClass().getSimpleName();
                }
                outcomes.merge(key, 1, Integer::sum);
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private static OffsetDateTime utcNow() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static StorageEntryRequest entry(String batchId, String kg, String zoneId, String harvestId,
                                             OffsetDateTime packagingDate) {
        return new StorageEntryRequest(batchId, "PKG-" + batchId, new BigDecimal(kg), null, packagingDate,
                "op-1", "line-1", zoneId, "A", harvestId, "farm-1", "block-1", "variety-1");
    }

    private StorageZone zone(String capacityKg) {
        StorageZone zone = new StorageZone();
        zone.setZoneCode("Z-" + SEQ.incrementAndGet());
        zone.setCapacityKg(new BigDecimal(capacityKg));
        zone.setStatus(OperationalStatus.ACTIVE);
        return zones.save(zone);
    }

    private CropStorageBatch batch(StorageZone zone, String kg, int priority, LocalDate entryDate) {
        CropStorageBatch batch = new CropStorageBatch();
        batch.setBatchCode("B-" + SEQ.incrementAndGet());
        batch.setProductType(CropProductType.DRIED_MANGO);
        batch.setInitialQuantityKg(new BigDecimal(kg));
        batch.setCurrentQuantityKg(new BigDecimal(kg));
        batch.setStatus(BatchStatus.STORED);
        batch.setCustomerPriority(priority);
        batch.setStorageEntryDate(entryDate);
        batch.setStorageZone(zone);
        return batches.save(batch);
    }

    private static StockMovement move(String type, String kg, CropStorageBatch batch, StorageZone toZone) {
        StockMovement movement = new StockMovement();
        movement.setType(MovementType.valueOf(type));
        movement.setQuantityKg(new BigDecimal(kg));
        CropStorageBatch ref = new CropStorageBatch();
        ref.setId(batch.getId());
        movement.setBatch(ref);
        if (toZone != null) {
            StorageZone zoneRef = new StorageZone();
            zoneRef.setId(toZone.getId());
            movement.setToZone(zoneRef);
        }
        movement.setMovedAt(LocalDateTime.now());
        return movement;
    }

    private Sensor sensor(StorageZone zone) {
        Sensor sensor = new Sensor();
        sensor.setSensorCode("S-" + SEQ.incrementAndGet());
        sensor.setStatus(OperationalStatus.ACTIVE);
        sensor.setStorageZone(zone);
        return sensors.save(sensor);
    }

    private static SensorReading reading(Sensor sensor, String temperature) {
        SensorReading reading = new SensorReading();
        Sensor ref = new Sensor();
        ref.setId(sensor.getId());
        reading.setSensor(ref);
        reading.setTemperature(new BigDecimal(temperature));
        reading.setMeasuredAt(LocalDateTime.now());
        assertNotNull(sensor.getId());
        return reading;
    }
}
