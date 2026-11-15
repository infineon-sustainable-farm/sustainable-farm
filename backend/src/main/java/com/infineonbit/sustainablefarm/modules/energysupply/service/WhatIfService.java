package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.WhatIfRequestDto;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.WhatIfResponseDto;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Recalculates CAPEX, payback, and CO2 reduction when the user adjusts
 * panel count, battery size, or production scale on the What-If tab.
 *
 * Assumptions used (from the Energy Supply Systems Excel business case,
 * Base scenario). TODO (Aida): replace with the exact figures from the
 * "TEAM SUMMARY — Energy System" sheet once the payback discrepancy
 * between tabs is resolved.
 */
@Service
public class WhatIfService {

    private static final BigDecimal PANEL_WATT_PEAK_KW = new BigDecimal("0.40"); // 400W panel
    private static final BigDecimal PANEL_UNIT_COST_EUR = new BigDecimal("180.00");
    private static final BigDecimal BATTERY_COST_PER_KWH_EUR = new BigDecimal("210.00");
    private static final BigDecimal ANNUAL_SAVINGS_PER_KWP_EUR = new BigDecimal("140.00");
    private static final BigDecimal GRID_EMISSION_FACTOR = new BigDecimal("0.500"); // kg CO2/kWh
    private static final BigDecimal PV_EMISSION_FACTOR = new BigDecimal("0.050");   // kg CO2/kWh
    private static final BigDecimal DRYER_KWH_PER_KG = new BigDecimal("0.90");      // extra load per kg/day scaled

    private final EnergyComponentRepository componentRepository;

    @Autowired
    public WhatIfService(EnergyComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
    }

    public WhatIfResponseDto simulate(WhatIfRequestDto request) {
        BigDecimal currentPvCapacity = currentPvCapacityKwp();

        BigDecimal additionalPvCapacity = PANEL_WATT_PEAK_KW
                .multiply(BigDecimal.valueOf(request.getAdditionalPanels() != null ? request.getAdditionalPanels() : 0));

        BigDecimal newSystemCapacity = currentPvCapacity.add(additionalPvCapacity);

        BigDecimal panelCapex = PANEL_UNIT_COST_EUR
                .multiply(BigDecimal.valueOf(request.getAdditionalPanels() != null ? request.getAdditionalPanels() : 0));

        BigDecimal batteryCapex = BATTERY_COST_PER_KWH_EUR
                .multiply(request.getAdditionalBatteryKwh() != null ? request.getAdditionalBatteryKwh() : BigDecimal.ZERO);

        BigDecimal additionalCapex = panelCapex.add(batteryCapex);

        // Extra daily load introduced by scaling up dried-mango production (dryer demand)
        BigDecimal extraDailyLoadKwh = (request.getProductionScaleKgPerDay() != null ? request.getProductionScaleKgPerDay() : BigDecimal.ZERO)
                .multiply(DRYER_KWH_PER_KG);

        BigDecimal annualSavings = additionalPvCapacity.multiply(ANNUAL_SAVINGS_PER_KWP_EUR);

        BigDecimal newPaybackYears = annualSavings.compareTo(BigDecimal.ZERO) > 0
                ? additionalCapex.divide(annualSavings, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal annualKwhFromNewPanels = additionalPvCapacity
                .multiply(BigDecimal.valueOf(365))
                .multiply(BigDecimal.valueOf(4)); // ~4 sun-hours/day equivalent

        BigDecimal co2AvoidedKg = annualKwhFromNewPanels
                .multiply(GRID_EMISSION_FACTOR.subtract(PV_EMISSION_FACTOR));

        BigDecimal baselineAnnualEmissionsKg = currentPvCapacity
                .multiply(BigDecimal.valueOf(365))
                .multiply(BigDecimal.valueOf(4))
                .multiply(GRID_EMISSION_FACTOR)
                .max(BigDecimal.ONE);

        BigDecimal newCo2ReductionPct = co2AvoidedKg
                .divide(baselineAnnualEmissionsKg, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);

        return new WhatIfResponseDto(additionalCapex, newPaybackYears, newCo2ReductionPct, newSystemCapacity);
    }

    private BigDecimal currentPvCapacityKwp() {
        List<EnergyComponent> components = componentRepository.findAll();
        return components.stream()
                .filter(c -> c.getCategory() == ComponentCategory.PV_ARRAY)
                .map(EnergyComponent::getCapacityValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
