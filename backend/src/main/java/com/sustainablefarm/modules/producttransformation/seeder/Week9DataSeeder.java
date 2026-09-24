package com.sustainablefarm.modules.producttransformation.seeder;

import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.batch.repository.BatchRepository;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.model.ComplianceRecord;
import com.sustainablefarm.modules.producttransformation.resources.compliancerecord.repository.ComplianceRecordRepository;
import com.sustainablefarm.modules.producttransformation.resources.dryingrun.model.DryingRun;
import com.sustainablefarm.modules.producttransformation.resources.dryingrun.repository.DryingRunRepository;
import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment;
import com.sustainablefarm.modules.producttransformation.resources.equipment.repository.EquipmentRepository;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.repository.HarvestEventRepository;
import com.sustainablefarm.modules.producttransformation.resources.historicalharvest.model.HistoricalHarvest;
import com.sustainablefarm.modules.producttransformation.resources.historicalharvest.repository.HistoricalHarvestRepository;
import com.sustainablefarm.modules.producttransformation.resources.operator.model.Operator;
import com.sustainablefarm.modules.producttransformation.resources.operator.repository.OperatorRepository;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.model.PackagingRecord;
import com.sustainablefarm.modules.producttransformation.resources.packagingrecord.repository.PackagingRecordRepository;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.repository.QcCheckpointRepository;
import com.sustainablefarm.modules.producttransformation.resources.rawintake.model.RawIntake;
import com.sustainablefarm.modules.producttransformation.resources.rawintake.repository.RawIntakeRepository;
import com.sustainablefarm.modules.producttransformation.resources.washsortrecord.model.WashSortRecord;
import com.sustainablefarm.modules.producttransformation.resources.washsortrecord.repository.WashSortRecordRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class Week9DataSeeder {

    @Autowired private OperatorRepository operatorRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private HarvestEventRepository harvestEventRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private RawIntakeRepository rawIntakeRepository;
    @Autowired private QcCheckpointRepository qcCheckpointRepository;
    @Autowired private ComplianceRecordRepository complianceRecordRepository;
    @Autowired private WashSortRecordRepository washSortRecordRepository;
    @Autowired private DryingRunRepository dryingRunRepository;
    @Autowired private PackagingRecordRepository packagingRecordRepository;
    @Autowired private HistoricalHarvestRepository historicalHarvestRepository;

    private int operatorsCreated = 0;
    private int equipmentCreated = 0;
    private int harvestEventsCreated = 0;
    private int batchesCreated = 0;
    private int rawIntakesCreated = 0;
    private int qcCheckpointsCreated = 0;
    private int complianceRecordsCreated = 0;
    private int washSortRecordsCreated = 0;
    private int dryingRunsCreated = 0;
    private int packagingRecordsCreated = 0;
    private int historicalHarvestsCreated = 0;

    @PostConstruct
    public void seed() {
        log.info("=== Week 9 Data Seeder Starting ===");
        seedOperators();
        seedEquipment();
        seedHarvestEvents();
        seedBatches();
        seedRawIntakes();
        seedQcCheckpoints();
        seedComplianceRecords();
        seedWashSortRecords();
        seedDryingRuns();
        seedPackagingRecords();
        seedHistoricalHarvests();
        logSummary();
    }

    private void seedOperators() {
        if (operatorRepository.count() > 0) return;

        List<Operator> operators = new ArrayList<>();
        operators.add(createOperator("OP-001", "Alice Mango", Operator.Role.SUPERVISOR, "Supervisor Cert", LocalDate.of(2020, 5, 15)));
        operators.add(createOperator("OP-002", "Bob Washer", Operator.Role.WASHER, "Washer Cert", LocalDate.of(2021, 3, 10)));
        operators.add(createOperator("OP-003", "Carol Dryer", Operator.Role.DRYER, "Dryer Cert", LocalDate.of(2021, 6, 20)));
        operators.add(createOperator("OP-004", "Dan Packer", Operator.Role.PACKAGER, "Packager Cert", LocalDate.of(2022, 1, 8)));
        operators.add(createOperator("OP-005", "Eve Inspector", Operator.Role.QC_INSPECTOR, "QC Inspector Cert", LocalDate.of(2019, 9, 12)));
        operators.add(createOperator("OP-006", "Frank Auditor", Operator.Role.AUDITOR, "Auditor Cert", LocalDate.of(2018, 11, 25)));
        operators.add(createOperator("OP-007", "Gina Helper", Operator.Role.WASHER, "Washer Cert", LocalDate.of(2023, 2, 14)));

        operatorRepository.saveAll(operators);
        operatorsCreated = operators.size();
        log.info("Created {} operators", operatorsCreated);
    }

    private void seedEquipment() {
        if (equipmentRepository.count() > 0) return;

        List<Equipment> equipmentList = new ArrayList<>();
        equipmentList.add(createEquipment("EQ-001", "Washing Machine 1", Equipment.EquipmentType.WASHING, new BigDecimal("500.00"), new BigDecimal("0.5000"), "Line A", Equipment.MaintenanceStatus.ACTIVE, LocalDate.of(2025, 1, 15)));
        equipmentList.add(createEquipment("EQ-002", "Solar Dryer 1", Equipment.EquipmentType.SOLAR_DRYER, new BigDecimal("300.00"), new BigDecimal("0.3000"), "Solar Field", Equipment.MaintenanceStatus.ACTIVE, LocalDate.of(2025, 3, 20)));
        equipmentList.add(createEquipment("EQ-003", "Packaging Line A", Equipment.EquipmentType.PACKAGING, new BigDecimal("400.00"), new BigDecimal("0.4000"), "Packaging Hub", Equipment.MaintenanceStatus.ACTIVE, LocalDate.of(2025, 2, 10)));
        equipmentList.add(createEquipment("EQ-004", "Tunnel Dryer 1", Equipment.EquipmentType.TUNNEL_DRYER, new BigDecimal("600.00"), new BigDecimal("0.6000"), "Drying Bay", Equipment.MaintenanceStatus.ACTIVE, LocalDate.of(2024, 11, 5)));
        equipmentList.add(createEquipment("EQ-005", "Testa Dryer 1", Equipment.EquipmentType.TESTA_DRYER, new BigDecimal("250.00"), new BigDecimal("0.2500"), "Testa Lab", Equipment.MaintenanceStatus.MAINTENANCE, LocalDate.of(2025, 6, 1)));

        equipmentRepository.saveAll(equipmentList);
        equipmentCreated = equipmentList.size();
        log.info("Created {} equipment records", equipmentCreated);
    }

    private void seedHarvestEvents() {
        if (harvestEventRepository.count() > 0) return;

        List<HarvestEvent> events = new ArrayList<>();
        events.add(createHarvestEvent("HE-001", "BATCH-001", LocalDate.of(2026, 8, 1), LocalTime.of(8, 0), HarvestEvent.MangoVariety.KEITT, "FARM-001", "BLOCK-001", new BigDecimal("500.00"), HarvestEvent.QualityGrade.A, "Team A", "Supervisor 1", "Sunny", "Cold Storage A"));
        events.add(createHarvestEvent("HE-002", "BATCH-002", LocalDate.of(2026, 8, 2), LocalTime.of(9, 30), HarvestEvent.MangoVariety.KENT, "FARM-001", "BLOCK-002", new BigDecimal("600.00"), HarvestEvent.QualityGrade.B, "Team B", "Supervisor 2", "Partly Cloudy", "Cold Storage B"));
        events.add(createHarvestEvent("HE-003", "BATCH-003", LocalDate.of(2026, 8, 3), LocalTime.of(7, 45), HarvestEvent.MangoVariety.TOMMY, "FARM-002", "BLOCK-001", new BigDecimal("450.00"), HarvestEvent.QualityGrade.A, "Team C", "Supervisor 1", "Rainy", "Cold Storage A"));

        harvestEventRepository.saveAll(events);
        harvestEventsCreated = events.size();
        log.info("Created {} harvest events", harvestEventsCreated);
    }

    private void seedBatches() {
        if (batchRepository.count() > 0) return;

        List<Batch> batches = new ArrayList<>();
        batches.add(createBatch("B-001", LocalDate.of(2026, 8, 1), HarvestEvent.MangoVariety.KEITT, new BigDecimal("500.00"), Batch.BatchStatus.CREATED, "FARM-001", "BLOCK-001"));
        batches.add(createBatch("B-002", LocalDate.of(2026, 8, 2), HarvestEvent.MangoVariety.KENT, new BigDecimal("600.00"), Batch.BatchStatus.WASHING, "FARM-001", "BLOCK-002"));
        batches.add(createBatch("B-003", LocalDate.of(2026, 8, 3), HarvestEvent.MangoVariety.TOMMY, new BigDecimal("450.00"), Batch.BatchStatus.DRYING, "FARM-002", "BLOCK-001"));

        batchRepository.saveAll(batches);
        batchesCreated = batches.size();
        log.info("Created {} batches", batchesCreated);
    }

    private void seedRawIntakes() {
        if (rawIntakeRepository.count() > 0) return;
        if (batchRepository.count() < 3) return;

        Batch b1 = batchRepository.findById("B-001").orElse(null);
        Batch b2 = batchRepository.findById("B-002").orElse(null);
        Batch b3 = batchRepository.findById("B-003").orElse(null);
        if (b1 == null || b2 == null || b3 == null) return;

        List<RawIntake> intakes = new ArrayList<>();
        intakes.add(createRawIntake("RI-001", b1, "Source Farm A", "BLOCK-001", LocalDate.of(2026, 8, 1), new BigDecimal("500.00"), HarvestEvent.MangoVariety.KEITT, HarvestEvent.QualityGrade.A, "Operator 1"));
        intakes.add(createRawIntake("RI-002", b2, "Source Farm B", "BLOCK-002", LocalDate.of(2026, 8, 2), new BigDecimal("600.00"), HarvestEvent.MangoVariety.KENT, HarvestEvent.QualityGrade.B, "Operator 2"));
        intakes.add(createRawIntake("RI-003", b3, "Source Farm C", "BLOCK-001", LocalDate.of(2026, 8, 3), new BigDecimal("450.00"), HarvestEvent.MangoVariety.TOMMY, HarvestEvent.QualityGrade.A, "Operator 3"));

        rawIntakeRepository.saveAll(intakes);
        rawIntakesCreated = intakes.size();
        log.info("Created {} raw intakes", rawIntakesCreated);
    }

    private void seedQcCheckpoints() {
        if (qcCheckpointRepository.count() > 0) return;
        if (batchRepository.count() < 3) return;

        Operator inspector = operatorRepository.findByRole(Operator.Role.QC_INSPECTOR).stream().findFirst().orElse(null);
        if (inspector == null) return;

        List<QcCheckpoint> checkpoints = new ArrayList<>();
        for (String batchId : new String[]{"B-001", "B-002", "B-003"}) {
            Batch batch = batchRepository.findById(batchId).orElse(null);
            if (batch == null) continue;
            checkpoints.add(createQcCheckpoint("QC-" + batchId + "-INTAKE", batch, QcCheckpoint.QcStage.INTAKE, QcCheckpoint.QcResult.PASS, 0, inspector, LocalDateTime.of(2026, 8, 1, 8, 0)));
            checkpoints.add(createQcCheckpoint("QC-" + batchId + "-WASHING", batch, QcCheckpoint.QcStage.WASHING, QcCheckpoint.QcResult.PASS, 2, inspector, LocalDateTime.of(2026, 8, 1, 10, 0)));
            checkpoints.add(createQcCheckpoint("QC-" + batchId + "-COOLING", batch, QcCheckpoint.QcStage.COOLING, QcCheckpoint.QcResult.PASS, 1, inspector, LocalDateTime.of(2026, 8, 1, 12, 0)));
        }

        qcCheckpointRepository.saveAll(checkpoints);
        qcCheckpointsCreated = checkpoints.size();
        log.info("Created {} QC checkpoints", qcCheckpointsCreated);
    }

    private void seedComplianceRecords() {
        if (complianceRecordRepository.count() > 0) return;
        if (batchRepository.count() < 3) return;

        Operator auditor = operatorRepository.findByRole(Operator.Role.AUDITOR).stream().findFirst().orElse(null);
        if (auditor == null) return;

        List<ComplianceRecord> records = new ArrayList<>();
        for (String batchId : new String[]{"B-001", "B-002", "B-003"}) {
            Batch batch = batchRepository.findById(batchId).orElse(null);
            if (batch == null) continue;
            records.add(createComplianceRecord("CR-" + batchId + "-HACCP", batch, ComplianceRecord.ComplianceType.HACCP, "HACCP Compliance Check", ComplianceRecord.ComplianceResult.COMPLIANT, "Evidence HACCP", auditor, LocalDate.of(2026, 8, 1), LocalDate.of(2027, 2, 1)));
            records.add(createComplianceRecord("CR-" + batchId + "-FS", batch, ComplianceRecord.ComplianceType.FOOD_SAFETY, "Food Safety Audit", ComplianceRecord.ComplianceResult.COMPLIANT, "Evidence FS", auditor, LocalDate.of(2026, 8, 2), LocalDate.of(2027, 2, 2)));
        }

        complianceRecordRepository.saveAll(records);
        complianceRecordsCreated = records.size();
        log.info("Created {} compliance records", complianceRecordsCreated);
    }

    private void seedWashSortRecords() {
        if (washSortRecordRepository.count() > 0) return;
        if (batchRepository.count() < 3) return;

        Equipment washer = equipmentRepository.findByEquipmentType(Equipment.EquipmentType.WASHING).stream().findFirst().orElse(null);
        Operator washerOp = operatorRepository.findByRole(Operator.Role.WASHER).stream().findFirst().orElse(null);
        if (washer == null || washerOp == null) return;

        List<WashSortRecord> records = new ArrayList<>();
        for (String batchId : new String[]{"B-001", "B-002", "B-003"}) {
            Batch batch = batchRepository.findById(batchId).orElse(null);
            if (batch == null) continue;
            records.add(createWashSortRecord("WS-" + batchId, batch, new BigDecimal("500.00"), new BigDecimal("475.00"), new BigDecimal("25.00"), new BigDecimal("200.00"), LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 12, 0), washer, washerOp));
        }

        washSortRecordRepository.saveAll(records);
        washSortRecordsCreated = records.size();
        log.info("Created {} wash sort records", washSortRecordsCreated);
    }

    private void seedDryingRuns() {
        if (dryingRunRepository.count() > 0) return;
        if (batchRepository.count() < 3) return;

        Equipment dryer = equipmentRepository.findByEquipmentType(Equipment.EquipmentType.SOLAR_DRYER).stream().findFirst().orElse(null);
        if (dryer == null) return;
        Operator dryerOp = operatorRepository.findByRole(Operator.Role.DRYER).stream().findFirst().orElse(null);
        if (dryerOp == null) return;

        List<DryingRun> runs = new ArrayList<>();
        for (String batchId : new String[]{"B-001", "B-002", "B-003"}) {
            Batch batch = batchRepository.findById(batchId).orElse(null);
            if (batch == null) continue;
            runs.add(createDryingRun("DR-" + batchId, batch, new BigDecimal("8.00"), new BigDecimal("65.00"), new BigDecimal("62.00"), new BigDecimal("80.00"), new BigDecimal("15.00"), new BigDecimal("12.50"), LocalDateTime.of(2026, 8, 1, 13, 0), LocalDateTime.of(2026, 8, 1, 21, 0), dryer, dryerOp));
        }

        dryingRunRepository.saveAll(runs);
        dryingRunsCreated = runs.size();
        log.info("Created {} drying runs", dryingRunsCreated);
    }

    private void seedPackagingRecords() {
        if (packagingRecordRepository.count() > 0) return;
        if (batchRepository.count() < 3) return;

        Equipment packager = equipmentRepository.findByEquipmentType(Equipment.EquipmentType.PACKAGING).stream().findFirst().orElse(null);
        Operator packagerOp = operatorRepository.findByRole(Operator.Role.PACKAGER).stream().findFirst().orElse(null);
        if (packager == null || packagerOp == null) return;

        List<PackagingRecord> records = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String batchId = "B-00" + (i + 1);
            Batch batch = batchRepository.findById(batchId).orElse(null);
            if (batch == null) continue;
            records.add(createPackagingRecord("PK-" + batchId, batch, PackagingRecord.PackageType.ONE_KG_BAG, new BigDecimal("100.00"), "LOT-" + batchId + "-2026", true, LocalDate.of(2026, 8, 5), packager, packagerOp));
        }

        packagingRecordRepository.saveAll(records);
        packagingRecordsCreated = records.size();
        log.info("Created {} packaging records", packagingRecordsCreated);
    }

    private void seedHistoricalHarvests() {
        if (historicalHarvestRepository.count() > 0) return;

        List<HistoricalHarvest> historicals = new ArrayList<>();
        String[] varieties = {"KEITT", "KENT", "TOMMY", "AMÉLIE"};
        for (String variety : varieties) {
            for (int month = 6; month <= 9; month++) {
                historicals.add(createHistoricalHarvest(2024, month, (month - 6) * 4 + 1, HarvestEvent.MangoVariety.valueOf(variety), new BigDecimal((500 + (int)(Math.random() * 500)) + ".00"), new BigDecimal("35.00"), new BigDecimal("30.00"), new BigDecimal("25.00"), "Sunny", new BigDecimal("50.00"), new BigDecimal("28.50")));
            }
        }

        historicalHarvestRepository.saveAll(historicals);
        historicalHarvestsCreated = historicals.size();
        log.info("Created {} historical harvest records", historicalHarvestsCreated);
    }

    private void logSummary() {
        log.info("=== Week 9 Data Seeder Summary ===");
        log.info("Operators: {}, Equipment: {}, Harvest Events: {}, Batches: {}, Raw Intakes: {}, QC Checkpoints: {}, Compliance Records: {}, Wash Sort: {}, Drying Runs: {}, Packaging: {}, Historical Harvests: {}",
            operatorsCreated, equipmentCreated, harvestEventsCreated, batchesCreated, rawIntakesCreated, qcCheckpointsCreated, complianceRecordsCreated, washSortRecordsCreated, dryingRunsCreated, packagingRecordsCreated, historicalHarvestsCreated);
        log.info("Total records seeded: {}", operatorsCreated + equipmentCreated + harvestEventsCreated + batchesCreated + rawIntakesCreated + qcCheckpointsCreated + complianceRecordsCreated + washSortRecordsCreated + dryingRunsCreated + packagingRecordsCreated + historicalHarvestsCreated);
        log.info("=== Week 9 Data Seeder Complete ===");
    }

    // ===== Helper Methods =====

    private Operator createOperator(String id, String name, Operator.Role role, String certs, LocalDate hireDate) {
        Operator op = new Operator();
        op.setOperatorId(id);
        op.setOperatorName(name);
        op.setRole(role);
        op.setCertifications(certs);
        op.setActiveStatus(Operator.ActiveStatus.ACTIVE);
        op.setHireDate(hireDate);
        return op;
    }

    private Equipment createEquipment(String id, String name, Equipment.EquipmentType type, BigDecimal capacity, BigDecimal energy, String location, Equipment.MaintenanceStatus status, LocalDate lastMaint) {
        Equipment eq = new Equipment();
        eq.setEquipmentId(id);
        eq.setEquipmentName(name);
        eq.setEquipmentType(type);
        eq.setCapacityKgPerHour(capacity);
        eq.setEnergyConsumptionKwhPerKg(energy);
        eq.setLocation(location);
        eq.setMaintenanceStatus(status);
        eq.setLastMaintenanceDate(lastMaint);
        return eq;
    }

    private HarvestEvent createHarvestEvent(String id, String batchId, LocalDate date, LocalTime time, HarvestEvent.MangoVariety variety, String farmId, String blockId, BigDecimal quantity, HarvestEvent.QualityGrade grade, String team, String supervisor, String weather, String storage) {
        HarvestEvent event = new HarvestEvent();
        event.setHarvestId(id);
        event.setBatchId(batchId);
        event.setHarvestDate(date);
        event.setHarvestTime(time);
        event.setMangoVariety(variety);
        event.setFarmId(farmId);
        event.setBlockId(blockId);
        event.setHarvestQuantityKg(quantity);
        event.setQualityGrade(grade);
        event.setHarvestTeamId(team);
        event.setHarvestSupervisor(supervisor);
        event.setWeatherConditions(weather);
        event.setStorageLocation(storage);
        return event;
    }

    private Batch createBatch(String id, LocalDate harvestDate, HarvestEvent.MangoVariety variety, BigDecimal quantity, Batch.BatchStatus status, String farmId, String blockId) {
        Batch batch = new Batch();
        batch.setBatchId(id);
        batch.setHarvestDate(harvestDate);
        batch.setMangoVariety(variety);
        batch.setHarvestQuantityKg(quantity);
        batch.setCurrentStatus(status);
        batch.setFarmId(farmId);
        batch.setBlockId(blockId);
        return batch;
    }

    private RawIntake createRawIntake(String id, Batch batch, String farm, String block, LocalDate date, BigDecimal quantity, HarvestEvent.MangoVariety variety, HarvestEvent.QualityGrade grade, String operator) {
        RawIntake intake = new RawIntake();
        intake.setIntakeId(id);
        intake.setBatch(batch);
        intake.setSourceFarm(farm);
        intake.setSourceBlock(block);
        intake.setIntakeDate(date);
        intake.setReceivedQuantityKg(quantity);
        intake.setReceivedVariety(variety);
        intake.setReceivedGrade(grade);
        intake.setIntakeOperator(operator);
        return intake;
    }

    private QcCheckpoint createQcCheckpoint(String id, Batch batch, QcCheckpoint.QcStage stage, QcCheckpoint.QcResult result, int defects, Operator inspector, LocalDateTime time) {
        QcCheckpoint checkpoint = new QcCheckpoint();
        checkpoint.setCheckpointId(id);
        checkpoint.setBatch(batch);
        checkpoint.setStage(stage);
        checkpoint.setResult(result);
        checkpoint.setDefects("Defects found during " + stage.name() + " inspection");
        checkpoint.setDefectsCount(defects);
        checkpoint.setInspector(inspector);
        checkpoint.setCheckpointTime(time);
        checkpoint.setNotes("Mandatory checkpoint completed");
        return checkpoint;
    }

    private ComplianceRecord createComplianceRecord(String id, Batch batch, ComplianceRecord.ComplianceType type, String requirement, ComplianceRecord.ComplianceResult result, String evidence, Operator auditor, LocalDate auditDate, LocalDate nextAudit) {
        ComplianceRecord record = new ComplianceRecord();
        record.setRecordId(id);
        record.setBatch(batch);
        record.setComplianceType(type);
        record.setRequirement(requirement);
        record.setResult(result);
        record.setEvidence(evidence);
        record.setAuditor(auditor);
        record.setAuditDate(auditDate);
        record.setNextAuditDate(nextAudit);
        return record;
    }

    private WashSortRecord createWashSortRecord(String id, Batch batch, BigDecimal input, BigDecimal output, BigDecimal waste, BigDecimal water, LocalDateTime start, LocalDateTime end, Equipment equipment, Operator operator) {
        WashSortRecord record = new WashSortRecord();
        record.setRecordId(id);
        record.setBatch(batch);
        record.setInputQuantityKg(input);
        record.setOutputQuantityKg(output);
        record.setWasteQuantityKg(waste);
        record.setWaterUsageLiters(water);
        record.setStartTime(start);
        record.setEndTime(end);
        record.setEquipment(equipment);
        record.setOperator(operator);
        return record;
    }

    private DryingRun createDryingRun(String id, Batch batch, BigDecimal duration, BigDecimal targetTemp, BigDecimal actualTemp, BigDecimal startMoisture, BigDecimal endMoisture, BigDecimal energy, LocalDateTime start, LocalDateTime end, Equipment equipment, Operator operator) {
        DryingRun run = new DryingRun();
        run.setRunId(id);
        run.setBatch(batch);
        run.setDurationHours(duration);
        run.setTargetTemperatureC(targetTemp);
        run.setActualTemperatureC(actualTemp);
        run.setStartMoisturePct(startMoisture);
        run.setEndMoisturePct(endMoisture);
        run.setEnergyUsageKwh(energy);
        run.setStartTime(start);
        run.setEndTime(end);
        run.setEquipment(equipment);
        run.setOperator(operator);
        return run;
    }

    private PackagingRecord createPackagingRecord(String id, Batch batch, PackagingRecord.PackageType type, BigDecimal quantity, String lotCode, boolean exportReady, LocalDate packDate, Equipment equipment, Operator operator) {
        PackagingRecord record = new PackagingRecord();
        record.setRecordId(id);
        record.setBatch(batch);
        record.setPackageType(type);
        record.setPackageQuantityKg(quantity);
        record.setLotCode(lotCode);
        record.setExportReady(exportReady);
        record.setPackagingDate(packDate);
        record.setEquipment(equipment);
        record.setOperator(operator);
        return record;
    }

    private HistoricalHarvest createHistoricalHarvest(int year, int month, int week, HarvestEvent.MangoVariety variety, BigDecimal quantity, BigDecimal aPct, BigDecimal bPct, BigDecimal cPct, String weather, BigDecimal rainfall, BigDecimal temp) {
        HistoricalHarvest h = new HistoricalHarvest();
        h.setYear(year);
        h.setMonth(month);
        h.setWeek(week);
        h.setMangoVariety(variety);
        h.setHarvestQuantityKg(quantity);
        h.setQualityGradeAPct(aPct);
        h.setQualityGradeBPct(bPct);
        h.setQualityGradeCPct(cPct);
        h.setWeatherCondition(weather);
        h.setRainfallMm(rainfall);
        h.setTemperatureAvgC(temp);
        return h;
    }
}
