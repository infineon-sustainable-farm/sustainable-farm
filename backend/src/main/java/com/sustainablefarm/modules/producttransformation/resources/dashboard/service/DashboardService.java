package com.sustainablefarm.modules.producttransformation.resources.dashboard.service;

import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.DashboardKPIResponse;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.HarvestQuantityKPI;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.ActiveBatchesKPI;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.QualityPassRateKPI;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.EnergyConsumptionKPI;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.EquipmentUtilizationKPI;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.HarvestTrendData;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.ProductionOutputData;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.EnergyBreakdownData;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.EquipmentUtilizationChartData;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.QualityTrendData;
import com.sustainablefarm.modules.producttransformation.resources.batch.model.Batch;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.model.HarvestEvent;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.model.QcCheckpoint;
import com.sustainablefarm.modules.producttransformation.resources.dryingrun.model.DryingRun;
import com.sustainablefarm.modules.producttransformation.resources.washsortrecord.model.WashSortRecord;
import com.sustainablefarm.modules.producttransformation.resources.equipment.model.Equipment;
import com.sustainablefarm.modules.producttransformation.resources.equipment.repository.EquipmentRepository;
import com.sustainablefarm.modules.producttransformation.resources.batch.repository.BatchRepository;
import com.sustainablefarm.modules.producttransformation.resources.dryingrun.repository.DryingRunRepository;
import com.sustainablefarm.modules.producttransformation.resources.harvestevent.repository.HarvestEventRepository;
import com.sustainablefarm.modules.producttransformation.resources.qccheckpoint.repository.QcCheckpointRepository;
import com.sustainablefarm.modules.producttransformation.resources.washsortrecord.repository.WashSortRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard Service
 * Calculates and aggregates KPIs for the Product Transformation dashboard
 * Based on KPI data lineage documented in WEEK_6_KPI_DATA_LINEAGE.md
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final HarvestEventRepository harvestEventRepository;
    private final BatchRepository batchRepository;
    private final QcCheckpointRepository qcCheckpointRepository;
    private final DryingRunRepository dryingRunRepository;
    private final EquipmentRepository equipmentRepository;
    private final WashSortRecordRepository washSortRecordRepository;
    
    /**
     * Get comprehensive dashboard KPIs
     * Aggregates all dashboard KPIs into a single response
     */
    public DashboardKPIResponse getDashboardKPIs(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating dashboard KPIs for period: {} to {}", startDate, endDate);
        
        DashboardKPIResponse response = DashboardKPIResponse.builder()
            .startDate(startDate)
            .endDate(endDate)
            .period(determinePeriod(startDate, endDate))
            .generatedAt(LocalDateTime.now().toString())
            .build();
        
        // Operational KPIs
        response.setHarvestQuantity(calculateHarvestQuantity(startDate, endDate).getTotalQuantity());
        response.setActiveBatches(calculateActiveBatches().getActiveBatchCount());
        response.setProductionOutput(calculateProductionOutput(startDate, endDate));
        response.setQualityPassRate(calculateQualityPassRate(startDate, endDate).getPassRate());
        
        // Resource KPIs
        response.setWaterConsumption(calculateWaterConsumption(startDate, endDate));
        response.setEnergyConsumption(calculateEnergyConsumption(startDate, endDate).getAverageConsumption());
        response.setSolarEnergyShare(calculateEnergyConsumption(startDate, endDate).getSolarShare());
        response.setEquipmentUtilization(calculateEquipmentUtilization().getUtilizationRate());
        
        // Quality KPIs
        response.setGradeAPercentage(calculateGradeAPercentage(startDate, endDate));
        response.setQualityTargetStatus(calculateQualityTargetStatus(response.getGradeAPercentage()));
        
        // Production Analytics KPIs
        response.setTotalEnergyToday(calculateTotalEnergyToday());
        response.setProductionEfficiency(calculateProductionEfficiency(startDate, endDate));
        
        log.info("Dashboard KPIs calculated successfully");
        return response;
    }
    
    /**
     * Calculate Harvest Quantity KPI
     * Data Source: harvest_event table
     * Calculation: SUM(quantity) for harvest events in period
     */
    public HarvestQuantityKPI calculateHarvestQuantity(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating harvest quantity for period: {} to {}", startDate, endDate);
        
        List<HarvestEvent> harvestEvents = harvestEventRepository.findByHarvestDateBetween(startDate, endDate);
        
        BigDecimal totalQuantity = harvestEvents.stream()
            .map(HarvestEvent::getHarvestQuantityKg)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate change vs previous period (simplified)
        BigDecimal changePercentage = calculateChangePercentage(
            totalQuantity, 
            calculatePreviousPeriodQuantity(startDate, endDate)
        );
        
        String trend = determineTrend(changePercentage);
        
        return HarvestQuantityKPI.builder()
            .totalQuantity(totalQuantity)
            .changePercentage(changePercentage)
            .trend(trend)
            .startDate(startDate)
            .endDate(endDate)
            .harvestEventCount(harvestEvents.size())
            .build();
    }
    
    /**
     * Calculate Active Batches KPI
     * Data Source: batch table
     * Calculation: COUNT(*) WHERE status = 'active' or 'in_progress'
     */
    public ActiveBatchesKPI calculateActiveBatches() {
        log.debug("Calculating active batches");
        
        List<Batch> allBatches = batchRepository.findAll();
        List<Batch> activeBatches = allBatches.stream()
            .filter(batch -> batch.getCurrentStatus() == Batch.BatchStatus.DRYING || 
                           batch.getCurrentStatus() == Batch.BatchStatus.WASHING ||
                           batch.getCurrentStatus() == Batch.BatchStatus.PACKAGING)
            .collect(Collectors.toList());
        
        BigDecimal activePercentage = allBatches.isEmpty() 
            ? BigDecimal.ZERO 
            : new BigDecimal(activeBatches.size())
                .divide(new BigDecimal(allBatches.size()), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        String status = determineActiveBatchStatus(activeBatches.size());
        
        List<ActiveBatchesKPI.BatchSummary> batchSummaries = activeBatches.stream()
            .limit(10) // Limit to first 10 for performance
            .map(this::mapToBatchSummary)
            .collect(Collectors.toList());
        
        return ActiveBatchesKPI.builder()
            .activeBatchCount(activeBatches.size())
            .totalBatchCount(allBatches.size())
            .activePercentage(activePercentage)
            .status(status)
            .asOfDate(LocalDate.now())
            .batchSummaries(batchSummaries)
            .build();
    }
    
    /**
     * Calculate Quality Pass Rate KPI
     * Data Source: qc_checkpoint table
     * Calculation: (COUNT WHERE result = 'pass') / COUNT(*) * 100
     */
    public QualityPassRateKPI calculateQualityPassRate(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating quality pass rate for period: {} to {}", startDate, endDate);
        
        List<QcCheckpoint> checkpoints = qcCheckpointRepository.findByCheckpointDateBetween(startDate, endDate);
        
        int totalCheckpoints = checkpoints.size();
        int passedCheckpoints = (int) checkpoints.stream()
            .filter(cp -> cp.getResult() == QcCheckpoint.QcResult.PASS)
            .count();
        int failedCheckpoints = (int) checkpoints.stream()
            .filter(cp -> cp.getResult() == QcCheckpoint.QcResult.FAIL)
            .count();
        int reworkCheckpoints = (int) checkpoints.stream()
            .filter(cp -> cp.getResult() == QcCheckpoint.QcResult.REWORK)
            .count();
        
        BigDecimal passRate = totalCheckpoints == 0 
            ? BigDecimal.ZERO 
            : new BigDecimal(passedCheckpoints)
                .divide(new BigDecimal(totalCheckpoints), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        BigDecimal targetPercentage = new BigDecimal("90"); // 90% target
        String targetStatus = determineTargetStatus(passRate, targetPercentage);
        
        return QualityPassRateKPI.builder()
            .passRate(passRate)
            .totalCheckpoints(totalCheckpoints)
            .passedCheckpoints(passedCheckpoints)
            .failedCheckpoints(failedCheckpoints)
            .reworkCheckpoints(reworkCheckpoints)
            .targetStatus(targetStatus)
            .targetPercentage(targetPercentage)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    }
    
    /**
     * Calculate Energy Consumption KPI
     * Data Source: drying_run table
     * Calculation: SUM(energy_consumption) / SUM(dried_quantity) = kWh/kg
     */
    public EnergyConsumptionKPI calculateEnergyConsumption(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating energy consumption for period: {} to {}", startDate, endDate);
        
        List<DryingRun> dryingRuns = dryingRunRepository.findByStartTimeBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        
        BigDecimal totalEnergy = dryingRuns.stream()
            .map(DryingRun::getEnergyUsageKwh)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Dried quantity needs to be calculated from batch quantity or similar
        // For now, we'll use duration * target as a proxy
        BigDecimal totalDriedQuantity = dryingRuns.stream()
            .map(run -> run.getDurationHours().multiply(run.getTargetTemperatureC()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageConsumption = totalDriedQuantity.compareTo(BigDecimal.ZERO) == 0 
            ? BigDecimal.ZERO 
            : totalEnergy.divide(totalDriedQuantity, 2, RoundingMode.HALF_UP);
        
        // Calculate solar energy share (simplified - assume 0 for now as dryer_type not fully implemented)
        BigDecimal solarConsumption = BigDecimal.ZERO;
        BigDecimal gridConsumption = totalEnergy;
        BigDecimal solarShare = BigDecimal.ZERO;
        
        BigDecimal targetConsumption = new BigDecimal("1.5"); // 1.5 kWh/kg target
        String efficiencyStatus = determineEfficiencyStatus(averageConsumption, targetConsumption);
        
        return EnergyConsumptionKPI.builder()
            .averageConsumption(averageConsumption)
            .totalConsumption(totalEnergy)
            .solarConsumption(solarConsumption)
            .gridConsumption(gridConsumption)
            .solarShare(solarShare)
            .efficiencyStatus(efficiencyStatus)
            .targetConsumption(targetConsumption)
            .startDate(startDate)
            .endDate(endDate)
            .dryingRunCount(dryingRuns.size())
            .build();
    }
    
    /**
     * Calculate Equipment Utilization KPI
     * Data Source: equipment table
     * Calculation: (COUNT WHERE status = 'active') / COUNT(*) * 100
     */
    public EquipmentUtilizationKPI calculateEquipmentUtilization() {
        log.debug("Calculating equipment utilization");
        
        List<Equipment> allEquipment = equipmentRepository.findAll();
        int totalEquipment = allEquipment.size();
        
        int activeEquipment = (int) allEquipment.stream()
            .filter(eq -> eq.getMaintenanceStatus() == Equipment.MaintenanceStatus.ACTIVE)
            .count();
        
        int maintenanceEquipment = (int) allEquipment.stream()
            .filter(eq -> eq.getMaintenanceStatus() == Equipment.MaintenanceStatus.MAINTENANCE)
            .count();
        
        int inactiveEquipment = (int) allEquipment.stream()
            .filter(eq -> eq.getMaintenanceStatus() == Equipment.MaintenanceStatus.RETIRED)
            .count();
        
        BigDecimal utilizationRate = totalEquipment == 0 
            ? BigDecimal.ZERO 
            : new BigDecimal(activeEquipment)
                .divide(new BigDecimal(totalEquipment), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        
        String overallStatus = determineEquipmentStatus(utilizationRate);
        
        return EquipmentUtilizationKPI.builder()
            .utilizationRate(utilizationRate)
            .totalEquipment(totalEquipment)
            .activeEquipment(activeEquipment)
            .maintenanceEquipment(maintenanceEquipment)
            .inactiveEquipment(inactiveEquipment)
            .overallStatus(overallStatus)
            .asOfDate(LocalDate.now())
            .build();
    }
    
    /**
     * Get Harvest Trend Chart Data
     * Data Source: harvest_event table
     * Calculation: SUM(quantity) grouped by harvest date
     */
    public List<HarvestTrendData> getHarvestTrendChartData(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating harvest trend chart data for period: {} to {}", startDate, endDate);
        
        List<HarvestEvent> harvestEvents = harvestEventRepository.findByHarvestDateBetween(startDate, endDate);
        
        List<HarvestTrendData> trendData = new java.util.ArrayList<>();
        java.util.Map<LocalDate, BigDecimal> dateToQuantity = new java.util.HashMap<>();
        for (HarvestEvent event : harvestEvents) {
            LocalDate date = event.getHarvestDate();
            BigDecimal quantity = event.getHarvestQuantityKg();
            if (quantity == null) quantity = BigDecimal.ZERO;
            dateToQuantity.merge(date, quantity, BigDecimal::add);
        }
        
        for (java.util.Map.Entry<LocalDate, BigDecimal> entry : dateToQuantity.entrySet()) {
            trendData.add(HarvestTrendData.builder()
                .date(entry.getKey())
                .quantity(entry.getValue())
                .build());
        }
        
        trendData.sort(java.util.Comparator.comparing(HarvestTrendData::getDate));
        
        log.debug("Harvest trend chart data calculated successfully");
        return trendData;
    }
    
    /**
     * Get Production Output Chart Data
     * Data Source: drying_run table
     * Calculation: SUM(energy_usage_kwh) grouped by week
     */
    public List<ProductionOutputData> getProductionOutputChartData(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating production output chart data for period: {} to {}", startDate, endDate);
        
        List<DryingRun> dryingRuns = dryingRunRepository.findByStartTimeBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        
        List<ProductionOutputData> outputData = new java.util.ArrayList<>();
        java.util.Map<String, BigDecimal> periodToOutput = new java.util.HashMap<>();
        for (DryingRun run : dryingRuns) {
            LocalDateTime startTime = run.getStartTime();
            long epochDay = startTime.toLocalDate().toEpochDay();
            long weekStartDay = epochDay - (epochDay % 7);
            String period = "Week " + (weekStartDay / 7 + 1);
            BigDecimal energy = run.getEnergyUsageKwh();
            if (energy == null) energy = BigDecimal.ZERO;
            periodToOutput.merge(period, energy, BigDecimal::add);
        }
        
        for (java.util.Map.Entry<String, BigDecimal> entry : periodToOutput.entrySet()) {
            outputData.add(ProductionOutputData.builder()
                .period(entry.getKey())
                .output(entry.getValue())
                .build());
        }
        
        outputData.sort(java.util.Comparator.comparing(ProductionOutputData::getPeriod));
        
        log.debug("Production output chart data calculated successfully");
        return outputData;
    }
    
    /**
     * Get Energy Breakdown Chart Data
     * Data Source: drying_run table
     * Calculation: Energy consumption by source (grid vs solar)
     */
    public List<EnergyBreakdownData> getEnergyBreakdownChartData() {
        log.debug("Calculating energy breakdown chart data");
        
        List<DryingRun> dryingRuns = dryingRunRepository.findAll();
        BigDecimal totalEnergy = dryingRuns.stream()
            .map(DryingRun::getEnergyUsageKwh)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal gridConsumption = totalEnergy;
        BigDecimal solarConsumption = BigDecimal.ZERO;
        
        List<EnergyBreakdownData> breakdownData = new java.util.ArrayList<>();
        breakdownData.add(EnergyBreakdownData.builder()
            .name("Grid")
            .value(gridConsumption)
            .build());
        breakdownData.add(EnergyBreakdownData.builder()
            .name("Solar")
            .value(solarConsumption)
            .build());
        breakdownData.add(EnergyBreakdownData.builder()
            .name("Generator")
            .value(BigDecimal.ZERO)
            .build());
        
        log.debug("Energy breakdown chart data calculated successfully");
        return breakdownData;
    }
    
    /**
     * Get Equipment Utilization Chart Data
     * Data Source: equipment table
     * Calculation: Utilization rate for each equipment
     */
    public List<EquipmentUtilizationChartData> getEquipmentUtilizationChartData() {
        log.debug("Calculating equipment utilization chart data");
        
        List<Equipment> allEquipment = equipmentRepository.findAll();
        int totalEquipment = allEquipment.size();
        
        List<EquipmentUtilizationChartData> utilizationData = new java.util.ArrayList<>();
        for (Equipment equipment : allEquipment) {
            BigDecimal utilizationRate = totalEquipment == 0 
                ? BigDecimal.ZERO 
                : new BigDecimal(1).divide(BigDecimal.valueOf(totalEquipment), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            
            utilizationData.add(EquipmentUtilizationChartData.builder()
                .equipment(equipment.getEquipmentName() != null ? equipment.getEquipmentName() : "Unknown Equipment")
                .utilization(utilizationRate)
                .build());
        }
        
        log.debug("Equipment utilization chart data calculated successfully");
        return utilizationData;
    }
    
    /**
     * Get Quality Trend Chart Data
     * Data Source: qc_checkpoint table
     * Calculation: Pass rate grouped by checkpoint date
     */
    public List<QualityTrendData> getQualityTrendChartData(LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating quality trend chart data for period: {} to {}", startDate, endDate);
        
        List<QcCheckpoint> checkpoints = qcCheckpointRepository.findByCheckpointDateBetween(startDate, endDate);
        
        List<QualityTrendData> trendData = new java.util.ArrayList<>();
        java.util.Map<LocalDate, int[]> dateToCounts = new java.util.HashMap<>();
        for (QcCheckpoint checkpoint : checkpoints) {
            LocalDate date = checkpoint.getCheckpointTime().toLocalDate();
            int[] counts = dateToCounts.getOrDefault(date, new int[]{0, 0});
            counts[0]++;
            if (checkpoint.getResult() == QcCheckpoint.QcResult.PASS) {
                counts[1]++;
            }
            dateToCounts.put(date, counts);
        }
        
        for (java.util.Map.Entry<LocalDate, int[]> entry : dateToCounts.entrySet()) {
            int total = entry.getValue()[0];
            int passed = entry.getValue()[1];
            BigDecimal passRate = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(passed).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            trendData.add(QualityTrendData.builder()
                .date(entry.getKey())
                .passRate(passRate)
                .build());
        }
        
        trendData.sort(java.util.Comparator.comparing(QualityTrendData::getDate));
        
        log.debug("Quality trend chart data calculated successfully");
        return trendData;
    }
    
    /**
     * Calculate Production Output
     * Data Source: drying_run table
     * Calculation: SUM(energy_usage_kwh) / target (proxy for dried quantity)
     */
    private BigDecimal calculateProductionOutput(LocalDate startDate, LocalDate endDate) {
        List<DryingRun> dryingRuns = dryingRunRepository.findByStartTimeBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        
        // Using energy usage as a proxy for production output
        // In production, this would use actual dried quantity from the batch
        return dryingRuns.stream()
            .map(DryingRun::getEnergyUsageKwh)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate Water Consumption
     * Data Source: wash_sort_record table
     * Calculation: SUM(water_consumption) / SUM(input_quantity) = L/kg
     */
    private BigDecimal calculateWaterConsumption(LocalDate startDate, LocalDate endDate) {
        List<WashSortRecord> washSortRecords = washSortRecordRepository.findByStartTimeBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        
        BigDecimal totalWater = washSortRecords.stream()
            .map(WashSortRecord::getWaterUsageLiters)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalInput = washSortRecords.stream()
            .map(WashSortRecord::getInputQuantityKg)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalInput.compareTo(BigDecimal.ZERO) == 0 
            ? BigDecimal.ZERO 
            : totalWater.divide(totalInput, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate Grade A Percentage
     * Data Source: harvest_event table
     * Calculation: SUM(grade_a_quantity) / SUM(quantity) * 100
     */
    private BigDecimal calculateGradeAPercentage(LocalDate startDate, LocalDate endDate) {
        List<HarvestEvent> harvestEvents = harvestEventRepository.findByHarvestDateBetween(startDate, endDate);
        
        BigDecimal totalQuantity = harvestEvents.stream()
            .map(HarvestEvent::getHarvestQuantityKg)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Grade A quantity is not directly stored in HarvestEvent
        // For now, we'll calculate it as 75% of total (simplified)
        BigDecimal totalGradeA = totalQuantity.multiply(new BigDecimal("0.75"));
        
        return totalQuantity.compareTo(BigDecimal.ZERO) == 0 
            ? BigDecimal.ZERO 
            : totalGradeA.divide(totalQuantity, 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }
    
    /**
     * Calculate Quality Target Status
     */
    private String calculateQualityTargetStatus(BigDecimal gradeAPercentage) {
        if (gradeAPercentage == null) return "unknown";
        if (gradeAPercentage.compareTo(new BigDecimal("75")) >= 0) return "above";
        if (gradeAPercentage.compareTo(new BigDecimal("50")) >= 0) return "on";
        return "below";
    }
    
    /**
     * Calculate Total Energy Today
     * Data Source: drying_run table
     * Calculation: SUM(energy_usage_kwh) for drying runs today
     */
    private BigDecimal calculateTotalEnergyToday() {
        LocalDate today = LocalDate.now();
        List<DryingRun> dryingRuns = dryingRunRepository.findByStartTimeBetween(today.atStartOfDay(), today.atTime(23, 59, 59));
        
        return dryingRuns.stream()
            .map(DryingRun::getEnergyUsageKwh)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate Production Efficiency
     * Data Source: wash_sort_record and drying_run tables
     * Calculation: (Output Quantity / Input Quantity) * 100
     */
    private BigDecimal calculateProductionEfficiency(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalInput = washSortRecordRepository.findByStartTimeBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
            .stream()
            .map(WashSortRecord::getInputQuantityKg)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Using energy usage as proxy for output (simplified)
        BigDecimal totalOutput = dryingRunRepository.findByStartTimeBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
            .stream()
            .map(DryingRun::getEnergyUsageKwh)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalInput.compareTo(BigDecimal.ZERO) == 0 
            ? BigDecimal.ZERO 
            : totalOutput.divide(totalInput, 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }
    
    // Helper methods
    
    private BigDecimal calculatePreviousPeriodQuantity(LocalDate startDate, LocalDate endDate) {
        // Simplified: return 0 for now, would calculate previous period in production
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateChangePercentage(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(previous)
            .divide(previous, 2, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
    
    private String determineTrend(BigDecimal changePercentage) {
        if (changePercentage.compareTo(BigDecimal.ZERO) > 0) return "up";
        if (changePercentage.compareTo(BigDecimal.ZERO) < 0) return "down";
        return "stable";
    }
    
    private String determinePeriod(LocalDate startDate, LocalDate endDate) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 1) return "day";
        if (days <= 7) return "week";
        if (days <= 30) return "month";
        return "custom";
    }
    
    private String determineActiveBatchStatus(int activeBatches) {
        if (activeBatches == 0) return "idle";
        if (activeBatches <= 3) return "production";
        return "production";
    }
    
    private String determineTargetStatus(BigDecimal actual, BigDecimal target) {
        if (actual.compareTo(target) >= 0) return "above";
        if (actual.compareTo(target.multiply(new BigDecimal("0.9"))) >= 0) return "on";
        return "below";
    }
    
    private String determineEfficiencyStatus(BigDecimal actual, BigDecimal target) {
        if (actual.compareTo(target) <= 0) return "on_target";
        if (actual.compareTo(target.multiply(new BigDecimal("1.1"))) <= 0) return "above";
        return "below";
    }
    
    private String determineEquipmentStatus(BigDecimal utilizationRate) {
        if (utilizationRate.compareTo(new BigDecimal("80")) >= 0) return "operational";
        if (utilizationRate.compareTo(new BigDecimal("50")) >= 0) return "degraded";
        return "critical";
    }
    
    private ActiveBatchesKPI.BatchSummary mapToBatchSummary(Batch batch) {
        return ActiveBatchesKPI.BatchSummary.builder()
            .batchId(batch.getBatchId())
            .productName(batch.getMangoVariety() != null ? batch.getMangoVariety().name() : "unknown")
            .currentStage(batch.getCurrentStatus() != null ? batch.getCurrentStatus().name() : "unknown")
            .quantity(batch.getHarvestQuantityKg())
            .startDate(batch.getHarvestDate())
            .status(batch.getCurrentStatus() != null ? batch.getCurrentStatus().name() : "unknown")
            .build();
    }
}