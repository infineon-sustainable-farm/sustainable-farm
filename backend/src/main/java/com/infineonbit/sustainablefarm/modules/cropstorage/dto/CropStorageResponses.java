package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.BatchStatus;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropProductType;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.MovementType;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.OperationalStatus;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.ProductStage;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The JSON answers of the Crop Storage API (ADR 0002: records, never an entity). They keep
 * the field names the screens already read; a related row is a small reference record
 * (id, code, name) instead of the whole entity. Built by CropStorageMapper.
 */
public final class CropStorageResponses {

    private CropStorageResponses() {
    }

    /** A storage zone as referenced by another row. */
    public record ZoneRef(Long id, String zoneCode, String name) {
    }

    /** A supplier as referenced by a batch. */
    public record SupplierRef(Long id, String supplierCode, String name) {
    }

    /** A batch as referenced by a movement, a check, a loss or an alert. */
    public record BatchRef(Long id, String batchCode) {
    }

    /** A sensor as referenced by a reading or an alert. */
    public record SensorRef(Long id, String sensorCode, String name) {
    }

    public record ZoneResponse(
            Long id,
            String zoneCode,
            String name,
            String description,
            BigDecimal capacityKg,
            BigDecimal temperatureMinCelsius,
            BigDecimal temperatureMaxCelsius,
            BigDecimal humidityMinPercent,
            BigDecimal humidityMaxPercent,
            Boolean coolingAvailable,
            OperationalStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record SupplierResponse(
            Long id,
            String supplierCode,
            String name,
            String region,
            String contactPhone,
            Instant createdAt) {
    }

    /**
     * mangoVariety is the name of the Plants catalogue entry whose key is varietyKey;
     * valuePerKgFcfa is the XOF amount of the stored value per kilogram.
     */
    public record BatchResponse(
            Long id,
            Long version,
            String batchCode,
            CropProductType productType,
            ProductStage productStage,
            String origin,
            SupplierRef supplier,
            String varietyKey,
            String mangoVariety,
            LocalDate harvestDate,
            Long harvestId,
            LocalDate processingDate,
            LocalDate packagingDate,
            LocalDate storageEntryDate,
            BigDecimal initialQuantityKg,
            BigDecimal currentQuantityKg,
            BigDecimal moisturePercent,
            String qualityGrade,
            BatchStatus status,
            ZoneRef storageZone,
            LocalDate expectedDispatchDate,
            LocalDate expiryDate,
            Integer customerPriority,
            BigDecimal valuePerKgFcfa,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record MovementResponse(
            Long id,
            MovementType type,
            BigDecimal quantityKg,
            BatchRef batch,
            ZoneRef fromZone,
            ZoneRef toZone,
            String reason,
            LocalDateTime movedAt) {
    }

    public record SensorResponse(
            Long id,
            String sensorCode,
            String sensorType,
            String name,
            ZoneRef storageZone,
            OperationalStatus status,
            LocalDate installationDate,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record SensorReadingResponse(
            Long id,
            SensorRef sensor,
            BigDecimal temperature,
            BigDecimal humidity,
            LocalDateTime measuredAt) {
    }

    /** inspectorName is the full name of the person from the staff directory. */
    public record QualityCheckResponse(
            Long id,
            BatchRef batch,
            BigDecimal moisturePercent,
            String qualityGrade,
            Boolean passed,
            Long inspectorPersonId,
            String inspectorName,
            String notes,
            LocalDateTime checkedAt) {
    }

    /** valueFcfa is the XOF amount of the lost stock value. */
    public record LossRecordResponse(
            Long id,
            BatchRef batch,
            BigDecimal quantityKg,
            String cause,
            BigDecimal valueFcfa,
            LocalDateTime recordedAt) {
    }

    public record AlertResponse(
            Long id,
            String type,
            String severity,
            String message,
            String status,
            SensorRef sensor,
            ZoneRef storageZone,
            BatchRef batch,
            String correctiveAction,
            LocalDateTime createdAt) {
    }

    /**
     * Inventory valuation in every reporting unit. Only kg and XOF are stored; the other
     * units are derived (see CropStorageBatchService#summary).
     */
    public record ValuationSummary(
            BigDecimal totalKg,
            BigDecimal totalTonnes,
            BigDecimal totalLb,
            BigDecimal totalM3,
            BigDecimal totalValueFcfa,
            BigDecimal totalValueEur,
            BigDecimal totalValueUsd) {
    }
}
