package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.WhatIfRequestDto;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.WhatIfResponseDto;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatIfServiceTest {

    @Mock
    private EnergyComponentRepository componentRepository;

    @InjectMocks
    private WhatIfService service;

    @Test
    void simulationIncludesProductionDemandInEnergyAndCo2Results() {
        when(componentRepository.findAll()).thenReturn(List.of(pvComponent()));

        WhatIfResponseDto result = service.simulate(new WhatIfRequestDto(
                40, new BigDecimal("80"), new BigDecimal("500")));

        assertEquals(new BigDecimal("24000.00"), result.getAdditionalCapexEur());
        assertEquals(new BigDecimal("10.71"), result.getNewPaybackYears());
        assertEquals(new BigDecimal("26.50"), result.getNewSystemCapacityKwp());
        assertEquals(new BigDecimal("23360.00"), result.getAdditionalPvGenerationKwhPerYear());
        assertEquals(new BigDecimal("164250.00"), result.getAdditionalProductionDemandKwhPerYear());
        assertEquals(new BigDecimal("-140890.00"), result.getNetAnnualEnergyBalanceKwh());
        assertEquals(new BigDecimal("-71613.0"), result.getNetAnnualCo2AvoidedKg());
    }

    @Test
    void batteryOnlyScenarioDoesNotClaimZeroYearPayback() {
        when(componentRepository.findAll()).thenReturn(List.of(pvComponent()));

        WhatIfResponseDto result = service.simulate(new WhatIfRequestDto(
                0, new BigDecimal("80"), new BigDecimal("50")));

        assertEquals(new BigDecimal("16425.00"), result.getAdditionalProductionDemandKwhPerYear());
        assertEquals(new BigDecimal("-8212.5"), result.getNetAnnualCo2AvoidedKg());
        assertNull(result.getNewPaybackYears());
    }

    private EnergyComponent pvComponent() {
        EnergyComponent component = new EnergyComponent();
        component.setComponentId("ENG-001");
        component.setCategory(ComponentCategory.PV_ARRAY);
        component.setCapacityValue(new BigDecimal("10.5"));
        return component;
    }
}
