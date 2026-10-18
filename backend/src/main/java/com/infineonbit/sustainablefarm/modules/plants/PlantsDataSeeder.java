package com.infineonbit.sustainablefarm.modules.plants;

import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * PROVISIONAL. Loads the single real variety row of the project into
 * PostgreSQL at startup.
 *
 * <p>Runs under the {@code dev} Spring profile only
 * (e.g. {@code SPRING_PROFILES_ACTIVE=dev}), like {@code DevDataSeeder}. With no
 * profile set the bean is not created at all, so this never seeds production.
 *
 * <p>Why this exists: the project has neither Flyway nor a SQL script — the
 * schema comes from Hibernate ddl-auto=update — and the team has not yet
 * decided how reference data is managed. This seeder is the stop-gap and is
 * expected to be replaced once that decision is made.
 *
 * <p>Runs only if the varietes table is empty, so restarting the application
 * never duplicates rows.
 *
 * <p>The row below is the only real data available: the Zalka 2025 study.
 * The four fields it does not document (densite_arbres_ha, rendement_reel_kg,
 * vigueur, origine_plant) are left NULL on purpose and must not be filled with
 * a default, an average or a zero. The A/B/C/D blocks shown in the visual
 * mock-up are illustration only and are deliberately NOT seeded.
 */
@Component
@Profile("dev")
public class PlantsDataSeeder implements CommandLineRunner {

    private final VarietyRepository varietyRepository;

    public PlantsDataSeeder(VarietyRepository varietyRepository) {
        this.varietyRepository = varietyRepository;
    }

    @Override
    public void run(String... args) {
        if (varietyRepository.count() > 0) {
            return; // already seeded, do nothing
        }
        seedZalka2025();
    }

    private void seedZalka2025() {
        Variety keitt = new Variety();
        keitt.setName("Keitt");
        keitt.setTreeCount(200);
        keitt.setRowSpacingM(8.0);
        keitt.setTreeSpacingM(8.0);
        keitt.setExpectedYieldKg(44000.0);
        keitt.setBlockCode("A");
        keitt.setSource("Zalka_2025");
        // treeDensityPerHa, actualYieldKg, vigor, plantOrigin and lastUpdated:
        // no source data in the study, left NULL. lastUpdated records when the variety
        // record itself was last revised, which the study does not state; seeding it
        // with the current time would date 2025 data to the day the container started.
        varietyRepository.save(keitt);
    }
}
