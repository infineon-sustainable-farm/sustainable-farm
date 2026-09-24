package com.sustainablefarm.modules.producttransformation.resources.traceability.impl;

import com.sustainablefarm.core.exception.ResourceNotFoundException;
import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.batch.repository.BatchRepository;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.repository.ComplianceRecordRepository;
import com.sustainablefarm.modules.producttransformation.resources.dryingrun.model.DryingRun;
import com.sustainablefarm.modules.producttransformation.resources.dryingrun.repository.DryingRunRepository;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.repository.HarvestEventRepository;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator;
import com.sustainablefarm.modules.producttransformation.resources.operator.repository.OperatorRepository;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.repository.PackagingRecordRepository;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.repository.QcCheckpointRepository;
import com.sustainablefarm.modules.producttransformation.resources.rawintake.model.RawIntake;
import com.sustainablefarm.modules.producttransformation.resources.rawintake.repository.RawIntakeRepository;
import com.sustainablefarm.modules.producttransformation.resources.traceability.dto.response.TraceabilityResponse;
import com.sustainablefarm.modules.producttransformation.resources.traceability.service.TraceabilityService;
import com.sustainablefarm.modules.producttransformation.resources.washsortrecord.model.WashSortRecord;
import com.sustainablefarm.modules.producttransformation.resources.washsortrecord.repository.WashSortRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of Traceability Service
 * Provides complete traceability chain from harvest to packaging
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Slf4j
public class TraceabilityServiceImpl implements TraceabilityService {

    @Autowired
    private BatchRepository batchRepository;
    
    @Autowired
    private HarvestEventRepository harvestEventRepository;
    
    @Autowired
    private RawIntakeRepository rawIntakeRepository;
    
    @Autowired
    private WashSortRecordRepository washSortRecordRepository;
    
    @Autowired
    private DryingRunRepository dryingRunRepository;
    
    @Autowired
    private QcCheckpointRepository qcCheckpointRepository;
    
    @Autowired
    private ComplianceRecordRepository complianceRecordRepository;
    
    @Autowired
    private PackagingRecordRepository packagingRecordRepository;
    
    @Autowired
    private OperatorRepository operatorRepository;

    @Override
    public TraceabilityResponse getBatchTraceability(String batchId) {
        log.info("Getting traceability for batch: {}", batchId);
        
        // Get batch
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found with ID: " + batchId));
        
        // Build traceability response
        TraceabilityResponse response = TraceabilityResponse.builder()
                .batchId(batchId)
                .batch(mapBatchInfo(batch))
                .lastUpdated(batch.getUpdatedAt() != null ? batch.getUpdatedAt().toLocalDateTime() : LocalDateTime.now())
                .build();
        
        // Get harvest event
        harvestEventRepository.findByBatchId(batchId).ifPresent(harvest -> {
            response.setHarvest(mapHarvestInfo(harvest));
        });
        
        // Get raw intake
        rawIntakeRepository.findByBatchBatchId(batchId).ifPresent(intake -> {
            response.setRawIntake(mapRawIntakeInfo(intake));
        });
        
        // Get wash sort records
        List<WashSortRecord> washSortRecords = washSortRecordRepository.findByBatchBatchId(batchId);
        response.setWashSortRecords(washSortRecords.stream()
                .map(this::mapWashSortInfo)
                .collect(Collectors.toList()));
        
        // Get drying runs
        List<DryingRun> dryingRuns = dryingRunRepository.findByBatchBatchId(batchId);
        response.setDryingRuns(dryingRuns.stream()
                .map(this::mapDryingRunInfo)
                .collect(Collectors.toList()));
        
        // Get QC checkpoints
        List<QcCheckpoint> qcCheckpoints = qcCheckpointRepository.findByBatchBatchId(batchId);
        response.setQcCheckpoints(qcCheckpoints.stream()
                .map(this::mapQcCheckpointInfo)
                .collect(Collectors.toList()));
        
        // Get compliance records
        List<ComplianceRecord> complianceRecords = complianceRecordRepository.findByBatchBatchId(batchId);
        response.setComplianceRecords(complianceRecords.stream()
                .map(this::mapComplianceInfo)
                .collect(Collectors.toList()));
        
        // Get packaging record
        List<PackagingRecord> packagingRecords = packagingRecordRepository.findByBatchBatchId(batchId);
        if (!packagingRecords.isEmpty()) {
            response.setPackaging(mapPackagingInfo(packagingRecords.get(0)));
        }
        
        log.info("Traceability chain built for batch: {}", batchId);
        return response;
    }
    
