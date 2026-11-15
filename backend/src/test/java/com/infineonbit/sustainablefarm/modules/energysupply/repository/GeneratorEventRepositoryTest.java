package com.infineonbit.sustainablefarm.modules.energysupply.repository;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.GeneratorEvent;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.GeneratorEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = GeneratorEventRepositoryTest.JpaTestConfiguration.class)
class GeneratorEventRepositoryTest {

    @Autowired
    private GeneratorEventRepository repository;

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = GeneratorEvent.class)
    @EnableJpaRepositories(basePackageClasses = GeneratorEventRepository.class)
    static class JpaTestConfiguration {
    }

    @Test
    void monthlyTotalsIncludeOnlyDatesInTheRequestedMonthAndDefaultToZero() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

        saveEvent(startOfMonth, "2.50", "4.00");
        saveEvent(today, "1.25", null);
        saveEvent(startOfNextMonth, "50.00", "70.00");
        saveEvent(startOfMonth.minusDays(1), "90.00", "100.00");

        GeneratorEventRepository.GeneratorEventMonthlyTotals totals =
                repository.sumMonthlyTotals(startOfMonth, startOfNextMonth);

        assertThat(totals.getDurationHours()).isEqualByComparingTo("3.75");
        assertThat(totals.getFuelLiters()).isEqualByComparingTo("4.00");

        repository.deleteAll();
        GeneratorEventRepository.GeneratorEventMonthlyTotals emptyTotals =
                repository.sumMonthlyTotals(startOfMonth, startOfNextMonth);

        assertThat(emptyTotals.getDurationHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(emptyTotals.getFuelLiters()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private void saveEvent(LocalDate date, String duration, String fuel) {
        GeneratorEvent event = new GeneratorEvent();
        event.setDate(date);
        event.setDurationHours(new BigDecimal(duration));
        event.setFuelLiters(fuel == null ? null : new BigDecimal(fuel));
        repository.save(event);
    }
}
