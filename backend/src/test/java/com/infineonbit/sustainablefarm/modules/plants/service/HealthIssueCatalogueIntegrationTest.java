package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthFindingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthInspectionRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthIssueRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthFindingResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthInspectionResponse;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthInspectionRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The catalogue of pests and diseases as the user fills it, through the real
 * services, their transactions and the database. Outside the dev profile no
 * catalogue is loaded at startup; each test removes the inspections and the
 * catalogue rows it added.
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthIssueCatalogueIntegrationTest {

    /** Each race runs this many times, since H2 does not reproduce it on every trial. */
    private static final int TRIALS = 10;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private HealthIssueService healthIssueService;

    @Autowired
    private HealthInspectionService healthInspectionService;

    @Autowired
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    @Autowired
    private HealthInspectionRepository healthInspectionRepository;

    @Autowired
    private HealthFindingRepository healthFindingRepository;

    /** The inspections the current test recorded. */
    private final List<Long> inspectionIds = new ArrayList<>();

    /** The catalogue codes the current test added. */
    private final Set<String> addedCodes = new LinkedHashSet<>();

    @AfterEach
    void removeWhatTheTestAdded() {
        removeTheInspections();
        addedCodes.forEach(this::removeTheIssue);
        addedCodes.clear();
    }

    private void removeTheInspections() {
        if (!inspectionIds.isEmpty()) {
            healthFindingRepository.deleteAll(healthFindingRepository.findByInspectionIds(inspectionIds));
            healthInspectionRepository.deleteAllById(inspectionIds);
            inspectionIds.clear();
        }
    }

    private void removeTheIssue(String code) {
        healthIssueReferenceRepository.findByCode(code).ifPresent(healthIssueReferenceRepository::delete);
    }

    /**
     * Runs the calls together and returns, for each one in order, its result
     * or the exception it threw.
     */
    private static List<Object> together(List<Callable<Object>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        CountDownLatch ready = new CountDownLatch(calls.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (Callable<Object> call : calls) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        return call.call();
                    } catch (RuntimeException refused) {
                        return refused;
                    }
                }));
            }
            ready.await();
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static HealthInspectionRequest inspection(String blockCode, HealthFindingRequest finding) {
        return new HealthInspectionRequest(null, blockCode, LocalDate.of(2026, 10, 1), 48.0, "Awa", "VISUAL",
                List.of(finding));
    }

    @Test
    void startup_shouldLoadNoCatalogue_outsideTheDevProfile() {
        // Assert: the loader is not even a bean, so no default catalogue reaches another environment
        assertTrue(applicationContext.getBeansOfType(HealthIssueReferenceLoader.class).isEmpty());
    }

    @Test
    void anIssueAddedByTheUser_shouldBeUsableByAnInspection() {
        // Act
        HealthIssueResponse added = healthIssueService.createIssue(
                new HealthIssueRequest(" Powdery mildew ", "disease", null, null, null));
        addedCodes.add(added.code());
        HealthInspectionResponse recorded = healthInspectionService.recordInspection(
                inspection("HP", new HealthFindingRequest("powdery_mildew", null, null)));
        inspectionIds.add(recorded.id());
        // Assert
        assertEquals("POWDERY_MILDEW", added.code());
        assertEquals("Powdery mildew", added.name());
        assertEquals("user_entry", added.source());
        HealthFindingResponse finding = recorded.findings().getFirst();
        assertEquals("POWDERY_MILDEW", finding.issueCode());
        assertEquals(HealthIssueKind.DISEASE, finding.issueKind());
    }

    @Test
    void twoFirstOtherInspectionsSentTogether_shouldAddOneOtherRow_andStoreBoth() throws Exception {
        addedCodes.add(HealthIssueReference.OTHER_CODE);
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: no "Other" row yet
            removeTheInspections();
            removeTheIssue(HealthIssueReference.OTHER_CODE);
            String blockCode = "HO" + trial;
            // Act
            List<Object> results = together(List.of(
                    () -> healthInspectionService.recordInspection(
                            inspection(blockCode, new HealthFindingRequest("OTHER", "Leaf curl", null))),
                    () -> healthInspectionService.recordInspection(
                            inspection(blockCode, new HealthFindingRequest("other", "Sooty mould", null)))));
            results.stream()
                    .filter(HealthInspectionResponse.class::isInstance)
                    .forEach(result -> inspectionIds.add(((HealthInspectionResponse) result).id()));
            // Assert: both inspections stored, their findings on the same single "Other" row
            assertEquals(2, inspectionIds.size(), "trial " + trial + ": " + results);
            // findByCode fails on a second row with the same code
            HealthIssueReference other = healthIssueReferenceRepository.findByCode("OTHER").orElseThrow();
            assertEquals("Other", other.getName());
            assertEquals(HealthIssueKind.OTHER, other.getKind());
            assertEquals("by_definition", other.getSource());
            List<HealthFinding> findings = healthFindingRepository.findByInspectionIds(inspectionIds);
            assertEquals(2, findings.size(), "trial " + trial);
            for (HealthFinding finding : findings) {
                assertEquals(other.getId(), finding.getIssue().getId(), "trial " + trial);
            }
        }
    }

    @Test
    void theSameIssueAddedTwiceTogether_shouldKeepOneRow_andRefuseTheOtherWith409() throws Exception {
        for (int trial = 1; trial <= TRIALS; trial++) {
            // Arrange: a new name on each trial
            String code = "GALL_MIDGE_" + trial;
            addedCodes.add(code);
            HealthIssueRequest request = new HealthIssueRequest("Gall midge " + trial, "PEST", null, null, null);
            // Act
            List<Object> results = together(List.of(
                    () -> healthIssueService.createIssue(request),
                    () -> healthIssueService.createIssue(request)));
            // Assert: one added; the other refused, by the unique code or, once the first is committed, by the name
            assertEquals(1, results.stream().filter(HealthIssueResponse.class::isInstance).count(),
                    "trial " + trial + ": " + results);
            assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count(),
                    "trial " + trial + ": " + results);
            assertTrue(healthIssueReferenceRepository.findByCode(code).isPresent(), "trial " + trial);
        }
    }
}