    private TraceabilityResponse.BatchInfo mapBatchInfo(Batch batch) {
        return TraceabilityResponse.BatchInfo.builder()
                .batchId(batch.getBatchId())
                .harvestDate(batch.getHarvestDate())
                .mangoVariety(batch.getMangoVariety() != null ? batch.getMangoVariety().name() : null)
                .harvestQuantityKg(batch.getHarvestQuantityKg())
                .currentStatus(batch.getCurrentStatus() != null ? batch.getCurrentStatus().name() : null)
                .farmId(batch.getFarmId())
                .blockId(batch.getBlockId())
                .createdAt(batch.getCreatedAt() != null ? batch.getCreatedAt().toLocalDateTime() : null)
                .build();
    }
    
    private TraceabilityResponse.HarvestInfo mapHarvestInfo(HarvestEvent harvest) {
        return TraceabilityResponse.HarvestInfo.builder()
                .harvestId(harvest.getHarvestId())
                .harvestDate(harvest.getHarvestDate())
                .harvestTime(harvest.getHarvestTime() != null ? harvest.getHarvestTime().toString() : null)
                .mangoVariety(harvest.getMangoVariety() != null ? harvest.getMangoVariety().name() : null)
                .farmId(harvest.getFarmId())
                .blockId(harvest.getBlockId())
                .harvestQuantityKg(harvest.getHarvestQuantityKg())
                .qualityGrade(harvest.getQualityGrade() != null ? harvest.getQualityGrade().name() : null)
                .harvestTeamId(harvest.getHarvestTeamId())
                .harvestSupervisor(harvest.getHarvestSupervisor())
                .weatherConditions(harvest.getWeatherConditions())
                .storageLocation(harvest.getStorageLocation())
                .build();
    }
    
    private TraceabilityResponse.RawIntakeInfo mapRawIntakeInfo(RawIntake intake) {
        return TraceabilityResponse.RawIntakeInfo.builder()
                .intakeId(intake.getIntakeId())
                .sourceFarm(intake.getSourceFarm())
                .sourceBlock(intake.getSourceBlock())
                .intakeDate(intake.getIntakeDate())
                .receivedQuantityKg(intake.getReceivedQuantityKg())
                .receivedVariety(intake.getReceivedVariety() != null ? intake.getReceivedVariety().name() : null)
                .receivedGrade(intake.getReceivedGrade() != null ? intake.getReceivedGrade().name() : null)
                .intakeOperator(intake.getIntakeOperator())
                .build();
    }
    
    private TraceabilityResponse.WashSortInfo mapWashSortInfo(WashSortRecord record) {
        String operatorName = record.getOperator() != null ? record.getOperator().getOperatorName() : null;
        String equipmentName = record.getEquipment() != null ? record.getEquipment().getEquipmentName() : null;
        
        return TraceabilityResponse.WashSortInfo.builder()
                .recordId(record.getRecordId())
                .inputQuantityKg(record.getInputQuantityKg())
                .outputQuantityKg(record.getOutputQuantityKg())
                .wasteQuantityKg(record.getWasteQuantityKg())
                .waterUsageLiters(record.getWaterUsageLiters())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .equipmentId(record.getEquipment() != null ? record.getEquipment().getEquipmentId() : null)
                .equipmentName(equipmentName)
                .operatorId(record.getOperator() != null ? record.getOperator().getOperatorId() : null)
                .operatorName(operatorName)
                .build();
    }
    
