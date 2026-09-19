package com.infineonbit.sustainablefarm.modules.plants;

import com.infineonbit.sustainablefarm.modules.plants.entity.CalendrierCroissance;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variete;
import com.infineonbit.sustainablefarm.modules.plants.repository.CalendrierCroissanceRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarieteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * PROVISIONAL. Loads the only real plants data of the project (the Zalka 2025
 * study) into PostgreSQL at startup: one variety row and one growth calendar row.
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
public class PlantsDataSeeder implements CommandLineRunner {

    private final VarieteRepository varieteRepository;
    private final CalendrierCroissanceRepository calendrierCroissanceRepository;

    public PlantsDataSeeder(VarieteRepository varieteRepository,
                            CalendrierCroissanceRepository calendrierCroissanceRepository) {
        this.varieteRepository = varieteRepository;
        this.calendrierCroissanceRepository = calendrierCroissanceRepository;
    }

    @Override
    public void run(String... args) {
        if (varieteRepository.count() == 0) {
            seedZalka2025();
        }
        if (calendrierCroissanceRepository.count() == 0) {
            seedZalka2025GrowthCalendar();
        }
    }

    private void seedZalka2025() {
        Variete keitt = new Variete();
        keitt.setNom("Keitt");
        keitt.setNombreArbres(200);
        keitt.setEspacementInterRangM(8.0);
        keitt.setEspacementIntraRangM(8.0);
        keitt.setRendementAttenduKg(44000.0);
        keitt.setBlocParcelle("A");
        keitt.setSource("Zalka_2025");
        // densiteArbresHa, rendementReelKg, vigueur, originePlant and dateMaj:
        // no source data in the study, left NULL. dateMaj records when the variety
        // record itself was last revised, which the study does not state; seeding it
        // with the current time would date 2025 data to the day the container started.
        varieteRepository.save(keitt);
    }

    private void seedZalka2025GrowthCalendar() {
        CalendrierCroissance blockA = new CalendrierCroissance();
        // Same block as the Keitt variety row.
        blockA.setBlocParcelle("A");
        blockA.setSource("Zalka_2025");
        // The only qualification the source allows: "Beginning of the rainy season",
        // with no year.
        blockA.setPrecisionDate("rainy season (year unknown)");
        // datePlantation: no date in the source. Never replaced by a "plausible" one.
        // stadeActuel: a field observation; none recorded.
        // phaseAnnees: the Zalka phase scale is a general reference, not data about
        // this block.
        // pluviometrieLocaleMm: the source gives 1000-1200 mm/year for the farm, a
        // range and not a local value; taking its midpoint would be an invention.
        // dateMaj: not stated by the source, as for the variety row.
        // idFerme: left NULL, as for the variety row.
        calendrierCroissanceRepository.save(blockA);
    }
}
