package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDetails;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus;
import com.infineonbit.sustainablefarm.modules.notification.api.SourceModule;
import com.infineonbit.sustainablefarm.modules.notification.entity.Alert;
import com.infineonbit.sustainablefarm.modules.notification.repository.AlertRepository;
import com.infineonbit.sustainablefarm.modules.notification.service.AlertBoardService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.AlertRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.AlertResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The sales alerts live in the alert store and keep the JSON values of the former {@code alerts} table. */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = SalesAlertServiceTest.JpaTestConfiguration.class)
class SalesAlertServiceTest {

    @Autowired
    private AlertService service;

    @Autowired
    private AlertBoard board;

    @Autowired
    private AlertRepository repository;

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Alert.class)
    @EnableJpaRepositories(basePackageClasses = AlertRepository.class)
    @Import({AlertBoardService.class, AlertService.class})
    static class JpaTestConfiguration {
    }

    @Test
    void createStoresASalesAlertAndAnswersWithTheFormerValues() {
        AlertResponse created = service.create(new AlertRequest("ALT-1", "High Demand", "critical", "Demand up",
                "Germany", "Burkina Faso", "product", 3, "open", null));

        assertThat(created.alertId()).isNotNull();
        assertThat(created.severity()).isEqualTo("critical");
        assertThat(created.status()).isEqualTo("open");
        assertThat(created.relatedTable()).isEqualTo("product");
        assertThat(created.relatedId()).isEqualTo(3);
        assertThat(created.createdAt()).isNotNull();
        Alert stored = repository.findById(created.alertId()).orElseThrow();
        assertThat(stored.getSource()).isEqualTo(SourceModule.SALESMARKETING);
        assertThat(stored.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(stored.getCode()).isEqualTo("ALT-1");
    }

    @Test
    void updateReplacesTheFieldsAndKeepsTheCreationTime() {
        AlertResponse created = service.create(new AlertRequest("ALT-2", "High Demand", "critical", "Demand up",
                "Germany", null, null, null, "open", null));
        LocalDateTime resolvedAt = LocalDateTime.of(2026, 11, 18, 10, 0);

        AlertResponse updated = service.update(created.alertId(), new AlertRequest("ALT-2", "High Demand",
                "warning", "Handled", null, null, null, null, "resolved", resolvedAt));

        assertThat(updated.status()).isEqualTo("resolved");
        assertThat(updated.severity()).isEqualTo("warning");
        assertThat(updated.region()).isNull();
        assertThat(updated.resolvedAt()).isEqualTo(resolvedAt);
        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
    }

    @Test
    void valuesOutsideTheFormerCheckConstraintsAreRefused() {
        assertThatThrownBy(() -> service.create(new AlertRequest(null, null, "huge", "m",
                null, null, null, null, "open", null))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.create(new AlertRequest(null, null, "Critical", "m",
                null, null, null, null, "open", null))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.create(new AlertRequest(null, null, "info", "m",
                null, null, null, null, "closed", null))).isInstanceOf(ConflictException.class);
    }

    @Test
    void anAlertWithoutStatusIsOpen() {
        assertThat(service.create(new AlertRequest(null, null, "info", "m", null, null, null, null, null, null))
                .status()).isEqualTo("open");
    }

    @Test
    void theAlertsOfAnotherModuleAreNotSalesAlerts() {
        Long waterAlert = board.raise(SourceModule.WATERSUPPLY,
                AlertDetails.builder().title("Leak").status(AlertStatus.OPEN).build()).id();

        assertThat(service.getAll()).isEmpty();
        assertThatThrownBy(() -> service.getById(waterAlert)).isInstanceOf(ResourceNotFoundException.class);
        service.delete(waterAlert);
        assertThat(repository.existsById(waterAlert)).isTrue();
    }
}
