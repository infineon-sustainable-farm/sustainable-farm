package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ForecastModelRunTest {

    @Test
    void onCreateSetsCreatedAtWhenMissing() {
        ForecastModelRun modelRun = new ForecastModelRun();

        modelRun.onCreate();

        assertNotNull(modelRun.getCreatedAt());
    }

    @Test
    void onCreatePreservesProvidedCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 7, 13, 0);
        ForecastModelRun modelRun = new ForecastModelRun();
        modelRun.setCreatedAt(createdAt);

        modelRun.onCreate();

        assertEquals(createdAt, modelRun.getCreatedAt());
    }
}
