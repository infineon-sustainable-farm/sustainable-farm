package com.infineonbit.sustainablefarm.modules.energysupply;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.CarbonMetricRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.ConsumptionLogRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyAlertRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.GeneratorEventRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.SolarGenerationLogRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnergySupplyDataSeederTest {

    private final EnergyComponentRepository componentRepository = mock(EnergyComponentRepository.class);
    private final ConsumptionLogRepository consumptionLogRepository = mock(ConsumptionLogRepository.class);
    private final CarbonMetricRepository carbonMetricRepository = mock(CarbonMetricRepository.class);
    private final GeneratorEventRepository generatorEventRepository = mock(GeneratorEventRepository.class);
    private final SolarGenerationLogRepository solarGenerationLogRepository = mock(SolarGenerationLogRepository.class);
    private final EnergyAlertRepository energyAlertRepository = mock(EnergyAlertRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);

    @Test
    void seedsComponentsWithPersistInsteadOfMergeBasedSave() {
        when(componentRepository.count()).thenReturn(0L);
        when(consumptionLogRepository.count()).thenReturn(1L);
        when(carbonMetricRepository.count()).thenReturn(1L);
        when(generatorEventRepository.count()).thenReturn(1L);
        when(solarGenerationLogRepository.count()).thenReturn(1L);
        when(energyAlertRepository.count()).thenReturn(1L);

        seeder().run();

        verify(entityManager, times(5)).persist(any(EnergyComponent.class));
        verify(entityManager).flush();
        verify(componentRepository, never()).save(any(EnergyComponent.class));
        verify(entityManager).persist(argThat(component ->
                component instanceof EnergyComponent energyComponent
                        && energyComponent.getComponentId().equals("ENG-001")));
    }

    private EnergySupplyDataSeeder seeder() {
        return new EnergySupplyDataSeeder(
                componentRepository,
                consumptionLogRepository,
                carbonMetricRepository,
                generatorEventRepository,
                solarGenerationLogRepository,
                energyAlertRepository,
                entityManager);
    }
}
