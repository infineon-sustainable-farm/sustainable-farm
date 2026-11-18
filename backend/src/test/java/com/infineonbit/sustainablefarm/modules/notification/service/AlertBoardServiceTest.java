package com.infineonbit.sustainablefarm.modules.notification.service;

import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDetails;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus;
import com.infineonbit.sustainablefarm.modules.notification.entity.Alert;
import com.infineonbit.sustainablefarm.modules.notification.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.time.Instant;

import static com.infineonbit.sustainablefarm.modules.notification.api.SourceModule.ENERGYSUPPLY;
import static com.infineonbit.sustainablefarm.modules.notification.api.SourceModule.SALESMARKETING;
import static com.infineonbit.sustainablefarm.modules.notification.api.SourceModule.WATERSUPPLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The single alert store: every module's alerts in one table, each module scoped to its own. */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = AlertBoardServiceTest.JpaTestConfiguration.class)
class AlertBoardServiceTest {

    @Autowired
    private AlertBoard board;

    @Autowired
    private AlertRepository repository;

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Alert.class)
    @EnableJpaRepositories(basePackageClasses = AlertRepository.class)
    @Import(AlertBoardService.class)
    static class JpaTestConfiguration {
    }

    @Test
    void theAlertsOfEveryModuleShareOneTableWithSourceSeverityAndStatus() {
        board.raise(ENERGYSUPPLY, details("Battery low").severity(AlertSeverity.WARNING).build());
        board.raise(SALESMARKETING, details("High demand").severity(AlertSeverity.CRITICAL).code("ALT-1").build());
        board.raise(WATERSUPPLY, details("Leak").type("critical").build());

        assertThat(repository.findAll())
                .extracting(Alert::getSource, Alert::getTitle, Alert::getStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(ENERGYSUPPLY, "Battery low", AlertStatus.OPEN),
                        org.assertj.core.groups.Tuple.tuple(SALESMARKETING, "High demand", AlertStatus.OPEN),
                        org.assertj.core.groups.Tuple.tuple(WATERSUPPLY, "Leak", AlertStatus.OPEN));
    }

    @Test
    void aModuleOnlySeesAndChangesItsOwnAlerts() {
        AlertDto energy = board.raise(ENERGYSUPPLY, details("Battery low").build());
        board.raise(WATERSUPPLY, details("Leak").build());

        assertThat(board.list(ENERGYSUPPLY, null)).extracting(AlertDto::title).containsExactly("Battery low");
        assertThat(board.find(WATERSUPPLY, energy.id())).isEmpty();
        assertThat(board.markRead(WATERSUPPLY, energy.id())).isEmpty();
        assertThat(board.delete(WATERSUPPLY, energy.id())).isFalse();
        assertThat(board.markAllRead(WATERSUPPLY)).extracting(AlertDto::title).containsExactly("Leak");
        assertThat(board.find(ENERGYSUPPLY, energy.id())).hasValueSatisfying(a -> assertThat(a.read()).isFalse());
    }

    @Test
    void raiseOnceSkipsAnUnreadAlertOfTheSameTitleWithinTheWindow() {
        Duration window = Duration.ofHours(12);
        assertThat(board.raiseOnce(WATERSUPPLY, details("Leak").build(), window)).isPresent();
        assertThat(board.raiseOnce(WATERSUPPLY, details("Leak").build(), window)).isEmpty();
        assertThat(board.raiseOnce(ENERGYSUPPLY, details("Leak").build(), window)).isPresent();

        board.markAllRead(WATERSUPPLY);
        assertThat(board.raiseOnce(WATERSUPPLY, details("Leak").build(), window)).isPresent();

        board.raise(WATERSUPPLY, details("Old").raisedAt(Instant.now().minus(Duration.ofHours(13))).build());
        assertThat(board.raiseOnce(WATERSUPPLY, details("Old").build(), window)).isPresent();
    }

    @Test
    void listFiltersByStatusAndChangeStatusStampsTheResolution() {
        AlertDto open = board.raise(ENERGYSUPPLY, details("Open").build());
        AlertDto toResolve = board.raise(ENERGYSUPPLY, details("To resolve").build());

        AlertDto resolved = board.changeStatus(ENERGYSUPPLY, toResolve.id(), AlertStatus.RESOLVED).orElseThrow();

        assertThat(resolved.resolvedAt()).isNotNull();
        assertThat(board.list(ENERGYSUPPLY, AlertStatus.OPEN)).extracting(AlertDto::id).containsExactly(open.id());
        assertThat(board.list(ENERGYSUPPLY, AlertStatus.RESOLVED)).extracting(AlertDto::id).containsExactly(toResolve.id());
    }

    @Test
    void updateReplacesTheAttributesButKeepsTheRaiseTime() {
        Instant raisedAt = Instant.parse("2026-11-01T10:00:00Z");
        AlertDto alert = board.raise(SALESMARKETING,
                details("High demand").region("Germany").raisedAt(raisedAt).build());

        AlertDto updated = board.update(SALESMARKETING, alert.id(), AlertDetails.builder()
                .message("Changed").status(AlertStatus.ACKNOWLEDGED).build()).orElseThrow();

        assertThat(updated.message()).isEqualTo("Changed");
        assertThat(updated.region()).isNull();
        assertThat(updated.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(updated.raisedAt()).isEqualTo(raisedAt);
    }

    @Test
    void theBusinessCodeIsUnique() {
        board.raise(SALESMARKETING, details("A").code("ALT-1").build());

        assertThatThrownBy(() -> board.raise(SALESMARKETING, details("B").code("ALT-1").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pageFiltersByRecipientAndCountsAndRelatedEntityAreScoped() {
        board.raise(WATERSUPPLY, details("For 7").recipientPersonId(7L).type("Critical").build());
        board.raise(WATERSUPPLY, details("For 8").recipientPersonId(8L).type("info").build());
        board.raise(ENERGYSUPPLY, details("Battery").type("critical")
                .relatedEntity("energy_component", "ENG-002").build());

        var page = board.page(WATERSUPPLY, 7L, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "raisedAt")));
        assertThat(page.getContent()).extracting(AlertDto::title).containsExactly("For 7");
        assertThat(board.page(WATERSUPPLY, null, PageRequest.of(0, 5)).getTotalElements()).isEqualTo(2);
        assertThat(board.count(WATERSUPPLY)).isEqualTo(2);
        assertThat(board.countByType(WATERSUPPLY, "critical")).isEqualTo(1);
        assertThat(board.isAbout(ENERGYSUPPLY, "energy_component", "ENG-002")).isTrue();
        assertThat(board.isAbout(WATERSUPPLY, "energy_component", "ENG-002")).isFalse();
    }

    private static AlertDetails.Builder details(String title) {
        return AlertDetails.builder().title(title).message(title + " message");
    }
}
