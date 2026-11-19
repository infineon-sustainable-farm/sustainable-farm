package com.infineonbit.sustainablefarm.modules.producttransformation.dashboard.service;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.historicalharvest.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.washsortrecord.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.dto.request.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.historicalharvest.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.washsortrecord.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.dto.response.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.rawintake.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.historicalharvest.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.compliancerecord.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.washsortrecord.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.*;
import com.infineonbit.sustainablefarm.modules.producttransformation.variety.MangoVarietyCodes;
import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentLookup;
import com.infineonbit.sustainablefarm.modules.machinery.api.EquipmentView;
import com.infineonbit.sustainablefarm.modules.producttransformation.block.ProductTransformationBlocks;
import com.infineonbit.sustainablefarm.modules.producttransformation.block.ProductTransformationBlocks.Label;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Entities and DTOs
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Component
public class DtoMapper {

    private final MangoVarietyCodes mangoVarietyCodes;
    private final EquipmentLookup equipmentLookup;
    private final ProductTransformationBlocks blocks;
    private final PersonDirectory personDirectory;

    public DtoMapper(MangoVarietyCodes mangoVarietyCodes, EquipmentLookup equipmentLookup,
                     ProductTransformationBlocks blocks, PersonDirectory personDirectory) {
        this.mangoVarietyCodes = mangoVarietyCodes;
        this.equipmentLookup = equipmentLookup;
        this.blocks = blocks;
        this.personDirectory = personDirectory;
    }

    /**
     * The person of the staff referential named by a free-text field of the API (intake
     * operator, harvest supervisor), created when nobody carries the name; {@code null} for a
     * blank name.
     */
    public Long personId(String fullName) {
        return personDirectory.idOf(fullName);
    }

    /**
     * The block a farm text and a block text designate, for an update: {@code null} when the block
     * text is blank, so that the update keeps the current block.
     *
     * @see ProductTransformationBlocks#resolve(String, String)
     */
    public Long blockIdOrNull(String farmText, String blockText) {
        return blocks.resolveOrNull(farmText, blockText);
    }

    /**
     * An audit instant of an entity ({@code core.entity.BaseEntity}) as the responses show it: a
     * {@link Timestamp}, so that the JSON keeps its format (milliseconds, {@code Z}).
     */
    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    /** The business code of the register equipment with this id, shown as {@code equipmentId}. */
    private String equipmentCode(Long equipmentId) {
        return equipmentLookup.findById(equipmentId).map(EquipmentView::code).orElse(null);
    }

    /**
     * The catalogue key of a variety sent to the API, for a path variable or an update.
     *
     * @see MangoVarietyCodes#toKey(String)
     */
    public String varietyKey(String apiValue) {
        return mangoVarietyCodes.toKey(apiValue);
    }

    // ===== BATCH MAPPERS =====

    public Batch toEntity(BatchCreateRequest request) {
        Batch batch = new Batch();
        batch.setBatchId(request.getBatchId());
        batch.setHarvestDate(request.getHarvestDate());
        batch.setVarietyKey(mangoVarietyCodes.toKey(request.getMangoVariety()));
        batch.setHarvestQuantityKg(request.getHarvestQuantityKg());
        batch.setFarmBlockId(blocks.resolve(request.getFarmId(), request.getBlockId()));
        batch.setCurrentStatus(request.getCurrentStatus());
        return batch;
    }

    public BatchResponse toResponse(Batch entity) {
        return toResponse(entity, blocks.label(entity.getFarmBlockId()));
    }

    /** The responses of several lots, with one lookup of their blocks. */
    public List<BatchResponse> toBatchResponses(List<Batch> entities) {
        Map<Long, Label> labels = blocks.labelsOf(entities, Batch::getFarmBlockId);
        return entities.stream().map(entity -> toResponse(entity, labelOf(labels, entity.getFarmBlockId()))).toList();
    }

    private static Label labelOf(Map<Long, Label> labels, Long blockId) {
        return blockId == null ? new Label(null, null) : labels.getOrDefault(blockId, new Label(null, null));
    }

    private BatchResponse toResponse(Batch entity, Label block) {
        return BatchResponse.builder()
                .batchId(entity.getBatchId())
                .harvestDate(entity.getHarvestDate())
                .mangoVariety(MangoVarietyCodes.toApi(entity.getVarietyKey()))
                .harvestQuantityKg(entity.getHarvestQuantityKg())
                .currentStatus(entity.getCurrentStatus())
                .farmId(block.farm())
                .blockId(block.block())
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
                .build();
    }

    // ===== RAW INTAKE MAPPERS =====

    public RawIntake toEntity(RawIntakeCreateRequest request, Batch batch) {
        RawIntake intake = new RawIntake();
        intake.setIntakeId(request.getIntakeId());
        intake.setBatch(batch);
        intake.setSourceBlockId(blocks.resolve(request.getSourceFarm(), request.getSourceBlock()));
        intake.setIntakeDate(request.getIntakeDate());
        intake.setReceivedQuantityKg(request.getReceivedQuantityKg());
        intake.setReceivedVarietyKey(mangoVarietyCodes.toKey(request.getReceivedVariety()));
        intake.setReceivedGrade(request.getReceivedGrade());
        intake.setIntakeOperatorId(personId(request.getIntakeOperator()));
        return intake;
    }

    public RawIntakeResponse toResponse(RawIntake entity) {
        return toResponse(entity, blocks.label(entity.getSourceBlockId()));
    }

