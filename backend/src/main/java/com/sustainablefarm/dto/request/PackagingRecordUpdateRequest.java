package com.sustainablefarm.dto.request;

import com.sustainablefarm.model.PackagingRecord.PackageType;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating an existing PackagingRecord
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackagingRecordUpdateRequest {

    private PackageType packageType;

    @Positive(message = "Package quantity must be positive")
    private BigDecimal packageQuantityKg;

    // Lot code not updatable - mandatory for traceability compliance
    // private String lotCode;

    private Boolean exportReady;

    private LocalDate packagingDate;

    private String equipmentId;

    private String operatorId;
}
