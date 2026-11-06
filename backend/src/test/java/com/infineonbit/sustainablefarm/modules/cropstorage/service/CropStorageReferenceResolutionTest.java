package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Supplier;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.AlertRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorReadingRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SensorRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.SupplierRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CropStorageReferenceResolutionTest {

    @Test
    void batchCreateReplacesNestedReferenceShellsWithStoredReferences() {
        BatchRepository batches = mock(BatchRepository.class);
        SupplierRepository suppliers = mock(SupplierRepository.class);
        StorageZoneRepository zones = mock(StorageZoneRepository.class);
        Supplier storedSupplier = new Supplier();
        StorageZone storedZone = new StorageZone();
        storedSupplier.setId(5L);
        storedZone.setId(6L);
        when(suppliers.findById(5L)).thenReturn(Optional.of(storedSupplier));
        when(zones.findById(6L)).thenReturn(Optional.of(storedZone));
        when(batches.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Batch request = new Batch();
        Supplier supplierReference = new Supplier();
        supplierReference.setId(5L);
        StorageZone zoneReference = new StorageZone();
        zoneReference.setId(6L);
        request.setSupplier(supplierReference);
        request.setStorageZone(zoneReference);

        new BatchService(batches, suppliers, zones).create(request);

        assertSame(storedSupplier, request.getSupplier());
        assertSame(storedZone, request.getStorageZone());
        verify(suppliers).findById(5L);
        verify(zones).findById(6L);
    }

    @Test
    void alertCreateReplacesAllNestedReferenceShellsWithStoredReferences() {
        AlertRepository alerts = mock(AlertRepository.class);
        SensorRepository sensors = mock(SensorRepository.class);
        StorageZoneRepository zones = mock(StorageZoneRepository.class);
        BatchRepository batches = mock(BatchRepository.class);
        Sensor storedSensor = new Sensor();
        StorageZone storedZone = new StorageZone();
        Batch storedBatch = new Batch();
        storedSensor.setId(5L);
        storedZone.setId(6L);
        storedBatch.setId(7L);
        when(sensors.findWithStorageZoneById(5L)).thenReturn(Optional.of(storedSensor));
        when(zones.findById(6L)).thenReturn(Optional.of(storedZone));
        when(batches.findWithRelationshipsById(7L)).thenReturn(Optional.of(storedBatch));
        when(alerts.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        Alert request = new Alert();
        Sensor sensorReference = new Sensor();
        sensorReference.setId(5L);
        StorageZone zoneReference = new StorageZone();
        zoneReference.setId(6L);
        Batch batchReference = new Batch();
        batchReference.setId(7L);
        request.setSensor(sensorReference);
        request.setStorageZone(zoneReference);
        request.setBatch(batchReference);
        when(alerts.findWithRelationshipsById(11L)).thenReturn(Optional.of(request));

        new AlertService(alerts, sensors, zones, batches).create(request);

        assertSame(storedSensor, request.getSensor());
        assertSame(storedZone, request.getStorageZone());
        assertSame(storedBatch, request.getBatch());
        verify(sensors).findWithStorageZoneById(5L);
        verify(batches).findWithRelationshipsById(7L);
        verify(alerts).findWithRelationshipsById(11L);
    }

    @Test
    void sensorCreateReplacesNestedZoneReferenceWithStoredZone() {
        SensorRepository sensors = mock(SensorRepository.class);
        StorageZoneRepository zones = mock(StorageZoneRepository.class);
        StorageZone storedZone = new StorageZone();
        storedZone.setId(6L);
        when(zones.findById(6L)).thenReturn(Optional.of(storedZone));
        when(sensors.save(any(Sensor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Sensor request = new Sensor();
        StorageZone zoneReference = new StorageZone();
        zoneReference.setId(6L);
        request.setStorageZone(zoneReference);

        new SensorService(sensors, zones).create(request);

        assertSame(storedZone, request.getStorageZone());
    }

    @Test
    void sensorReadingCreateAttachesStoredSensorInsteadOfRequestShell() {
        SensorReadingRepository readings = mock(SensorReadingRepository.class);
        SensorRepository sensors = mock(SensorRepository.class);
        AlertRepository alerts = mock(AlertRepository.class);
        Sensor storedSensor = new Sensor();
        storedSensor.setId(5L);
        when(sensors.findWithStorageZoneById(5L)).thenReturn(Optional.of(storedSensor));
        when(readings.save(any(SensorReading.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SensorReading request = new SensorReading();
        Sensor sensorReference = new Sensor();
        sensorReference.setId(5L);
        request.setSensor(sensorReference);

        new SensorReadingService(readings, sensors, alerts).create(request);

        assertSame(storedSensor, request.getSensor());
        verify(sensors).findWithStorageZoneById(5L);
    }

    @Test
    void stockMovementCreateReplacesBatchAndBothZoneReferenceShells() {
        StockMovementRepository movements = mock(StockMovementRepository.class);
        BatchRepository batches = mock(BatchRepository.class);
        StorageZoneRepository zones = mock(StorageZoneRepository.class);
        Batch storedBatch = new Batch();
        storedBatch.setId(7L);
        storedBatch.setCurrentQuantityKg(new BigDecimal("2"));
        StorageZone storedFromZone = new StorageZone();
        storedFromZone.setId(6L);
        StorageZone storedToZone = new StorageZone();
        storedToZone.setId(8L);
        storedToZone.setCapacityKg(new BigDecimal("10"));
        when(batches.findWithRelationshipsById(7L)).thenReturn(Optional.of(storedBatch));
        when(zones.findById(6L)).thenReturn(Optional.of(storedFromZone));
        when(zones.findById(8L)).thenReturn(Optional.of(storedToZone));
        when(batches.findByStorageZoneId(8L)).thenReturn(List.of());
        when(batches.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(movements.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement request = new StockMovement();
        request.setType("TRANSFER");
        request.setQuantityKg(BigDecimal.ONE);
        Batch batchReference = new Batch();
        batchReference.setId(7L);
        StorageZone fromReference = new StorageZone();
        fromReference.setId(6L);
        StorageZone toReference = new StorageZone();
        toReference.setId(8L);
        request.setBatch(batchReference);
        request.setFromZone(fromReference);
        request.setToZone(toReference);

        new StockMovementService(movements, batches, zones).create(request);

        assertSame(storedBatch, request.getBatch());
        assertSame(storedFromZone, request.getFromZone());
        assertSame(storedToZone, request.getToZone());
    }
}
