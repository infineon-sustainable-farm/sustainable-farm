package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyComponentRequest;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Creating a component on a real database: its id is a business id assigned by the caller
 * (ENG-001...). With a generated-value strategy on that id, Hibernate 7 refused new components
 * as detached entities, which broke both the create endpoint and the dev seeder.
 */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = EnergyComponentPersistenceTest.JpaTestConfiguration.class)
class EnergyComponentPersistenceTest {

    @Autowired
    private EnergyComponentService service;

    @Autowired
    private EnergyComponentRepository repository;

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = EnergyComponent.class)
    @EnableJpaRepositories(basePackageClasses = EnergyComponentRepository.class)
    @Import(EnergyComponentService.class)
    static class JpaTestConfiguration {
    }

    @Test
    void createStoresTheComponentUnderItsBusinessId() {
        service.create(request("ENG-101"));

        assertThat(repository.findById("ENG-101"))
                .hasValueSatisfying(c -> assertThat(c.getName()).isEqualTo("Rooftop array"));
    }

    @Test
    void createRefusesAnIdThatAlreadyExists() {
        service.create(request("ENG-102"));

        assertThatThrownBy(() -> service.create(request("ENG-102")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ENG-102");
    }

    @Test
    void saveOfANewComponentWithItsIdInserts() {
        EnergyComponent component = new EnergyComponent();
        component.setComponentId("ENG-103");
        component.setName("Battery bank");
        component.setCategory(ComponentCategory.BATTERY);
        component.setStatus(ComponentStatus.OPERATIONAL);

        repository.saveAndFlush(component);

        assertThat(repository.existsById("ENG-103")).isTrue();
    }

    private static EnergyComponentRequest request(String id) {
        return new EnergyComponentRequest(id, "Rooftop array", ComponentCategory.PV_ARRAY,
                new BigDecimal("12.50"), ComponentStatus.OPERATIONAL);
    }
}
