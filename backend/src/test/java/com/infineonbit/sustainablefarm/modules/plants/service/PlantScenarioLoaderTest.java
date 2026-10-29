package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.PlantScenario;
import com.infineonbit.sustainablefarm.modules.plants.repository.PlantScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The loader runs against a real database. It is built by hand, not imported as
 * a bean: a bean would already run at context startup, before the test.
 */
@DataJpaTest
@ActiveProfiles("test")
public class PlantScenarioLoaderTest {

    private static final Instant FIRST_START = Instant.parse("2026-10-25T10:00:00Z");
    private static final Instant SECOND_START = Instant.parse("2026-10-26T08:00:00Z");

    @Autowired
    private PlantScenarioRepository plantScenarioRepository;

    private PlantScenarioLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PlantScenarioLoader(plantScenarioRepository);
    }

    private PlantScenario onlyScenario() {
        List<PlantScenario> scenarios = plantScenarioRepository.findAll();
        assertEquals(1, scenarios.size());
        return scenarios.get(0);
    }

    @Test
    void load_shouldInsertTheZalkaScenario_withTheSourceOfEachValue() {
        // Act
        loader.load(FIRST_START);
        // Assert: the values of the study, each with its cell, and the derived ones said so
        PlantScenario zalka = onlyScenario();
        assertEquals("Zalka 2025", zalka.getName());
        assertEquals("Keitt", zalka.getVarietyName());
        assertEquals(200, zalka.getTreeCount());
        assertEquals("Zalka_2025_C43", zalka.getTreeCountSource());
        assertEquals(220.0, zalka.getYieldPerTreeKg());
        assertEquals("derived: Zalka_2025 D4 / B4", zalka.getYieldSource());
        assertEquals(0, new BigDecimal("0.9072").compareTo(zalka.getPricePerKg()));
        assertEquals("derived: Zalka_2025 R19 / D4", zalka.getPriceSource());
        assertEquals("EUR", zalka.getCurrency());
        assertEquals(0, new BigDecimal("6707").compareTo(zalka.getAnnualFixedCost()));
        assertEquals("Zalka_2025_L23", zalka.getAnnualFixedCostSource());
        assertEquals(0, new BigDecimal("171512").compareTo(zalka.getInitialInvestment()));
        assertEquals("Zalka_2025_H28", zalka.getInvestmentSource());
        assertEquals(FIRST_START, zalka.getLastUpdated());
        // Assert: what the study does not give stays unknown, never 0
        assertNull(zalka.getLossRate());
        assertNull(zalka.getLossRateSource());
        assertNull(zalka.getVariableCostPerKg());
        assertNull(zalka.getVariableCostSource());
        assertNull(zalka.getCostPerAdditionalTree());
        assertNull(zalka.getCostPerAdditionalTreeSource());
    }

    @Test
    void load_shouldNotDuplicateTheScenario_whenRunTwice() {
        // Act
        loader.load(FIRST_START);
        loader.load(SECOND_START);
        // Assert: one row, still dated from the first start
        assertEquals(FIRST_START, onlyScenario().getLastUpdated());
    }

    @Test
    void load_shouldKeepAScenarioCorrectedSince() {
        // Arrange: the price corrected by the user
        loader.load(FIRST_START);
        PlantScenario zalka = onlyScenario();
        zalka.setPricePerKg(new BigDecimal("1.1000"));
        zalka.setPriceSource("market_survey_2026");
        plantScenarioRepository.saveAndFlush(zalka);
        // Act
        loader.load(SECOND_START);
        // Assert
        PlantScenario kept = onlyScenario();
        assertEquals(0, new BigDecimal("1.1").compareTo(kept.getPricePerKg()));
        assertEquals("market_survey_2026", kept.getPriceSource());
    }

    @Test
    void load_shouldInsertNothing_whenTheNameIsTakenInAnotherCase() {
        // Arrange: the scenario renamed by the user, case and spaces only
        loader.load(FIRST_START);
        PlantScenario zalka = onlyScenario();
        zalka.setName(" ZALKA 2025");
        plantScenarioRepository.saveAndFlush(zalka);
        // Act
        loader.load(SECOND_START);
        // Assert
        assertEquals(" ZALKA 2025", onlyScenario().getName());
    }
}
