package com.infineonbit.sustainablefarm.modules.plants.config;

import com.infineonbit.sustainablefarm.core.schema.CrossModuleForeignKey;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Foreign keys from plants to the people directory of the staff module (ADR 0004), added by
 * {@code core.schema.CrossModuleForeignKeyInstaller}: the observer of an inspection or a maturity
 * observation and the applicator of a treatment or a fertilizer application are people picked
 * in the directory, never typed names.
 */
@Configuration
public class PlantsPersonForeignKeys {

    @Bean
    CrossModuleForeignKey healthInspectionObserverForeignKey() {
        return toPerson("fk_health_inspection_observer_person", "health_inspection", "observer_person_id");
    }

    @Bean
    CrossModuleForeignKey maturityObservationObserverForeignKey() {
        return toPerson("fk_maturity_observation_observer_person", "maturity_observation", "observer_person_id");
    }

    @Bean
    CrossModuleForeignKey healthTreatmentApplicatorForeignKey() {
        return toPerson("fk_health_treatment_applicator_person", "health_treatment", "applicator_person_id");
    }

    @Bean
    CrossModuleForeignKey fertilizerMovementApplicatorForeignKey() {
        return toPerson("fk_fertilizer_movement_applicator_person", "fertilizer_movement", "applicator_person_id");
    }

    private static CrossModuleForeignKey toPerson(String name, String table, String column) {
        return new CrossModuleForeignKey(name, table, column, PersonDirectory.TABLE, PersonDirectory.KEY_COLUMN);
    }
}
