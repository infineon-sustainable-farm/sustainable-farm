package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.PlantScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
public class PlantScenarioRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");

    @Autowired
    private PlantScenarioRepository plantScenarioRepository;

    private static PlantScenario scenario(String name, int treeCount, Double lossRate) {
        return new PlantScenario(null, name, null, treeCount, "user_entry", 220.0, "user_entry",
                lossRate, lossRate == null ? null : "user_entry", new BigDecimal("0.9072"), "user_entry", "EUR",
                new BigDecimal("6707.00"), "user_entry", null, null, new BigDecimal("171512.00"), "user_entry",
                null, null, NOW);
    }

    @Test
    void saveAndFlush_shouldKeepTheDecimalsOfTheAmounts() {
        // Act
        PlantScenario saved = plantScenarioRepository.saveAndFlush(scenario("Plan A", 200, 0.125));
        // Assert
        PlantScenario read = plantScenarioRepository.findById(saved.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("0.9072").compareTo(read.getPricePerKg()));
        assertEquals(0.125, read.getLossRate());
    }

    @Test
    void saveAndFlush_shouldRefuseASecondScenarioWithTheSameName() {
        // Arrange
        plantScenarioRepository.saveAndFlush(scenario("Plan A", 200, null));
        // Act & Assert: the constraint behind the 409 of two identical names sent at the same time
        assertThrows(DataIntegrityViolationException.class,
                () -> plantScenarioRepository.saveAndFlush(scenario("Plan A", 100, null)));
    }

    @Test
    void saveAndFlush_shouldRefuseALossRateAboveOne() {
        // Act & Assert: the database keeps the bound of the API
        assertThrows(DataIntegrityViolationException.class,
                () -> plantScenarioRepository.saveAndFlush(scenario("Plan B", 200, 1.5)));
    }

    @Test
    void saveAndFlush_shouldRefuseAScenarioWithoutTree() {
        // Act & Assert: the database keeps the bound of the API
        assertThrows(DataIntegrityViolationException.class,
                () -> plantScenarioRepository.saveAndFlush(scenario("Plan C", 0, null)));
    }
}
