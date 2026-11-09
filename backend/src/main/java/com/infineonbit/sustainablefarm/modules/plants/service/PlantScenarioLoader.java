package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.CurrencyCode;
import com.infineonbit.sustainablefarm.modules.plants.entity.PlantScenario;
import com.infineonbit.sustainablefarm.modules.plants.repository.PlantScenarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Loads the Zalka 2025 business case as a scenario at startup in the dev
 * profile, so a local database has a plan to analyse from the start.
 *
 * <p>Runs in the dev profile only, like {@link AgronomicReferenceLoader}: no
 * reference data is loaded in production. There, the user enters each scenario
 * with {@code POST /api/plants/scenarios}.
 *
 * <p>The values come from the "Plants(Zalka)" tab of
 * {@code sustainable_farm2025.xlsx}, each with its cell as source. Two are
 * derived from two cells, and their source says so: the yield per tree is the
 * output of Keitt divided by its trees (44,000 kg / 200), and the price is the
 * annual income divided by that output (39,917.8 / 44,000 = 0.90722…, kept to
 * 4 decimals), the study giving no price. The amounts are in euros, the
 * currency the "Overall Farm" tab gives for Plants. The study gives no loss
 * rate, no amount for its variable costs and no cost for an added tree: they
 * stay unknown.
 *
 * <p>The scenario is inserted only when no scenario has its name, ignoring
 * case, accents and surrounding spaces. An existing row is never overwritten:
 * a value corrected since is the one that counts. Restarting the application
 * therefore never duplicates nor resets it.
 */
@Component
@Profile("dev")
public class PlantScenarioLoader implements CommandLineRunner {

    /** Name of the loaded scenario. */
    static final String ZALKA_2025 = "Zalka 2025";

    private final PlantScenarioRepository plantScenarioRepository;

    public PlantScenarioLoader(PlantScenarioRepository plantScenarioRepository) {
        this.plantScenarioRepository = plantScenarioRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        load(Instant.now());
    }

    /**
     * Inserts the scenario when its name is missing, at an explicit write time
     * so the {@code lastUpdated} value can be tested.
     *
     * @param now insertion time, stored as {@code lastUpdated}
     */
    void load(Instant now) {
        String key = VarietyReferenceMatcher.matchKey(ZALKA_2025);
        boolean present = plantScenarioRepository.findAll().stream()
                .anyMatch(scenario -> VarietyReferenceMatcher.matchKey(scenario.getName()).equals(key));
        if (!present) {
            plantScenarioRepository.save(zalka2025(now));
        }
    }

    private static PlantScenario zalka2025(Instant now) {
        PlantScenario scenario = new PlantScenario();
        scenario.setName(ZALKA_2025);
        scenario.setVarietyName("Keitt");
        scenario.setTreeCount(200);
        scenario.setTreeCountSource("Zalka_2025_C43");
        scenario.setYieldPerTreeKg(220.0);
        scenario.setYieldSource("derived: Zalka_2025 D4 / B4");
        scenario.setPricePerKg(new BigDecimal("0.9072"));
        scenario.setPriceSource("derived: Zalka_2025 R19 / D4");
        scenario.setCurrency(CurrencyCode.EUR.name());
        scenario.setAnnualFixedCost(new BigDecimal("6707.00"));
        scenario.setAnnualFixedCostSource("Zalka_2025_L23");
        scenario.setInitialInvestment(new BigDecimal("171512.00"));
        scenario.setInvestmentSource("Zalka_2025_H28");
        scenario.setLastUpdated(now);
        return scenario;
    }
}
