package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.DashboardSummaryDto;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.CarbonMetric;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ConsumptionLog;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.CarbonMetricRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.ConsumptionLogRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final EnergyComponentRepository componentRepository;
    private final ConsumptionLogRepository consumptionLogRepository;
    private final CarbonMetricRepository carbonMetricRepository;

    @Autowired
    public DashboardService(EnergyComponentRepository componentRepository,
                             ConsumptionLogRepository consumptionLogRepository,
                             CarbonMetricRepository carbonMetricRepository) {
        this.componentRepository = componentRepository;
        this.consumptionLogRepository = consumptionLogRepository;
        this.carbonMetricRepository = carbonMetricRepository;
    }

    public DashboardSummaryDto getSummary() {
        List<EnergyComponent> components = componentRepository.findAll();

        BigDecimal totalPvCapacity = components.stream()
                .filter(c -> c.getCategory() == ComponentCategory.PV_ARRAY)
                .map(EnergyComponent::getCapacityValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ConsumptionLog> todayLogs = consumptionLogRepository.findByDate(LocalDate.now());
        BigDecimal todayConsumption = todayLogs.stream()
                .map(ConsumptionLog::getKwhConsumed)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String generatorStatus = components.stream()
                .filter(c -> c.getCategory() == ComponentCategory.GENERATOR)
                .findFirst()
                .map(c -> c.getStatus() != null ? c.getStatus().name() : "UNKNOWN")
                .orElse("NOT_CONFIGURED");

        BigDecimal co2ThisMonth = carbonMetricRepository.findAll().stream()
                .filter(m -> m.getPeriod() != null && m.getPeriod().getMonth() == LocalDate.now().getMonth())
                .map(CarbonMetric::getCo2AvoidedKg)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardSummaryDto(totalPvCapacity, todayConsumption, generatorStatus, co2ThisMonth);
    }
}
