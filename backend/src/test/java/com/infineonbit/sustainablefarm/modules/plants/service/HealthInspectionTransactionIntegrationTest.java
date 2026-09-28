package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthFindingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthInspectionRequest;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthFindingRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthInspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An inspection and its findings are stored together or not at all. Runs the
 * real service, its transaction and the database; the catalogue is the one the
 * loader puts in at startup.
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthInspectionTransactionIntegrationTest {

    @Autowired
    private HealthInspectionService healthInspectionService;

    @Autowired
    private HealthInspectionRepository healthInspectionRepository;

    @Autowired
    private HealthFindingRepository healthFindingRepository;

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
