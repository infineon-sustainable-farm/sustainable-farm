package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
public class HealthIssueReferenceRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

    @Autowired
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    @Test
    void saveAndFlush_shouldRefuseASecondRowWithTheSameCode() {
        // Arrange
        healthIssueReferenceRepository.saveAndFlush(new HealthIssueReference(null, "OTHER", "Other",
                HealthIssueKind.OTHER, null, null, "by_definition", NOW));
        // Act & Assert: the constraint behind one "Other" row for two first uses at the same time
        assertThrows(DataIntegrityViolationException.class, () -> healthIssueReferenceRepository.saveAndFlush(
                new HealthIssueReference(null, "OTHER", "Other", HealthIssueKind.OTHER, null, null,
                        "by_definition", NOW)));
    }
}
