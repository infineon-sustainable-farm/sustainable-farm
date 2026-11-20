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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Creating a component on a real database: its key is a generated Long id, and the business
 * code assigned by the caller (ENG-001...) is a unique column, the {@code componentId} of the API.
 */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = EnergyComponentPersistenceTest.JpaTestConfiguration.class)
class EnergyComponentPersistenceTest {

    @Autowired
    private EnergyComponentService service;

    @Autowired
    private EnergyComponentRepository repository;

    @MockitoBean
    private AlertService alertService;

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = EnergyComponent.class)
    @EnableJpaRepositories(basePackageClasses = EnergyComponentRepository.class)
    @Import(EnergyComponentService.class)
    static class JpaTestConfiguration {
    }

    @Test
    void createStoresTheComponentUnderAGeneratedIdWithItsBusinessCode() {
        assertThat(service.create(request("ENG-101")).componentId()).isEqualTo("ENG-101");

        assertThat(repository.findByCode("ENG-101")).hasValueSatisfying(component -> {
            assertThat(component.getId()).isNotNull();
            assertThat(component.getName()).isEqualTo("Rooftop array");
            assertThat(component.getCreatedAt()).isNotNull();
            assertThat(component.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void createRefusesACodeThatAlreadyExists() {
        service.create(request("ENG-102"));

        assertThatThrownBy(() -> service.create(request("ENG-102")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ENG-102");
    }

    @Test
    void theDatabaseRefusesTwoComponentsWithTheSameCode() {
        repository.saveAndFlush(component("ENG-103"));

        assertThatThrownBy(() -> repository.saveAndFlush(component("ENG-103")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static EnergyComponent component(String code) {
        EnergyComponent component = new EnergyComponent();
        component.setCode(code);
        component.setName("Battery bank");
        component.setCategory(ComponentCategory.BATTERY);
        component.setStatus(ComponentStatus.OPERATIONAL);
        return component;
    }

    private static EnergyComponentRequest request(String code) {
        return new EnergyComponentRequest(code, "Rooftop array", ComponentCategory.PV_ARRAY,
                new BigDecimal("12.50"), ComponentStatus.OPERATIONAL);
    }
}
