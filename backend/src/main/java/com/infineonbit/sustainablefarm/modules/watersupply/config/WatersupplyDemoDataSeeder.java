package com.infineonbit.sustainablefarm.modules.watersupply.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Charge le jeu de demonstration water supply au demarrage, profil {@code dev} uniquement.
 *
 * <p>Pourquoi ce bean existe : les migrations Flyway ne doivent porter ni comptes ni
 * donnees de demonstration (PR-33) - Flyway n'a aucun mecanisme de profil, ces valeurs
 * partiraient en production. Le script vit donc dans {@code resources/seed/} et n'est
 * execute que sous le profil {@code dev}, comme {@code DevDataSeeder} et
 * {@code PlantsDataSeeder}.
 *
 * <p>Le script est idempotent (clauses {@code ON CONFLICT ... DO NOTHING}) : il peut etre
 * rejoue a chaque demarrage sans dupliquer de lignes. Il requiert PostgreSQL, comme le
 * reste des migrations du module watersupply.
 */
@Component
@Profile("dev")
public class WatersupplyDemoDataSeeder implements ApplicationRunner {

    private static final Resource SCRIPT = new ClassPathResource("seed/watersupply-demo-data.sql");

    private final DataSource dataSource;

    public WatersupplyDemoDataSeeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        new ResourceDatabasePopulator(SCRIPT).execute(dataSource);
    }
}
