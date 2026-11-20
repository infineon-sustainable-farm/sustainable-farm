package com.infineonbit.sustainablefarm.modules.cropstorage;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The guard itself: no database, no Spring context is started here. */
class CropStorageH2IsolationGuardTest {

    private static MockEnvironment isolated() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:crop_storage_guard;DB_CLOSE_DELAY=-1")
                .withProperty("spring.datasource.driver-class-name", "org.h2.Driver")
                .withProperty("spring.flyway.enabled", "false");
    }

    private static void initialize(MockEnvironment env) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(env);
        new CropStorageH2IsolationGuard().initialize(context);
    }

    @Test
    void acceptsANamedInMemoryCropStorageH2Database() {
        assertDoesNotThrow(() -> initialize(isolated()));
    }

    @Test
    void refusesPostgresFileOrSharedH2AndMissingUrl() {
        for (String url : new String[] {
                "jdbc:postgresql://localhost:5432/sustainable_farm",
                "jdbc:h2:file:./crop_storage_data",
                "jdbc:h2:tcp://localhost/mem:crop_storage_x",
                "jdbc:h2:mem:testdb"}) {
            MockEnvironment env = isolated().withProperty("spring.datasource.url", url);
            assertThrows(IllegalStateException.class, () -> initialize(env), url);
        }
        MockEnvironment missing = new MockEnvironment()
                .withProperty("spring.datasource.driver-class-name", "org.h2.Driver")
                .withProperty("spring.flyway.enabled", "false");
        assertThrows(IllegalStateException.class, () -> initialize(missing));
    }

    @Test
    void refusesOtherDriverPoolOverrideOrFlyway() {
        assertThrows(IllegalStateException.class, () -> initialize(
                isolated().withProperty("spring.datasource.driver-class-name", "org.postgresql.Driver")));
        assertThrows(IllegalStateException.class, () -> initialize(
                isolated().withProperty("spring.datasource.hikari.jdbc-url", "jdbc:postgresql://db/farm")));
        assertThrows(IllegalStateException.class, () -> initialize(
                isolated().withProperty("spring.flyway.enabled", "true")));
    }
}
