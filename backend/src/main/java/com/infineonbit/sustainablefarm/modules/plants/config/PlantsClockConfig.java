package com.infineonbit.sustainablefarm.modules.plants.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The clock of the Plants reads that depend on the current date, so that a
 * test can fix the day.
 */
@Configuration
public class PlantsClockConfig {

    /**
     * The system clock in the default time zone: the same day as
     * {@code LocalDate.now()} in the rest of the module.
     *
     * <p>Not a default candidate: it is injected only where it is asked for by
     * name, with {@code @Qualifier("plantsClock")}, so it never competes with a
     * clock another module may declare.
     */
    @Bean(defaultCandidate = false)
    public Clock plantsClock() {
        return Clock.systemDefaultZone();
    }
}
