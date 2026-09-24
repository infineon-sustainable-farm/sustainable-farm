package com.sustainablefarm.modules.producttransformation.resources.packagingrecord.dto.response;

import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord.PackageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * DTO for PackagingRecord response
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackagingRecordResponse {

    private String recordId;
    private String batchId;
    private PackageType packageType;
    private BigDecimal packageQuantityKg;
    private String lotCode;
    private Boolean exportReady;
    private LocalDate packagingDate;
    private String equipmentId;
    private String operatorId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
