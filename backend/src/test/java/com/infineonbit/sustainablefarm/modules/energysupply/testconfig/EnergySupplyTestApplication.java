package com.infineonbit.sustainablefarm.modules.energysupply.testconfig;

import com.infineonbit.sustainablefarm.core.config.SecurityConfig;
import com.infineonbit.sustainablefarm.modules.energysupply.EnergySupplyDataSeeder;
import com.infineonbit.sustainablefarm.modules.energysupply.controller.EnergyComponentController;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyExceptionHandler;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.service.EnergyComponentService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = EnergyComponent.class)
@EnableJpaRepositories(basePackageClasses = EnergyComponentRepository.class)
@Import({
        EnergySupplyDataSeeder.class,
        SecurityConfig.class,
        EnergyComponentController.class,
        EnergyComponentService.class,
        EnergySupplyExceptionHandler.class
})
public class EnergySupplyTestApplication {
}
