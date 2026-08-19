package com.sustainablefarm.dto.request;

import com.sustainablefarm.model.PackagingRecord.PackageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new PackagingRecord
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackagingRecordCreateRequest {

    @NotBlank(message = "Record ID is required")
    private String recordId;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Package type is required")
    private PackageType packageType;

    @NotNull(message = "Package quantity is required")
    @Positive(message = "Package quantity must be positive")
    private BigDecimal packageQuantityKg;

    @NotBlank(message = "Lot code is mandatory for traceability compliance")
    private String lotCode;

    private Boolean exportReady;

    @NotNull(message = "Packaging date is required")
    private LocalDate packagingDate;

    private String equipmentId;

    private String operatorId;
}
