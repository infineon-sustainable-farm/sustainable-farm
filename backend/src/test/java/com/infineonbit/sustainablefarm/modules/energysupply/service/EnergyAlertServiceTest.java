package com.infineonbit.sustainablefarm.modules.energysupply.service;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.AlertStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.EnergyAlertResponse;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.ComponentStatus;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyNotFoundException;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import com.infineonbit.sustainablefarm.modules.notification.entity.Alert;
import com.infineonbit.sustainablefarm.modules.notification.repository.AlertRepository;
import com.infineonbit.sustainablefarm.modules.notification.service.AlertBoardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The energy alerts live in the alert store and keep the JSON of the Energy App. */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = EnergyAlertServiceTest.JpaTestConfiguration.class)
class EnergyAlertServiceTest {

    @Autowired
    private AlertService service;

    @Autowired
    private EnergyComponentRepository componentRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = {Alert.class, EnergyComponent.class})
    @EnableJpaRepositories(basePackageClasses = {AlertRepository.class, EnergyComponentRepository.class})
    @Import({AlertBoardService.class, AlertService.class})
    static class JpaTestConfiguration {
    }

    @Test
    void listsTheAlertsWithTheirComponentAndTimestamp() {
        EnergyComponent battery = component("ENG-902", "Battery bank");
        Instant raisedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        service.raise(battery, AlertSeverity.WARNING, "Battery low", raisedAt, AlertStatus.OPEN);
        service.raise(null, AlertSeverity.INFO, "Inspection done", raisedAt, AlertStatus.RESOLVED);

        assertThat(service.findAll()).hasSize(2);
        assertThat(service.findOpen()).singleElement().satisfies(alert -> {
            assertThat(alert.alertId()).isNotNull();
            assertThat(alert.componentId()).isEqualTo("ENG-902");
            assertThat(alert.componentName()).isEqualTo("Battery bank");
            assertThat(alert.severity()).isEqualTo(AlertSeverity.WARNING);
            assertThat(alert.message()).isEqualTo("Battery low");
            assertThat(alert.timestamp()).isEqualTo(LocalDateTime.ofInstant(raisedAt, ZoneId.systemDefault()));
            assertThat(alert.status()).isEqualTo(AlertStatus.OPEN);
        });
        assertThat(service.isAboutComponent("ENG-902")).isTrue();
        assertThat(service.isAboutComponent("ENG-903")).isFalse();
    }

    @Test
    void resolvingAnAlertStampsItsResolutionAndItCannotBeReopened() {
        EnergyAlertResponse alert = service.raise(component("ENG-904", "Inverter"), AlertSeverity.CRITICAL,
                "Overheat", Instant.now(), AlertStatus.OPEN);

        EnergyAlertResponse resolved = service.updateStatus(alert.alertId(), AlertStatus.RESOLVED);

        assertThat(resolved.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(resolved.componentName()).isEqualTo("Inverter");
        assertThat(alertRepository.findById(alert.alertId()).orElseThrow().getResolvedAt()).isNotNull();
        assertThatThrownBy(() -> service.updateStatus(alert.alertId(), AlertStatus.OPEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateStatus(999_999L, AlertStatus.RESOLVED))
                .isInstanceOf(EnergySupplyNotFoundException.class);
    }

    private EnergyComponent component(String id, String name) {
        EnergyComponent component = new EnergyComponent();
        component.setComponentId(id);
        component.setName(name);
        component.setCategory(ComponentCategory.BATTERY);
        component.setCapacityValue(BigDecimal.ONE);
        component.setStatus(ComponentStatus.OPERATIONAL);
        return componentRepository.saveAndFlush(component);
    }
}
