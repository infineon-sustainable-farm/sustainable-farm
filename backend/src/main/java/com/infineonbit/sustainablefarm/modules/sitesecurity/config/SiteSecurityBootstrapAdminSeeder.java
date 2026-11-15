package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.OperatorService;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first admin from the environment, so that someone can add the
 * other operators. An existing operator is never changed: an admin who was
 * disabled or demoted stays that way after a restart.
 */
@Component
@Profile("!dev")
public class SiteSecurityBootstrapAdminSeeder implements CommandLineRunner {

    static final String BOOTSTRAP_ACTOR = "bootstrap";

    private static final Logger log = LoggerFactory.getLogger(SiteSecurityBootstrapAdminSeeder.class);
    private static final Pattern EMAIL_SHAPE = Pattern.compile("[^@\\s]+@[^@\\s]+");
    private static final int MAX_EMAIL_LENGTH = 254;

    private final OperatorRepository operatorRepository;
    private final String bootstrapEmail;

    public SiteSecurityBootstrapAdminSeeder(
            OperatorRepository operatorRepository,
            @Value("${SITESECURITY_BOOTSTRAP_ADMIN_EMAIL:}") String bootstrapEmail) {
        this.operatorRepository = operatorRepository;
        this.bootstrapEmail = bootstrapEmail;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bootstrapEmail == null || bootstrapEmail.isBlank()) {
            log.warn("SITESECURITY_BOOTSTRAP_ADMIN_EMAIL is not set: no first admin was declared.");
            return;
        }
        String email = OperatorService.normalizeEmail(bootstrapEmail);
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_SHAPE.matcher(email).matches()) {
            log.error("SITESECURITY_BOOTSTRAP_ADMIN_EMAIL is not a valid email: no first admin was created.");
            return;
        }
        if (operatorRepository.existsByEmail(email)) {
            return;
        }
        // Active, without a password: it signs in with an email code (stage 0) and then sets
        // its password in-session with a fresh code (stage 1).
        OperatorEntity admin = OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .role(SiteSecurityRole.ADMIN)
                .active(true)
                .createdBy(BOOTSTRAP_ACTOR)
                .createdAt(Instant.now())
                .build();
        admin.applyRole(AccountValues.AccountRole.ADMIN);
        admin.applyStatus(AccountValues.AccountStatus.ACTIVE);
        operatorRepository.save(admin);
        log.info("Site Security first admin created.");
    }
}
