package com.infineonbit.sustainablefarm.modules.cropstorage.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.LossRecord;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.QualityCheck;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Supplier;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.AlertService;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.BatchService;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.LossRecordService;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.QualityCheckService;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.SensorReadingService;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.SensorService;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.StockMovementService;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.StorageZoneService;
import com.infineonbit.sustainablefarm.modules.cropstorage.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AlertController.class,
        BatchController.class,
        LossRecordController.class,
        QualityCheckController.class,
        SensorController.class,
        SensorReadingController.class,
        StockMovementController.class,
        StorageZoneController.class,
        SupplierController.class
})
@Import(CoreExceptionHandler.class)
class CropStorageCreateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @MockitoBean
    private BatchService batchService;

    @MockitoBean
    private LossRecordService lossRecordService;

    @MockitoBean
    private QualityCheckService qualityCheckService;

    @MockitoBean
    private SensorService sensorService;

    @MockitoBean
    private SensorReadingService sensorReadingService;

    @MockitoBean
    private StockMovementService stockMovementService;

    @MockitoBean
    private StorageZoneService storageZoneService;

    @MockitoBean
    private SupplierService supplierService;

    @Test
    void createAlertIgnoresEntityAndNestedServerFields() throws Exception {
        when(alertService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/crop-storage/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"createdAt":"2000-01-01T00:00:00","type":"DAMAGE",
                                 "severity":"HIGH","message":"Observed","status":"ACTIVE",
                                 "correctiveAction":"Inspect",
                                 "sensor":{"id":5,"sensorCode":"overposted"},
                                 "storageZone":{"id":6,"zoneCode":"overposted","capacityKg":999},
                                 "batch":{"id":7,"batchCode":"overposted","version":44}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DAMAGE"));

        var captor = org.mockito.ArgumentCaptor.forClass(Alert.class);
        verify(alertService).create(captor.capture());
        Alert saved = captor.getValue();
        assertNull(saved.getId());
        assertNull(saved.getCreatedAt());
        assertEquals(5L, saved.getSensor().getId());
        assertNull(saved.getSensor().getSensorCode());
        assertEquals(6L, saved.getStorageZone().getId());
        assertNull(saved.getStorageZone().getZoneCode());
        assertEquals(7L, saved.getBatch().getId());
        assertNull(saved.getBatch().getBatchCode());
    }

    @Test
    void createBatchIgnoresEntityVersionAndNestedServerFields() throws Exception {
        when(batchService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"version":55,"createdAt":"2000-01-01T00:00:00",
                                 "updatedAt":"2000-01-01T00:00:00","batchCode":"B-1",
                                 "productType":"MANGO","initialQuantityKg":10,"status":"STORED",
                                 "supplier":{"id":5,"name":"overposted","createdAt":"2000-01-01T00:00:00"},
                                 "storageZone":{"id":6,"zoneCode":"overposted","capacityKg":999}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchCode").value("B-1"));

        var captor = org.mockito.ArgumentCaptor.forClass(Batch.class);
        verify(batchService).create(captor.capture());
        Batch saved = captor.getValue();
        assertNull(saved.getId());
        assertNull(saved.getVersion());
        assertNull(saved.getCreatedAt());
        assertNull(saved.getUpdatedAt());
        assertEquals(0, saved.getCustomerPriority());
        assertEquals(5L, saved.getSupplier().getId());
        assertNull(saved.getSupplier().getName());
        assertNull(saved.getSupplier().getCreatedAt());
        assertEquals(6L, saved.getStorageZone().getId());
        assertNull(saved.getStorageZone().getZoneCode());
    }

    @Test
    void createLossRecordIgnoresEntityAndNestedBatchFields() throws Exception {
        when(lossRecordService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/loss-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"batch":{"id":7,"batchCode":"overposted","version":44},
                                 "quantityKg":2,"cause":"Damage","recordedAt":"2026-10-30T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityKg").value(2));

        var captor = org.mockito.ArgumentCaptor.forClass(LossRecord.class);
        verify(lossRecordService).create(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(7L, captor.getValue().getBatch().getId());
        assertNull(captor.getValue().getBatch().getBatchCode());
        assertNull(captor.getValue().getBatch().getVersion());
    }

    @Test
    void createQualityCheckIgnoresEntityAndNestedBatchFields() throws Exception {
        when(qualityCheckService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/quality-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"batch":{"id":7,"batchCode":"overposted","version":44},
                                 "moisturePercent":12,"passed":true,"checkedAt":"2026-10-30T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").value(true));

        var captor = org.mockito.ArgumentCaptor.forClass(QualityCheck.class);
        verify(qualityCheckService).create(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(7L, captor.getValue().getBatch().getId());
        assertNull(captor.getValue().getBatch().getBatchCode());
        assertNull(captor.getValue().getBatch().getVersion());
    }

    @Test
    void createSensorIgnoresEntityAndNestedZoneFields() throws Exception {
        when(sensorService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"createdAt":"2000-01-01T00:00:00",
                                 "updatedAt":"2000-01-01T00:00:00","sensorCode":"S-1",
                                 "status":"ACTIVE",
                                 "storageZone":{"id":6,"zoneCode":"overposted","capacityKg":999}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorCode").value("S-1"));

        var captor = org.mockito.ArgumentCaptor.forClass(Sensor.class);
        verify(sensorService).create(captor.capture());
        assertNull(captor.getValue().getId());
        assertNull(captor.getValue().getCreatedAt());
        assertNull(captor.getValue().getUpdatedAt());
        assertEquals(6L, captor.getValue().getStorageZone().getId());
        assertNull(captor.getValue().getStorageZone().getZoneCode());
    }

    @Test
    void createSensorReadingIgnoresEntityAndNestedSensorFields() throws Exception {
        when(sensorReadingService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"sensor":{"id":5,"sensorCode":"overposted","status":"DISABLED"},
                                 "temperature":21,"humidity":45,"measuredAt":"2026-10-30T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperature").value(21));

        var captor = org.mockito.ArgumentCaptor.forClass(SensorReading.class);
        verify(sensorReadingService).create(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(5L, captor.getValue().getSensor().getId());
        assertNull(captor.getValue().getSensor().getSensorCode());
        assertNull(captor.getValue().getSensor().getStatus());
    }

    @Test
    void createStockMovementIgnoresEntityAndNestedRelationshipFields() throws Exception {
        when(stockMovementService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/stock-movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"type":"TRANSFER","quantityKg":2,
                                 "batch":{"id":7,"batchCode":"overposted","version":44},
                                 "fromZone":{"id":6,"zoneCode":"overposted"},
                                 "toZone":{"id":8,"zoneCode":"overposted","capacityKg":999},
                                 "movedAt":"2026-10-30T10:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER"));

        var captor = org.mockito.ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementService).create(captor.capture());
        StockMovement saved = captor.getValue();
        assertNull(saved.getId());
        assertEquals(7L, saved.getBatch().getId());
        assertNull(saved.getBatch().getBatchCode());
        assertNull(saved.getBatch().getVersion());
        assertEquals(6L, saved.getFromZone().getId());
        assertNull(saved.getFromZone().getZoneCode());
        assertEquals(8L, saved.getToZone().getId());
        assertNull(saved.getToZone().getZoneCode());
    }

    @Test
    void createStorageZoneIgnoresEntityAndTimestamps() throws Exception {
        when(storageZoneService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/storage-zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"createdAt":"2000-01-01T00:00:00",
                                 "updatedAt":"2000-01-01T00:00:00","zoneCode":"Z-1",
                                 "capacityKg":100,"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneCode").value("Z-1"));

        var captor = org.mockito.ArgumentCaptor.forClass(StorageZone.class);
        verify(storageZoneService).create(captor.capture());
        assertNull(captor.getValue().getId());
        assertNull(captor.getValue().getCreatedAt());
        assertNull(captor.getValue().getUpdatedAt());
    }

    @Test
    void createSupplierIgnoresEntityIdAndTimestamp() throws Exception {
        when(supplierService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":91,"createdAt":"2000-01-01T00:00:00",
                                 "supplierCode":"SUP-1","name":"Supplier"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierCode").value("SUP-1"));

        var captor = org.mockito.ArgumentCaptor.forClass(Supplier.class);
        verify(supplierService).create(captor.capture());
        assertNull(captor.getValue().getId());
        assertNull(captor.getValue().getCreatedAt());
    }

    @Test
    void invalidBatchStillReturnsValidationErrorWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchCode":"B-1","productType":"MANGO",
                                 "initialQuantityKg":-1,"status":"STORED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.initialQuantityKg").exists());

        verify(batchService, never()).create(any());
    }

    @Test
    void nestedReferenceWithoutAnIdIsRejectedBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/sensor-readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sensor":{},"measuredAt":"2026-10-30T10:00:00"}
                                """))
                .andExpect(status().isBadRequest());

        verify(sensorReadingService, never()).create(any());
    }
}
