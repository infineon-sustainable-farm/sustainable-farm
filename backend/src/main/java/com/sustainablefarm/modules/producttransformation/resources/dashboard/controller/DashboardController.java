package com.sustainablefarm.modules.producttransformation.resources.dashboard.controller;

import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.*;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.dto.response.DashboardKPIResponse;
import com.sustainablefarm.modules.producttransformation.resources.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard Controller
 * REST API endpoints for dashboard KPIs
 * Based on KPI data lineage documented in WEEK_6_KPI_DATA_LINEAGE.md
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard KPI endpoints for Product Transformation")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    /**
     * Get comprehensive dashboard KPIs
     * Aggregates all dashboard KPIs into a single response
     */
    @GetMapping("/kpis")
    @Operation(summary = "Get comprehensive dashboard KPIs", description = "Returns all dashboard KPIs for the specified period")
    public ResponseEntity<DashboardKPIResponse> getDashboardKPIs(
        @Parameter(description = "Start date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        
        @Parameter(description = "End date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        log.info("GET /api/dashboard/kpis - startDate: {}, endDate: {}", startDate, endDate);
        
        // Default to last 7 days if not specified
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        DashboardKPIResponse response = dashboardService.getDashboardKPIs(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Harvest Quantity KPI
     * Operational KPI - Total harvest quantity for period
     */
    @GetMapping("/kpi/harvest-quantity")
    @Operation(summary = "Get Harvest Quantity KPI", description = "Returns total harvest quantity and trend for the specified period")
    public ResponseEntity<HarvestQuantityKPI> getHarvestQuantityKPI(
        @Parameter(description = "Start date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        
        @Parameter(description = "End date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        log.info("GET /api/dashboard/kpi/harvest-quantity - startDate: {}, endDate: {}", startDate, endDate);
        
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        
        HarvestQuantityKPI response = dashboardService.calculateHarvestQuantity(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Active Batches KPI
     * Operational KPI - Current active batches in production
     */
    @GetMapping("/kpi/active-batches")
    @Operation(summary = "Get Active Batches KPI", description = "Returns current active batches and their status")
    public ResponseEntity<ActiveBatchesKPI> getActiveBatchesKPI() {
        log.info("GET /api/dashboard/kpi/active-batches");
        
        ActiveBatchesKPI response = dashboardService.calculateActiveBatches();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Quality Pass Rate KPI
     * Operational KPI - Quality pass rate for QC checkpoints
     */
    @GetMapping("/kpi/quality-pass-rate")
    @Operation(summary = "Get Quality Pass Rate KPI", description = "Returns quality pass rate and target status for the specified period")
    public ResponseEntity<QualityPassRateKPI> getQualityPassRateKPI(
        @Parameter(description = "Start date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        
        @Parameter(description = "End date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        log.info("GET /api/dashboard/kpi/quality-pass-rate - startDate: {}, endDate: {}", startDate, endDate);
        
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        
        QualityPassRateKPI response = dashboardService.calculateQualityPassRate(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Energy Consumption KPI
     * Resource KPI - Energy consumption metrics for drying operations
     */
    @GetMapping("/kpi/energy-consumption")
    @Operation(summary = "Get Energy Consumption KPI", description = "Returns energy consumption metrics and solar energy share for the specified period")
    public ResponseEntity<EnergyConsumptionKPI> getEnergyConsumptionKPI(
        @Parameter(description = "Start date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        
        @Parameter(description = "End date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        log.info("GET /api/dashboard/kpi/energy-consumption - startDate: {}, endDate: {}", startDate, endDate);
        
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        
        EnergyConsumptionKPI response = dashboardService.calculateEnergyConsumption(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Equipment Utilization KPI
     * Resource KPI - Equipment utilization metrics
     */
    @GetMapping("/kpi/equipment-utilization")
    @Operation(summary = "Get Equipment Utilization KPI", description = "Returns equipment utilization rate and status")
    public ResponseEntity<EquipmentUtilizationKPI> getEquipmentUtilizationKPI() {
        log.info("GET /api/dashboard/kpi/equipment-utilization");
        
        EquipmentUtilizationKPI response = dashboardService.calculateEquipmentUtilization();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Harvest Trend Chart Data
     * Chart Data - Harvest quantity trend over time
     */
    @GetMapping("/charts/harvest-trend")
    @Operation(summary = "Get Harvest Trend Chart Data", description = "Returns harvest quantity data for trend chart")
    public ResponseEntity<List<HarvestTrendData>> getHarvestTrendChart(
        @Parameter(description = "Start date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        
        @Parameter(description = "End date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        log.info("GET /api/dashboard/charts/harvest-trend - startDate: {}, endDate: {}", startDate, endDate);
        
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        
        List<HarvestTrendData> response = dashboardService.getHarvestTrendChartData(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Production Output Chart Data
     * Chart Data - Production output comparison over time
     */
    @GetMapping("/charts/production-output")
    @Operation(summary = "Get Production Output Chart Data", description = "Returns production output data for bar chart")
    public ResponseEntity<List<ProductionOutputData>> getProductionOutputChart(
        @Parameter(description = "Start date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        
        @Parameter(description = "End date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        log.info("GET /api/dashboard/charts/production-output - startDate: {}, endDate: {}", startDate, endDate);
        
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        
        List<ProductionOutputData> response = dashboardService.getProductionOutputChartData(startDate, endDate);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Energy Breakdown Chart Data
     * Chart Data - Energy consumption breakdown by source
     */
    @GetMapping("/charts/energy-breakdown")
    @Operation(summary = "Get Energy Breakdown Chart Data", description = "Returns energy consumption breakdown for pie chart")
    public ResponseEntity<List<EnergyBreakdownData>> getEnergyBreakdownChart() {
        log.info("GET /api/dashboard/charts/energy-breakdown");
        
        List<EnergyBreakdownData> response = dashboardService.getEnergyBreakdownChartData();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Equipment Utilization Chart Data
     * Chart Data - Equipment utilization by equipment
     */
    @GetMapping("/charts/equipment-utilization")
    @Operation(summary = "Get Equipment Utilization Chart Data", description = "Returns equipment utilization data for bar chart")
    public ResponseEntity<List<EquipmentUtilizationChartData>> getEquipmentUtilizationChart() {
        log.info("GET /api/dashboard/charts/equipment-utilization");
        
        List<EquipmentUtilizationChartData> response = dashboardService.getEquipmentUtilizationChartData();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get Quality Trend Chart Data
     * Chart Data - Quality pass rate trend over time
     */
    @GetMapping("/charts/quality-trend")
    @Operation(summary = "Get Quality Trend Chart Data", description = "Returns quality pass rate data for trend chart")
    public ResponseEntity<List<QualityTrendData>> getQualityTrendChart(
        @Parameter(description = "Start date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        
        @Parameter(description = "End date (format: yyyy-MM-dd)")
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        log.info("GET /api/dashboard/charts/quality-trend - startDate: {}, endDate: {}", startDate, endDate);
        
        if (startDate == null) startDate = LocalDate.now().minusDays(7);
        if (endDate == null) endDate = LocalDate.now();
        
        List<QualityTrendData> response = dashboardService.getQualityTrendChartData(startDate, endDate);
        return ResponseEntity.ok(response);
    }
}