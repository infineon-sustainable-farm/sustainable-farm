package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.money.Money;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.AlertResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.BatchRef;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.BatchResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.LossRecordResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.MovementResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.QualityCheckResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.SensorReadingResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.SensorRef;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.SensorResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.SupplierRef;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.SupplierResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.ZoneRef;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.CropStorageResponses.ZoneResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Alert;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropStorageBatch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.LossRecord;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.QualityCheck;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Sensor;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.SensorReading;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Supplier;
import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the response records from the entities. Reads only the relations the
 * repositories fetch (entity graphs) and the names of shared concepts through
 * the api packages of their owners: the variety name from the Plants catalogue,
 * the inspector name from the staff directory (one call per list, not per row).
 */
@Component
@RequiredArgsConstructor
public class CropStorageMapper {

    private final VarietyCatalog varietyCatalog;
    private final PersonDirectory personDirectory;

    public ZoneResponse zone(StorageZone z) {
        return new ZoneResponse(z.getId(), z.getZoneCode(), z.getName(), z.getDescription(), z.getCapacityKg(),
                z.getTemperatureMinCelsius(), z.getTemperatureMaxCelsius(), z.getHumidityMinPercent(),
                z.getHumidityMaxPercent(), z.getCoolingAvailable(), z.getStatus(), z.getCreatedAt(),
                z.getUpdatedAt());
    }

    public SupplierResponse supplier(Supplier s) {
        return new SupplierResponse(s.getId(), s.getSupplierCode(), s.getName(), s.getRegion(),
                s.getContactPhone(), s.getCreatedAt());
    }

    public BatchResponse batch(CropStorageBatch b) {
        return batches(List.of(b)).getFirst();
    }

    public List<BatchResponse> batches(Collection<CropStorageBatch> batches) {
        Map<String, String> varietyNames = batches.stream().anyMatch(b -> b.getVarietyKey() != null)
                ? varietyCatalog.listVarieties().stream()
                        .collect(Collectors.toMap(CatalogVariety::key, CatalogVariety::name, (a, b) -> a))
                : Map.of();
        return batches.stream().map(b -> new BatchResponse(
                b.getId(), b.getVersion(), b.getBatchCode(), b.getProductType(), b.getProductStage(),
                b.getOrigin(), supplierRef(b.getSupplier()), b.getVarietyKey(),
                b.getVarietyKey() == null ? null : varietyNames.get(b.getVarietyKey()),
                b.getHarvestDate(), b.getHarvestId(), b.getProcessingDate(), b.getPackagingDate(),
                b.getStorageEntryDate(), b.getInitialQuantityKg(), b.getCurrentQuantityKg(),
                b.getMoisturePercent(), b.getQualityGrade(), b.getStatus(), zoneRef(b.getStorageZone()),
                b.getExpectedDispatchDate(), b.getExpiryDate(), b.getCustomerPriority(),
                amount(b.getValuePerKg()), b.getCreatedAt(), b.getUpdatedAt())).toList();
    }

    public MovementResponse movement(StockMovement m) {
        return new MovementResponse(m.getId(), m.getType(), m.getQuantityKg(), batchRef(m.getBatch()),
                zoneRef(m.getFromZone()), zoneRef(m.getToZone()), m.getReason(), m.getMovedAt());
    }

    public SensorResponse sensor(Sensor s) {
        return new SensorResponse(s.getId(), s.getSensorCode(), s.getSensorType(), s.getName(),
                zoneRef(s.getStorageZone()), s.getStatus(), s.getInstallationDate(), s.getCreatedAt(),
                s.getUpdatedAt());
    }

    public SensorReadingResponse reading(SensorReading r) {
        return new SensorReadingResponse(r.getId(), sensorRef(r.getSensor()), r.getTemperature(), r.getHumidity(),
                r.getMeasuredAt());
    }

    public QualityCheckResponse qualityCheck(QualityCheck c) {
        return qualityChecks(List.of(c)).getFirst();
    }

    public List<QualityCheckResponse> qualityChecks(Collection<QualityCheck> checks) {
        List<Long> inspectors = checks.stream().map(QualityCheck::getInspectorPersonId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> names = inspectors.isEmpty() ? Map.of() : personDirectory.namesOf(inspectors);
        return checks.stream().map(c -> new QualityCheckResponse(c.getId(), batchRef(c.getBatch()),
                c.getMoisturePercent(), c.getQualityGrade(), c.getPassed(), c.getInspectorPersonId(),
                c.getInspectorPersonId() == null ? null : names.get(c.getInspectorPersonId()), c.getNotes(),
                c.getCheckedAt())).toList();
    }

    public LossRecordResponse lossRecord(LossRecord l) {
        return new LossRecordResponse(l.getId(), batchRef(l.getBatch()), l.getQuantityKg(), l.getCause(),
                amount(l.getValue()), l.getRecordedAt());
    }

    public AlertResponse alert(Alert a) {
        return new AlertResponse(a.getId(), a.getType(), a.getSeverity(), a.getMessage(), a.getStatus(),
                sensorRef(a.getSensor()), zoneRef(a.getStorageZone()), batchRef(a.getBatch()),
                a.getCorrectiveAction(), a.getCreatedAt());
    }

    public static <T, R> List<R> all(Collection<T> rows, Function<T, R> mapping) {
        return rows.stream().map(mapping).toList();
    }

    private static ZoneRef zoneRef(StorageZone z) {
        return z == null ? null : new ZoneRef(z.getId(), z.getZoneCode(), z.getName());
    }

    private static SupplierRef supplierRef(Supplier s) {
        return s == null ? null : new SupplierRef(s.getId(), s.getSupplierCode(), s.getName());
    }

    private static BatchRef batchRef(CropStorageBatch b) {
        return b == null ? null : new BatchRef(b.getId(), b.getBatchCode());
    }

    private static SensorRef sensorRef(Sensor s) {
        return s == null ? null : new SensorRef(s.getId(), s.getSensorCode(), s.getName());
    }

    private static BigDecimal amount(Money money) {
        return money == null ? null : money.amount();
    }
}
