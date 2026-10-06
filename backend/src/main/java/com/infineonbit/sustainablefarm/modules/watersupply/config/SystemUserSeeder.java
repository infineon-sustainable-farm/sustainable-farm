package com.infineonbit.sustainablefarm.modules.watersupply.config;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.User;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cree le compte technique IoT au demarrage de l'application.
 *
 * <p>Pourquoi ce bean existe : {@code irrigation_schedules.created_by} est obligatoire et
 * {@code notifications.user_id} pointe vers {@code users(id)}. Le module watersupply n'a
 * aucun endpoint d'authentification - elle est deleguee au logiciel global - donc ce compte
 * n'a aucun identifiant de connexion et ne peut pas etre utilise pour se connecter.
 *
 * <p>Ce n'est pas une donnee de demonstration : la ligne est requise en production, d'ou le
 * profil {@code !test} (et non {@code dev}). Les migrations Flyway ne portent que le schema
 * (PR-33) : les deux contraintes V2 vers {@code users} sont ajoutees {@code NOT VALID}
 * precisement parce que cette ligne n'existe pas encore au moment ou Flyway s'execute.
 *
 * <p>Sous le profil {@code test}, c'est {@code TestSystemUserSeeder} qui fournit la meme
 * ligne : les tests desactivent Flyway et travaillent sur H2.
 *
 * <p>Execution idempotente : la ligne est creee une seule fois.
 */
@Component
@Profile("!test")
public class SystemUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;

    public SystemUserSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findById(SystemUsers.IOT_SYSTEM_USER_ID).isPresent()) {
            return;
        }
        User system = new User();
        system.setId(SystemUsers.IOT_SYSTEM_USER_ID);
        system.setFirstName("Systeme");
        system.setLastName("IoT");
        system.setEmail("systeme.iot@watersupply.local");
        system.setStatus(false);
        userRepository.save(system);
    }
}
