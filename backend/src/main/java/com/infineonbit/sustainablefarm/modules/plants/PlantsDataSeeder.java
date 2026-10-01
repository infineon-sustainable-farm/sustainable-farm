package com.infineonbit.sustainablefarm.modules.plants;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * PROVISIONAL. Loads the only real plants data of the project (the Zalka 2025
 * study) into PostgreSQL at startup: one variety row and one growth calendar row.
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
 * <p>Each table is seeded only if it is empty, and independently of the other,
 * so restarting the application never duplicates rows.
 *
 * <p>Variety: the fields the study does not document (densite_arbres_ha,
 * rendement_reel_kg, vigueur, origine_plant, date_maj) are left NULL on purpose
 * and must not be filled with a default, an average or a zero. The A/B/C/D
 * blocks shown in the visual mock-up are illustration only and are
 * deliberately NOT seeded.
 *
 * <p>Growth calendar: the study gives no planting date. Its only statement on
 * timing is "Planting Period: Beginning of the rainy season", which is when
 * planting is recommended, not when it happened — no day, month or year. So the
 * planting date stays NULL, and with it the computed age and phase. See
 * {@link #seedZalka2025GrowthCalendar()} for every field.
 */
@Component
@Profile("dev")
public class PlantsDataSeeder implements CommandLineRunner {

    private final VarietyRepository varietyRepository;
    private final GrowthCalendarRepository growthCalendarRepository;

    public PlantsDataSeeder(VarietyRepository varietyRepository,
                            GrowthCalendarRepository growthCalendarRepository) {
        this.varietyRepository = varietyRepository;
        this.growthCalendarRepository = growthCalendarRepository;
    }

    @Override
    public void run(String... args) {
        if (varietyRepository.count() == 0) {
            seedZalka2025();
        }
        if (growthCalendarRepository.count() == 0) {
            seedZalka2025GrowthCalendar();
        }
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

    private void seedZalka2025GrowthCalendar() {
        GrowthCalendar blockA = new GrowthCalendar();
        // Same block as the Keitt variety row.
        blockA.setBlockCode("A");
        blockA.setSource("Zalka_2025");
        // The only qualification the source allows: "Beginning of the rainy season",
        // with no year.
        blockA.setDatePrecision("rainy season (year unknown)");
        // plantingDate: no date in the source. Never replaced by a "plausible" one.
        // currentStage: a field observation; none recorded.
        // phaseYears: the Zalka phase scale is a general reference, not data about
        // this block.
        // localRainfallMm: the source gives 1000-1200 mm/year for the farm, a
        // range and not a local value; taking its midpoint would be an invention.
        // lastUpdated: not stated by the source, as for the variety row.
        // farmId: left NULL, as for the variety row.
        growthCalendarRepository.save(blockA);
    }
}
