package com.infineonbit.sustainablefarm.modules.cropstorage.dto;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchCreateRequest(
        @NotBlank String batchCode,
        @NotBlank String productType,
        String productStage,
        String origin,
        @Valid EntityReferenceRequest supplier,
        String mangoVariety,
        LocalDate harvestDate,
        Long harvestId,
        LocalDate processingDate,
        LocalDate packagingDate,
        LocalDate storageEntryDate,
        @NotNull @PositiveOrZero BigDecimal initialQuantityKg,
        @PositiveOrZero BigDecimal currentQuantityKg,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal moisturePercent,
        String qualityGrade,
        @NotBlank String status,
        @Valid EntityReferenceRequest storageZone,
        LocalDate expectedDispatchDate,
        LocalDate expiryDate,
        @NotNull @Min(0) Integer customerPriority,
        @PositiveOrZero BigDecimal valuePerKgFcfa) {

    public BatchCreateRequest {
        if (customerPriority == null) {
            customerPriority = 0;
        }
    }

    public Batch toEntity() {
        Batch batch = new Batch();
        batch.setBatchCode(batchCode);
        batch.setProductType(productType);
        batch.setProductStage(productStage);
        batch.setOrigin(origin);
        batch.setSupplier(supplier == null ? null : supplier.toSupplier());
        batch.setMangoVariety(mangoVariety);
        batch.setHarvestDate(harvestDate);
        batch.setHarvestId(harvestId);
        batch.setProcessingDate(processingDate);
        batch.setPackagingDate(packagingDate);
        batch.setStorageEntryDate(storageEntryDate);
        batch.setInitialQuantityKg(initialQuantityKg);
        batch.setCurrentQuantityKg(currentQuantityKg);
        batch.setMoisturePercent(moisturePercent);
        batch.setQualityGrade(qualityGrade);
        batch.setStatus(status);
        batch.setStorageZone(storageZone == null ? null : storageZone.toStorageZone());
        batch.setExpectedDispatchDate(expectedDispatchDate);
        batch.setExpiryDate(expiryDate);
        batch.setCustomerPriority(customerPriority);
        batch.setValuePerKgFcfa(valuePerKgFcfa);
        return batch;
    }
}