    private TraceabilityResponse.DryingRunInfo mapDryingRunInfo(DryingRun run) {
        String operatorName = run.getOperator() != null ? run.getOperator().getOperatorName() : null;
        String equipmentName = run.getEquipment() != null ? run.getEquipment().getEquipmentName() : null;
        
        return TraceabilityResponse.DryingRunInfo.builder()
                .runId(run.getRunId())
                .durationHours(run.getDurationHours())
                .targetTemperatureC(run.getTargetTemperatureC())
                .actualTemperatureC(run.getActualTemperatureC())
                .startMoisturePct(run.getStartMoisturePct())
                .endMoisturePct(run.getEndMoisturePct())
                .energyUsageKwh(run.getEnergyUsageKwh())
                .startTime(run.getStartTime())
                .endTime(run.getEndTime())
                .equipmentId(run.getEquipment() != null ? run.getEquipment().getEquipmentId() : null)
                .equipmentName(equipmentName)
                .operatorId(run.getOperator() != null ? run.getOperator().getOperatorId() : null)
                .operatorName(operatorName)
                .build();
    }
    
    private TraceabilityResponse.QcCheckpointInfo mapQcCheckpointInfo(QcCheckpoint checkpoint) {
        String inspectorName = checkpoint.getInspector() != null ? checkpoint.getInspector().getOperatorName() : null;
        
        return TraceabilityResponse.QcCheckpointInfo.builder()
                .checkpointId(checkpoint.getCheckpointId())
                .stage(checkpoint.getStage() != null ? checkpoint.getStage().name() : null)
                .result(checkpoint.getResult() != null ? checkpoint.getResult().name() : null)
                .defects(checkpoint.getDefects())
                .defectsCount(checkpoint.getDefectsCount())
                .inspectorId(checkpoint.getInspector() != null ? checkpoint.getInspector().getOperatorId() : null)
                .inspectorName(inspectorName)
                .checkpointTime(checkpoint.getCheckpointTime())
                .notes(checkpoint.getNotes())
                .build();
    }
    
    private TraceabilityResponse.ComplianceInfo mapComplianceInfo(ComplianceRecord record) {
        String auditorName = record.getAuditor() != null ? record.getAuditor().getOperatorName() : null;
        
        return TraceabilityResponse.ComplianceInfo.builder()
                .recordId(record.getRecordId())
                .complianceType(record.getComplianceType() != null ? record.getComplianceType().name() : null)
                .requirement(record.getRequirement())
                .result(record.getResult() != null ? record.getResult().name() : null)
                .evidence(record.getEvidence())
                .auditorId(record.getAuditor() != null ? record.getAuditor().getOperatorId() : null)
                .auditorName(auditorName)
                .auditDate(record.getAuditDate())
                .nextAuditDate(record.getNextAuditDate())
                .auditOverdue(record.isAuditOverdue())
                .build();
    }
    
    private TraceabilityResponse.PackagingInfo mapPackagingInfo(PackagingRecord packaging) {
        String operatorName = packaging.getOperator() != null ? packaging.getOperator().getOperatorName() : null;
        String equipmentName = packaging.getEquipment() != null ? packaging.getEquipment().getEquipmentName() : null;
        
        return TraceabilityResponse.PackagingInfo.builder()
                .recordId(packaging.getRecordId())
                .packageType(packaging.getPackageType() != null ? packaging.getPackageType().name() : null)
                .packageQuantityKg(packaging.getPackageQuantityKg())
                .lotCode(packaging.getLotCode())
                .exportReady(packaging.getExportReady())
                .packagingDate(packaging.getPackagingDate())
                .equipmentId(packaging.getEquipment() != null ? packaging.getEquipment().getEquipmentId() : null)
                .equipmentName(equipmentName)
                .operatorId(packaging.getOperator() != null ? packaging.getOperator().getOperatorId() : null)
                .operatorName(operatorName)
                .build();
    }
}