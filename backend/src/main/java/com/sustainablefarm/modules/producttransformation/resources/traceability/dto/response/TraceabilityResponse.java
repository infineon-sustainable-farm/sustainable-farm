package com.sustainablefarm.modules.producttransformation.resources.traceability.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Traceability Response DTO
 * Represents the complete traceability chain for a batch from harvest to sales
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceabilityResponse {
    
    private String batchId;
    private BatchInfo batch;
    private HarvestInfo harvest;
    private RawIntakeInfo rawIntake;
    private List<WashSortInfo> washSortRecords;
    private List<DryingRunInfo> dryingRuns;
    private List<QcCheckpointInfo> qcCheckpoints;
    private List<ComplianceInfo> complianceRecords;
    private PackagingInfo packaging;
    private LocalDateTime lastUpdated;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchInfo {
        private String batchId;
        private LocalDate harvestDate;
        private String mangoVariety;
        private BigDecimal harvestQuantityKg;
        private String currentStatus;
        private String farmId;
        private String blockId;
        private LocalDateTime createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HarvestInfo {
        private String harvestId;
        private LocalDate harvestDate;
        private String harvestTime;
        private String mangoVariety;
        private String farmId;
        private String blockId;
        private BigDecimal harvestQuantityKg;
        private String qualityGrade;
        private String harvestTeamId;
        private String harvestSupervisor;
        private String weatherConditions;
        private String storageLocation;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawIntakeInfo {
        private String intakeId;
        private String sourceFarm;
        private String sourceBlock;
        private LocalDate intakeDate;
        private BigDecimal receivedQuantityKg;
        private String receivedVariety;
        private String receivedGrade;
        private String intakeOperator;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WashSortInfo {
        private String recordId;
        private BigDecimal inputQuantityKg;
        private BigDecimal outputQuantityKg;
        private BigDecimal wasteQuantityKg;
        private BigDecimal waterUsageLiters;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String equipmentId;
        private String equipmentName;
        private String operatorId;
        private String operatorName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DryingRunInfo {
        private String runId;
        private BigDecimal durationHours;
        private BigDecimal targetTemperatureC;
        private BigDecimal actualTemperatureC;
        private BigDecimal startMoisturePct;
        private BigDecimal endMoisturePct;
        private BigDecimal energyUsageKwh;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String equipmentId;
        private String equipmentName;
        private String operatorId;
        private String operatorName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QcCheckpointInfo {
        private String checkpointId;
        private String stage;
        private String result;
        private String defects;
        private Integer defectsCount;
        private String inspectorId;
        private String inspectorName;
        private LocalDateTime checkpointTime;
        private String notes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceInfo {
        private String recordId;
        private String complianceType;
        private String requirement;
        private String result;
        private String evidence;
        private String auditorId;
        private String auditorName;
        private LocalDate auditDate;
        private LocalDate nextAuditDate;
        private boolean auditOverdue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackagingInfo {
        private String recordId;
        private String packageType;
        private BigDecimal packageQuantityKg;
        private String lotCode;
        private boolean exportReady;
        private LocalDate packagingDate;
        private String equipmentId;
        private String equipmentName;
        private String operatorId;
        private String operatorName;
    }
}