    /** The responses of several raw intakes, with one lookup of their blocks. */
    public List<RawIntakeResponse> toRawIntakeResponses(List<RawIntake> entities) {
        Map<Long, Label> labels = blocks.labelsOf(entities, RawIntake::getSourceBlockId);
        return entities.stream().map(entity -> toResponse(entity, labelOf(labels, entity.getSourceBlockId()))).toList();
    }

    private RawIntakeResponse toResponse(RawIntake entity, Label block) {
        return RawIntakeResponse.builder()
                .intakeId(entity.getIntakeId())
                .batchId(entity.getBatch() != null ? entity.getBatch().getBatchId() : null)
                .sourceFarm(block.farm())
                .sourceBlock(block.block())
                .intakeDate(entity.getIntakeDate())
                .receivedQuantityKg(entity.getReceivedQuantityKg())
                .receivedVariety(MangoVarietyCodes.toApi(entity.getReceivedVarietyKey()))
                .receivedGrade(entity.getReceivedGrade())
                .intakeOperator(personDirectory.nameOf(entity.getIntakeOperatorId()))
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
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
        record.setEquipmentId(equipment != null ? equipment.getId() : null);
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
                .equipmentId(equipmentCode(entity.getEquipmentId()))
                .operatorId(entity.getOperator() != null ? entity.getOperator().getOperatorId() : null)
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
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
        run.setEquipmentId(equipment != null ? equipment.getId() : null);
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
                .equipmentId(equipmentCode(entity.getEquipmentId()))
                .operatorId(entity.getOperator() != null ? entity.getOperator().getOperatorId() : null)
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
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
        record.setEquipmentId(equipment != null ? equipment.getId() : null);
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
                .equipmentId(equipmentCode(entity.getEquipmentId()))
                .operatorId(entity.getOperator() != null ? entity.getOperator().getOperatorId() : null)
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
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
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
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
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
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
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
                .build();
    }

    // ===== HARVEST EVENT MAPPERS =====

    public HarvestEvent toEntity(HarvestEventCreateRequest request) {
        HarvestEvent event = new HarvestEvent();
        event.setHarvestId(request.getHarvestId());
        event.setBatchId(request.getBatchId());
        // Date, variety, block and quantity are copied from the plants harvest by the service.
        event.setPlantsHarvestId(request.getPlantsHarvestId());
        event.setHarvestTime(request.getHarvestTime());
        event.setQualityGrade(request.getQualityGrade());
        event.setQualityGradeDescription(request.getQualityGradeDescription());
        event.setHarvestTeamId(request.getHarvestTeamId());
        event.setHarvestSupervisorId(personId(request.getHarvestSupervisor()));
        event.setWeatherConditions(request.getWeatherConditions());
        event.setStorageLocation(request.getStorageLocation());
        return event;
    }

    public HarvestEventResponse toResponse(HarvestEvent entity) {
        return toResponse(entity, blocks.label(entity.getFarmBlockId()));
    }

    /** The responses of several harvest events, with one lookup of their blocks. */
    public List<HarvestEventResponse> toHarvestEventResponses(List<HarvestEvent> entities) {
        Map<Long, Label> labels = blocks.labelsOf(entities, HarvestEvent::getFarmBlockId);
        return entities.stream().map(entity -> toResponse(entity, labelOf(labels, entity.getFarmBlockId()))).toList();
    }

    private HarvestEventResponse toResponse(HarvestEvent entity, Label block) {
        return HarvestEventResponse.builder()
                .harvestId(entity.getHarvestId())
                .batchId(entity.getBatchId())
                .harvestDate(entity.getHarvestDate())
                .harvestTime(entity.getHarvestTime())
                .mangoVariety(MangoVarietyCodes.toApi(entity.getVarietyKey()))
                .farmId(block.farm())
                .blockId(block.block())
                .harvestQuantityKg(entity.getHarvestQuantityKg())
                .qualityGrade(entity.getQualityGrade())
                .qualityGradeDescription(entity.getQualityGradeDescription())
                .harvestTeamId(entity.getHarvestTeamId())
                .harvestSupervisor(personDirectory.nameOf(entity.getHarvestSupervisorId()))
                .weatherConditions(entity.getWeatherConditions())
                .storageLocation(entity.getStorageLocation())
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
                .build();
    }

    // ===== HISTORICAL HARVEST MAPPERS =====

    public HistoricalHarvest toEntity(HistoricalHarvestCreateRequest request) {
        HistoricalHarvest historical = new HistoricalHarvest();
        historical.setYear(request.getYear());
        historical.setMonth(request.getMonth());
        historical.setWeek(request.getWeek());
        historical.setVarietyKey(mangoVarietyCodes.toKey(request.getMangoVariety()));
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
                .mangoVariety(MangoVarietyCodes.toApi(entity.getVarietyKey()))
                .harvestQuantityKg(entity.getHarvestQuantityKg())
                .qualityGradeAPct(entity.getQualityGradeAPct())
                .qualityGradeBPct(entity.getQualityGradeBPct())
                .qualityGradeCPct(entity.getQualityGradeCPct())
                .weatherCondition(entity.getWeatherCondition())
                .rainfallMm(entity.getRainfallMm())
                .temperatureAvgC(entity.getTemperatureAvgC())
                .createdAt(timestamp(entity.getCreatedAt()))
                .updatedAt(timestamp(entity.getUpdatedAt()))
                .build();
    }
}
