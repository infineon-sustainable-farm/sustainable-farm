package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.cropstorage.CropStorageH2IsolationGuard;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropStorageBatch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.CropStorageBatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Zone capacity across the three writes that fill a zone: API-005 entry,
 * manual batch creation and IN movement. Isolation is enforced by
 * CropStorageH2IsolationGuard before the context (and its DataSource) exists.
 * Services run their own transactions (the test is not transactional).
 *
 * <p>H2 only: this shows the lock / check / write sequence works on a real
 * JPA stack, it is not a PostgreSQL proof.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:crop_storage_capacity_isolated;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never"
})
@ContextConfiguration(initializers = CropStorageH2IsolationGuard.class)
@Import({StorageEntryService.class, StockMovementService.class, CropStorageBatchService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CropStorageCapacityConcurrencyJpaTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private StorageEntryService entryService;
    @Autowired private CropStorageBatchService batchService;
    @Autowired private StockMovementService movementService;
    @Autowired private CropStorageBatchRepository batches;
    @Autowired private StorageZoneRepository zones;
    @Autowired private StockMovementRepository movements;

    @AfterEach
    void clear() {
        movements.deleteAll();
        batches.deleteAll();
        zones.deleteAll();
    }

    // ---------- manual creation ----------

    @Test
    void manualCreationAcceptsAnExactFitAndRefusesAnOverflow() {
        StorageZone zone = zone("100");

        batchService.create(newBatch(zone, "60", null));
        batchService.create(newBatch(zone, "40", null));
        assertThrows(ConflictException.class, () -> batchService.create(newBatch(zone, "0.01", null)));

        assertEquals(2, batches.count());
        assertEquals(0, new BigDecimal("100").compareTo(stored(zone)));
    }

    @Test
    void manualCreationCountsStockAlreadyStoredByAnEntryAndAMovement() {
        StorageZone zone = zone("100");
        CropStorageBatch existing = batchService.create(newBatch(zone, "10", null));
        entryService.registerEntry(entry(zone, "40"));
        movementService.create(in(existing, zone, "20"));          // zone now holds 70

        assertThrows(ConflictException.class, () -> batchService.create(newBatch(zone, "30.01", null)));
        batchService.create(newBatch(zone, "30", null));

        assertEquals(0, new BigDecimal("100").compareTo(stored(zone)));
    }

    @Test
    void manualCreationChecksTheCurrentQuantityWhenOneIsGiven() {
        StorageZone zone = zone("100");
        batchService.create(newBatch(zone, "90", null));

        // initial 50 but only 5 left in the batch: 90 + 5 fits.
        batchService.create(newBatch(zone, "50", "5"));
        assertThrows(ConflictException.class, () -> batchService.create(newBatch(zone, "6", "6")));
    }

    @Test
    void unknownZoneOnManualCreationWritesNothing() {
        StorageZone ghost = new StorageZone();
        ghost.setId(987654L);
        assertThrows(com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException.class,
                () -> batchService.create(newBatch(ghost, "1", null)));
        assertEquals(0, batches.count());
    }

    // ---------- IN movement ----------

    @Test
    void inWithoutDestinationIsRefusedAndChangesNothing() {
        StorageZone zone = zone("100");
        CropStorageBatch batch = batchService.create(newBatch(zone, "10", null));

        assertThrows(BusinessRuleException.class, () -> movementService.create(in(batch, null, "5")));

        assertEquals(0, new BigDecimal("10").compareTo(quantity(batch)));
        assertEquals(0, movements.count());
    }

    @Test
    void inAcceptsAnExactFitAndRefusesAnOverflowWithoutPartialWrite() {
        StorageZone zone = zone("100");
        batchService.create(newBatch(zone, "80", null));
        CropStorageBatch batch = batchService.create(newBatch(zone, "10", null));

        assertThrows(ConflictException.class, () -> movementService.create(in(batch, zone, "10.01")));
        assertEquals(0, new BigDecimal("10").compareTo(quantity(batch)));
        assertEquals(0, movements.count());

        movementService.create(in(batch, zone, "10"));
        assertEquals(0, new BigDecimal("20").compareTo(quantity(batch)));
        assertEquals(1, movements.count());
    }

    @Test
    void inCountsStockStoredByAnEntryAndAManualCreation() {
        StorageZone zone = zone("100");
        CropStorageBatch batch = batchService.create(newBatch(zone, "10", null));
        batchService.create(newBatch(zone, "30", null));
        entryService.registerEntry(entry(zone, "30"));             // zone now holds 70

        assertThrows(ConflictException.class, () -> movementService.create(in(batch, zone, "30.01")));
        movementService.create(in(batch, zone, "30"));

        assertEquals(0, new BigDecimal("100").compareTo(stored(zone)));
    }

    @Test
    void entryCountsStockStoredByAManualCreationAndAMovement() {
        StorageZone zone = zone("100");
        CropStorageBatch batch = batchService.create(newBatch(zone, "20", null));
        movementService.create(in(batch, zone, "50"));             // zone now holds 70

        assertThrows(ConflictException.class, () -> entryService.registerEntry(entry(zone, "30.01")));
        entryService.registerEntry(entry(zone, "30"));

        assertEquals(0, new BigDecimal("100").compareTo(stored(zone)));
    }

    // ---------- concurrency ----------

    /** One entry, one manual creation and one IN race for a zone that fits only two of them. */
    @RepeatedTest(5)
    void entryManualCreationAndInCannotOverfillAZoneTogether() throws Exception {
        StorageZone zone = zone("100");
        CropStorageBatch existing = batchService.create(newBatch(zone, "10", null));

        Map<String, Integer> outcomes = race(List.of(
                () -> entryService.registerEntry(entry(zone, "40")),
                () -> batchService.create(newBatch(zone, "40", null)),
                () -> movementService.create(in(existing, zone, "40"))));

        assertEquals(Map.of("conflict", 1, "ok", 2), outcomes);
        assertEquals(0, new BigDecimal("90").compareTo(stored(zone)), "10 + two accepted 40 kg writes");
    }

    @RepeatedTest(3)
    void concurrentManualCreationsCannotOverfillAZone() throws Exception {
        StorageZone zone = zone("100");
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> batchService.create(newBatch(zone, "40", null)));
        }

        assertEquals(Map.of("conflict", 2, "ok", 2), race(tasks));
        assertEquals(0, new BigDecimal("80").compareTo(stored(zone)));
    }

    /** Two INs on the same batch and one on another: any two fit, never three. */
    @RepeatedTest(3)
    void concurrentInMovementsIncludingOnTheSameBatchCannotOverfillAZone() throws Exception {
        StorageZone zone = zone("100");
        CropStorageBatch a = batchService.create(newBatch(zone, "10", null));
        CropStorageBatch b = batchService.create(newBatch(zone, "10", null));

        Map<String, Integer> outcomes = race(List.of(
                () -> movementService.create(in(a, zone, "40")),
                () -> movementService.create(in(a, zone, "40")),
                () -> movementService.create(in(b, zone, "40"))));

        assertEquals(Map.of("conflict", 1, "ok", 2), outcomes);
        assertEquals(0, new BigDecimal("100").compareTo(stored(zone)));
        assertEquals(2, movements.count());
    }

    // ---------- helpers (synthetic fixtures, not farm data) ----------

    /**
     * Starts all tasks at the same instant. Outcomes: "ok", "conflict" (409
     * capacity refusal) or the exception class name for anything else, so an
     * unexpected failure (optimistic lock, deadlock, timeout) fails the test.
     */
    private static Map<String, Integer> race(List<Callable<Object>> tasks) throws Exception {
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
            Map<String, Integer> outcomes = new TreeMap<>();
            for (Future<Object> future : futures) {
                String key;
                try {
                    future.get(30, TimeUnit.SECONDS);
                    key = "ok";
                } catch (ExecutionException e) {
                    key = e.getCause() instanceof ConflictException ? "conflict" : e.getCause().getClass().getName();
                }
                outcomes.merge(key, 1, Integer::sum);
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private BigDecimal stored(StorageZone zone) {
        return batches.sumCurrentQuantityKgByStorageZoneId(zone.getId());
    }

    private BigDecimal quantity(CropStorageBatch batch) {
        return batches.findById(batch.getId()).orElseThrow().getCurrentQuantityKg();
    }

    private StorageZone zone(String capacityKg) {
        StorageZone zone = new StorageZone();
        zone.setZoneCode("CAP-" + SEQ.incrementAndGet());
        zone.setCapacityKg(new BigDecimal(capacityKg));
        zone.setStatus("ACTIVE");
        return zones.save(zone);
    }

    /** A request-shaped batch: the zone travels as a bare id reference. */
    private static CropStorageBatch newBatch(StorageZone zone, String initialKg, String currentKg) {
        CropStorageBatch batch = new CropStorageBatch();
        batch.setBatchCode("MAN-" + SEQ.incrementAndGet());
        batch.setProductType("DRIED_MANGO");
        batch.setInitialQuantityKg(new BigDecimal(initialKg));
        batch.setCurrentQuantityKg(currentKg == null ? null : new BigDecimal(currentKg));
        batch.setStatus("STORED");
        batch.setCustomerPriority(0);
        batch.setStorageEntryDate(LocalDate.now());
        StorageZone ref = new StorageZone();
        ref.setId(zone.getId());
        batch.setStorageZone(ref);
        return batch;
    }

    private static StorageEntryRequest entry(StorageZone zone, String kg) {
        String batchId = "PT-CAP-" + SEQ.incrementAndGet();
        return new StorageEntryRequest(batchId, "PKG-" + batchId, new BigDecimal(kg), null,
                OffsetDateTime.now(ZoneOffset.UTC), "op-1", "line-1", String.valueOf(zone.getId()), "A",
                null, "farm-1", "block-1", "variety-1");
    }

    private static StockMovement in(CropStorageBatch batch, StorageZone toZone, String kg) {
        StockMovement movement = new StockMovement();
        movement.setType("IN");
        movement.setQuantityKg(new BigDecimal(kg));
        CropStorageBatch ref = new CropStorageBatch();
        ref.setId(batch.getId());
        movement.setBatch(ref);
        if (toZone != null) {
            StorageZone zoneRef = new StorageZone();
            zoneRef.setId(toZone.getId());
            movement.setToZone(zoneRef);
        }
        movement.setReason("test");
        movement.setMovedAt(LocalDateTime.now());
        return movement;
    }
}
