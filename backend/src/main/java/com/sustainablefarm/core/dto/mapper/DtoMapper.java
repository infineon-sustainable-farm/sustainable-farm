package com.sustainablefarm.core.dto.mapper;

import com.sustainablefarm.dto.request.*;
import com.sustainablefarm.dto.response.*;
import com.sustainablefarm.model.*;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Entities and DTOs
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Component
public class DtoMapper {

    // ===== BATCH MAPPERS =====

    public Batch toEntity(BatchCreateRequest request) {
        Batch batch = new Batch();
        batch.setBatchId(request.getBatchId());
        batch.setHarvestDate(request.getHarvestDate());
        batch.setMangoVariety(request.getMangoVariety());
        batch.setHarvestQuantityKg(request.getHarvestQuantityKg());
        batch.setFarmId(request.getFarmId());
        batch.setBlockId(request.getBlockId());
        batch.setCurrentStatus(request.getCurrentStatus());
        return batch;
    }

    public BatchResponse toResponse(Batch entity) {
        return BatchResponse.builder()
                .batchId(entity.getBatchId())
                .harvestDate(entity.getHarvestDate())
                .mangoVariety(entity.getMangoVariety())
                .harvestQuantityKg(entity.getHarvestQuantityKg())
                .currentStatus(entity.getCurrentStatus())
                .farmId(entity.getFarmId())
                .blockId(entity.getBlockId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ===== RAW INTAKE MAPPERS =====

    public RawIntake toEntity(RawIntakeCreateRequest request, Batch batch) {
        RawIntake intake = new RawIntake();
        intake.setIntakeId(request.getIntakeId());
        intake.setBatch(batch);
        intake.setSourceFarm(request.getSourceFarm());
        intake.setSourceBlock(request.getSourceBlock());
        intake.setIntakeDate(request.getIntakeDate());
        intake.setReceivedQuantityKg(request.getReceivedQuantityKg());
        intake.setReceivedVariety(request.getReceivedVariety());
        intake.setReceivedGrade(request.getReceivedGrade());
        intake.setIntakeOperator(request.getIntakeOperator());
        return intake;
    }

    public RawIntakeResponse toResponse(RawIntake entity) {
        return RawIntakeResponse.builder()
                .intakeId(entity.getIntakeId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getBatchId() : null)
                .sourceFarm(entity.getSourceFarm())
                .sourceBlock(entity.getSourceBlock())
                .intakeDate(entity.getIntakeDate())
                .receivedQuantityKg(entity.getReceivedQuantityKg())
                .receivedVariety(entity.getReceivedVariety())
                .receivedGrade(entity.getReceivedGrade())
                .intakeOperator(entity.getIntakeOperator())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ===== WASH SORT RECORD MAPPERS =====

    public WashSortRecord toEntity(WashSortRecordCreateRequest request, Batch batch, Equipment equipment, Operator operator) {
        WashSortRecord record = new WashSortRecord();
        record.setRecordId(request.getRecordId());
        record.setBatch(batch);
        record.setInputQuantityKg(request.getInputQuantityKg());
        record.setOutputQuantityKg(request.getOutputQuantityKg());
        record.setWasteQuantityKg(request.getWasteQuantityKg());
        record.setWaterUsageLiters(request.getWaterUsageLiters());
        record.setStartTime(request.getStartTime());
        record.setEndTime(request.getEndTime());
        record.setEquipment(equipment);
        record.setOperator(operator);
        return record;
    }

    public WashSortRecordResponse toResponse(WashSortRecord entity) {
        return WashSortRecordResponse.builder()
                .recordId(entity.getRecordId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getBatchId() : null)
                .inputQuantityKg(entity.getInputQuantityKg())
                .outputQuantityKg(entity.getOutputQuantityKg())
                .wasteQuantityKg(entity.getWasteQuantityKg())
                .waterUsageLiters(entity.getWaterUsageLiters())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .equipmentId(entity.getEquipment() != null ? entity.getEquipment().getEquipmentId() : null)
                .operatorId(entity.getOperator() != null ? entity.getOperator().getOperatorId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .yieldPercentage(entity.calculateYieldPercentage())
                .wastePercentage(entity.calculateWastePercentage())
                .build();
    }

    // ===== DRYING RUN MAPPERS =====

    public DryingRun toEntity(DryingRunCreateRequest request, Batch batch, Equipment equipment, Operator operator) {
        DryingRun run = new DryingRun();
        run.setRunId(request.getRunId());
        run.setBatch(batch);
        run.setDurationHours(request.getDurationHours());
        run.setTargetTemperatureC(request.getTargetTemperatureC());
        run.setActualTemperatureC(request.getActualTemperatureC());
        run.setStartMoisturePct(request.getStartMoisturePct());
        run.setEndMoisturePct(request.getEndMoisturePct());
        run.setEnergyUsageKwh(request.getEnergyUsageKwh());
        run.setStartTime(request.getStartTime());
        run.setEndTime(request.getEndTime());
        run.setEquipment(equipment);
        run.setOperator(operator);
        return run;
    }

    public DryingRunResponse toResponse(DryingRun entity) {
        return DryingRunResponse.builder()
                .runId(entity.getRunId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getBatchId() : null)
                .durationHours(entity.getDurationHours())
                .targetTemperatureC(entity.getTargetTemperatureC())
                .actualTemperatureC(entity.getActualTemperatureC())
                .startMoisturePct(entity.getStartMoisturePct())
                .endMoisturePct(entity.getEndMoisturePct())
                .energyUsageKwh(entity.getEnergyUsageKwh())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .equipmentId(entity.getEquipment() != null ? entity.getEquipment().getEquipmentId() : null)
                .operatorId(entity.getOperator() != null ? entity.getOperator().getOperatorId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .moistureReductionPct(entity.calculateMoistureReductionPct())
                .energyEfficiencyKwhPerKg(entity.calculateEnergyEfficiencyKwhPerKg())
                .withinTargetRange(entity.isWithinTargetRange())
                .build();
    }

    // ===== PACKAGING RECORD MAPPERS =====

    public PackagingRecord toEntity(PackagingRecordCreateRequest request, Batch batch, Equipment equipment, Operator operator) {
        PackagingRecord record = new PackagingRecord();
        record.setRecordId(request.getRecordId());
        record.setBatch(batch);
        record.setPackageType(request.getPackageType());
        record.setPackageQuantityKg(request.getPackageQuantityKg());
        record.setLotCode(request.getLotCode());
        record.setExportReady(request.getExportReady() != null ? request.getExportReady() : false);
        record.setPackagingDate(request.getPackagingDate());
        record.setEquipment(equipment);
        record.setOperator(operator);
        return record;
    }

    public PackagingRecordResponse toResponse(PackagingRecord entity) {
        return PackagingRecordResponse.builder()
                .recordId(entity.getRecordId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getBatchId() : null)
                .packageType(entity.getPackageType())
                .packageQuantityKg(entity.getPackageQuantityKg())
                .lotCode(entity.getLotCode())
                .exportReady(entity.getExportReady())
                .packagingDate(entity.getPackagingDate())
                .equipmentId(entity.getEquipment() != null ? entity.getEquipment().getEquipmentId() : null)
                .operatorId(entity.getOperator() != null ? entity.getOperator().getOperatorId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ===== QC CHECKPOINT MAPPERS =====

    public QcCheckpoint toEntity(QcCheckpointCreateRequest request, Batch batch, Operator inspector) {
        QcCheckpoint checkpoint = new QcCheckpoint();
        checkpoint.setCheckpointId(request.getCheckpointId());
        checkpoint.setBatch(batch);
        checkpoint.setStage(request.getStage());
        checkpoint.setResult(request.getResult());
        checkpoint.setDefects(request.getDefects());
        checkpoint.setDefectsCount(request.getDefectsCount());
        checkpoint.setInspector(inspector);
        checkpoint.setCheckpointTime(request.getCheckpointTime());
        checkpoint.setNotes(request.getNotes());
        return checkpoint;
    }

    public QcCheckpointResponse toResponse(QcCheckpoint entity) {
        return QcCheckpointResponse.builder()
                .checkpointId(entity.getCheckpointId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getBatchId() : null)
                .stage(entity.getStage())
                .result(entity.getResult())
                .defects(entity.getDefects())
                .defectsCount(entity.getDefectsCount())
                .inspectorId(entity.getInspector() != null ? entity.getInspector().getOperatorId() : null)
                .checkpointTime(entity.getCheckpointTime())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .mandatory(entity.isMandatory())
                .passed(entity.hasPassed())
                .build();
    }

    // ===== COMPLIANCE RECORD MAPPERS =====

    public ComplianceRecord toEntity(ComplianceRecordCreateRequest request, Batch batch, Operator auditor) {
        ComplianceRecord record = new ComplianceRecord();
        record.setRecordId(request.getRecordId());
        record.setBatch(batch);
        record.setComplianceType(request.getComplianceType());
        record.setRequirement(request.getRequirement());
        record.setResult(request.getResult());
        record.setEvidence(request.getEvidence());
        record.setAuditor(auditor);
        record.setAuditDate(request.getAuditDate());
        record.setNextAuditDate(request.getNextAuditDate());
        return record;
    }

    public ComplianceRecordResponse toResponse(ComplianceRecord entity) {
        return ComplianceRecordResponse.builder()
                .recordId(entity.getRecordId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getBatchId() : null)
                .complianceType(entity.getComplianceType())
                .requirement(entity.getRequirement())
                .result(entity.getResult())
                .evidence(entity.getEvidence())
                .auditorId(entity.getAuditor() != null ? entity.getAuditor().getOperatorId() : null)
                .auditDate(entity.getAuditDate())
                .nextAuditDate(entity.getNextAuditDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .compliant(entity.isCompliant())
                .auditOverdue(entity.isAuditOverdue())
                .build();
    }

    // ===== EQUIPMENT MAPPERS =====

    public Equipment toEntity(EquipmentCreateRequest request) {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(request.getEquipmentId());
        equipment.setEquipmentName(request.getEquipmentName());
        equipment.setEquipmentType(request.getEquipmentType());
        equipment.setCapacityKgPerHour(request.getCapacityKgPerHour());
        equipment.setEnergyConsumptionKwhPerKg(request.getEnergyConsumptionKwhPerKg());
        equipment.setLocation(request.getLocation());
        equipment.setMaintenanceStatus(request.getMaintenanceStatus());
        equipment.setLastMaintenanceDate(request.getLastMaintenanceDate());
        return equipment;
    }

    public EquipmentResponse toResponse(Equipment entity) {
        return EquipmentResponse.builder()
                .equipmentId(entity.getEquipmentId())
                .equipmentName(entity.getEquipmentName())
                .equipmentType(entity.getEquipmentType())
                .capacityKgPerHour(entity.getCapacityKgPerHour())
                .energyConsumptionKwhPerKg(entity.getEnergyConsumptionKwhPerKg())
                .location(entity.getLocation())
                .maintenanceStatus(entity.getMaintenanceStatus())
                .lastMaintenanceDate(entity.getLastMaintenanceDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ===== OPERATOR MAPPERS =====

    public Operator toEntity(OperatorCreateRequest request) {
        Operator operator = new Operator();
        operator.setOperatorId(request.getOperatorId());
        operator.setOperatorName(request.getOperatorName());
        operator.setRole(request.getRole());
        operator.setCertifications(request.getCertifications());
        operator.setActiveStatus(request.getActiveStatus());
        operator.setHireDate(request.getHireDate());
        return operator;
    }

    public OperatorResponse toResponse(Operator entity) {
        return OperatorResponse.builder()
                .operatorId(entity.getOperatorId())
                .operatorName(entity.getOperatorName())
                .role(entity.getRole())
                .certifications(entity.getCertifications())
                .activeStatus(entity.getActiveStatus())
                .hireDate(entity.getHireDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ===== HARVEST EVENT MAPPERS =====

    public HarvestEvent toEntity(HarvestEventCreateRequest request) {
        HarvestEvent event = new HarvestEvent();
        event.setHarvestId(request.getHarvestId());
        event.setBatchId(request.getBatchId());
        event.setHarvestDate(request.getHarvestDate());
        event.setHarvestTime(request.getHarvestTime());
        event.setMangoVariety(request.getMangoVariety());
        event.setFarmId(request.getFarmId());
        event.setBlockId(request.getBlockId());
        event.setHarvestQuantityKg(request.getHarvestQuantityKg());
        event.setQualityGrade(request.getQualityGrade());
        event.setQualityGradeDescription(request.getQualityGradeDescription());
        event.setHarvestTeamId(request.getHarvestTeamId());
        event.setHarvestSupervisor(request.getHarvestSupervisor());
        event.setWeatherConditions(request.getWeatherConditions());
        event.setStorageLocation(request.getStorageLocation());
        return event;
    }

    public HarvestEventResponse toResponse(HarvestEvent entity) {
        return HarvestEventResponse.builder()
                .harvestId(entity.getHarvestId())
                .batchId(entity.getBatchId())
                .harvestDate(entity.getHarvestDate())
                .harvestTime(entity.getHarvestTime())
                .mangoVariety(entity.getMangoVariety())
                .farmId(entity.getFarmId())
                .blockId(entity.getBlockId())
                .harvestQuantityKg(entity.getHarvestQuantityKg())
                .qualityGrade(entity.getQualityGrade())
                .qualityGradeDescription(entity.getQualityGradeDescription())
                .harvestTeamId(entity.getHarvestTeamId())
                .harvestSupervisor(entity.getHarvestSupervisor())
                .weatherConditions(entity.getWeatherConditions())
                .storageLocation(entity.getStorageLocation())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ===== HISTORICAL HARVEST MAPPERS =====

    public HistoricalHarvest toEntity(HistoricalHarvestCreateRequest request) {
        HistoricalHarvest historical = new HistoricalHarvest();
        historical.setYear(request.getYear());
        historical.setMonth(request.getMonth());
        historical.setWeek(request.getWeek());
        historical.setMangoVariety(request.getMangoVariety());
        historical.setHarvestQuantityKg(request.getHarvestQuantityKg());
        historical.setQualityGradeAPct(request.getQualityGradeAPct());
        historical.setQualityGradeBPct(request.getQualityGradeBPct());
        historical.setQualityGradeCPct(request.getQualityGradeCPct());
        historical.setWeatherCondition(request.getWeatherCondition());
        historical.setRainfallMm(request.getRainfallMm());
        historical.setTemperatureAvgC(request.getTemperatureAvgC());
        return historical;
    }

    public HistoricalHarvestResponse toResponse(HistoricalHarvest entity) {
        return HistoricalHarvestResponse.builder()
                .year(entity.getYear())
                .month(entity.getMonth())
                .week(entity.getWeek())
                .mangoVariety(entity.getMangoVariety())
                .harvestQuantityKg(entity.getHarvestQuantityKg())
                .qualityGradeAPct(entity.getQualityGradeAPct())
                .qualityGradeBPct(entity.getQualityGradeBPct())
                .qualityGradeCPct(entity.getQualityGradeCPct())
                .weatherCondition(entity.getWeatherCondition())
                .rainfallMm(entity.getRainfallMm())
                .temperatureAvgC(entity.getTemperatureAvgC())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
