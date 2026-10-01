package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the overlap rule against concurrent requests: several clients booking
 * the same slot at the same instant must produce exactly one slot, the others
 * being rejected with a conflict. Before the per-date lock, two requests could
 * both pass the overlap check and store overlapping rows.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchedulingConcurrencyIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2027, 3, 1); // a Monday

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @AfterEach
    void removeCreatedSlots() {
        timeSlotRepository.findByDate(DATE).forEach(timeSlotRepository::delete);
    }

    @Test
    void concurrentCreatesForSameSlot_keepExactlyOne() throws Exception {
        int threads = 4;
        TimeSlotRequest request = new TimeSlotRequest();
        request.setDate(DATE);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(11, 0));
        request.setMaxCapacity(10);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return schedulingService.create(request);
                    } catch (ConflictException conflict) {
                        return conflict;
                    }
                }));
            }
            ready.await();
            start.countDown();

            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }

            assertThat(results).filteredOn(TimeSlotResponse.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(ConflictException.class::isInstance)
                    .hasSize(threads - 1);
            assertThat(timeSlotRepository.findByDate(DATE)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }
}