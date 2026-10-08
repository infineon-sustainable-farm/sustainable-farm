package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthFindingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthInspectionRequest;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthInspectionRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An inspection and its findings are stored together or not at all. Runs the
 * real service, its transaction and the database. Outside the dev profile the
 * catalogue starts empty, so each test adds the catalogue rows it uses, and
 * removes them after.
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthInspectionTransactionIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");

    @Autowired
    private HealthInspectionService healthInspectionService;

    @Autowired
    private HealthInspectionRepository healthInspectionRepository;

    @Autowired
    private HealthFindingRepository healthFindingRepository;

    @Autowired
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    /** The catalogue rows the current test added. */
    private final List<HealthIssueReference> addedIssues = new ArrayList<>();

    private void addIfMissing(HealthIssueReference issue) {
        if (healthIssueReferenceRepository.findByCode(issue.getCode()).isEmpty()) {
            addedIssues.add(healthIssueReferenceRepository.save(issue));
        }
    }

    @BeforeEach
    void addTheIssuesOfTheTests() {
        addIfMissing(new HealthIssueReference(null, "ANTHRACNOSE", "Anthracnose", HealthIssueKind.DISEASE,
                "Colletotrichum gloeosporioides", "COLLGL", "Dianda_2025", NOW));
        addIfMissing(new HealthIssueReference(null, "OTHER", "Other", HealthIssueKind.OTHER, null, null,
                "by_definition", NOW));
    }

    @AfterEach
    void removeTheIssuesOfTheTests() {
        healthIssueReferenceRepository.deleteAll(addedIssues);
        addedIssues.clear();
    }

    private static HealthInspectionRequest blockC(List<HealthFindingRequest> findings) {
        return new HealthInspectionRequest(null, "C", LocalDate.of(2026, 9, 1), 48.0, "Awa", "VISUAL", findings);
    }

    @Test
    void recordInspection_shouldStoreNothing_whenAnIssueCodeIsUnknown() {
        // Arrange
        long inspectionsBefore = healthInspectionRepository.count();
        long findingsBefore = healthFindingRepository.count();
        HealthInspectionRequest request = blockC(List.of(
                new HealthFindingRequest("ANTHRACNOSE", null, "42"),
                new HealthFindingRequest("powdery_mildew", null, null)));
        // Act
        assertThrows(BusinessRuleException.class, () -> healthInspectionService.recordInspection(request));
        // Assert
        assertEquals(inspectionsBefore, healthInspectionRepository.count());
        assertEquals(findingsBefore, healthFindingRepository.count());
    }

    @Test
    void recordInspection_shouldRollBackTheInspection_whenAFindingCannotBeStored() {
        // Arrange: the second label is longer than its column; the request validation,
        // skipped here, would refuse it, so the failure comes from the database itself,
        // after the inspection and the first finding are written
        long inspectionsBefore = healthInspectionRepository.count();
        long findingsBefore = healthFindingRepository.count();
        HealthInspectionRequest request = blockC(List.of(
                new HealthFindingRequest("ANTHRACNOSE", null, "42"),
                new HealthFindingRequest("OTHER", "x".repeat(61), null)));
        // Act
        assertThrows(DataAccessException.class, () -> healthInspectionService.recordInspection(request));
        // Assert: the inspection and the first finding are rolled back too
        assertEquals(inspectionsBefore, healthInspectionRepository.count());
        assertEquals(findingsBefore, healthFindingRepository.count());
    }
}
