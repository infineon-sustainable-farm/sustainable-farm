package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
public class HealthFindingRepositoryTest {

    @Autowired
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    @Autowired
    private HealthInspectionRepository healthInspectionRepository;

    @Autowired
    private HealthFindingRepository healthFindingRepository;

    private HealthIssueReference issue(String code, HealthIssueKind kind) {
        return healthIssueReferenceRepository.save(
                new HealthIssueReference(null, code, code, kind, null, null, "by_definition", null));
    }

    private HealthInspection inspection(Integer farmId, String blockCode, LocalDate inspectedOn) {
        HealthInspection inspection = new HealthInspection();
        inspection.setFarmId(farmId);
        inspection.setBlockCode(blockCode);
        inspection.setInspectedOn(inspectedOn);
        inspection.setHealthScorePct(48);
        inspection.setSource("user_entry");
        return healthInspectionRepository.save(inspection);
    }

    private HealthFinding finding(HealthInspection inspection, HealthIssueReference issue, String treeLabel) {
        HealthFinding finding = new HealthFinding();
        finding.setInspection(inspection);
        finding.setIssue(issue);
        finding.setTreeLabel(treeLabel);
        finding.setSource("user_entry");
        return healthFindingRepository.save(finding);
    }

    @Test
    void findByInspectionIds_shouldReturnTheFindingsOfThoseInspections_withTheirIssue() {
        // Arrange
        HealthIssueReference anthracnose = issue("ANTHRACNOSE", HealthIssueKind.DISEASE);
        HealthIssueReference mealybug = issue("MANGO_MEALYBUG", HealthIssueKind.PEST);
        HealthInspection blockC = inspection(null, "C", LocalDate.of(2026, 9, 1));
        HealthInspection blockD = inspection(null, "D", LocalDate.of(2026, 9, 2));
        HealthInspection notAskedFor = inspection(null, "E", LocalDate.of(2026, 9, 3));
        HealthFinding first = finding(blockC, anthracnose, "42");
        HealthFinding second = finding(blockC, mealybug, null);
        HealthFinding third = finding(blockD, mealybug, null);
        finding(notAskedFor, anthracnose, null);
        // Act
        List<HealthFinding> findings = healthFindingRepository.findByInspectionIds(
                List.of(blockD.getId(), blockC.getId()));
        // Assert: by inspection then identifier, with the issue at hand
        assertEquals(List.of(first.getId(), second.getId(), third.getId()),
                findings.stream().map(HealthFinding::getId).toList());
        assertEquals(List.of("ANTHRACNOSE", "MANGO_MEALYBUG", "MANGO_MEALYBUG"),
                findings.stream().map(finding -> finding.getIssue().getCode()).toList());
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheInspection_andOrderByDateThenIds() {
        // Arrange: the later inspection saved first
        HealthIssueReference anthracnose = issue("ANTHRACNOSE", HealthIssueKind.DISEASE);
        HealthInspection september21 = inspection(null, "C", LocalDate.of(2026, 9, 21));
        HealthInspection september1 = inspection(null, "C", LocalDate.of(2026, 9, 1));
        HealthInspection farm1BlockD = inspection(1, "D", LocalDate.of(2026, 9, 2));
        HealthFinding late = finding(september21, anthracnose, null);
        HealthFinding early = finding(september1, anthracnose, "42");
        HealthFinding earlySecond = finding(september1, anthracnose, "43");
        HealthFinding blockD = finding(farm1BlockD, anthracnose, null);
        // Act & Assert: by inspection date, inspection, then identifier
        assertEquals(List.of(early.getId(), earlySecond.getId(), blockD.getId(), late.getId()),
                find(null, null, null, null));
        // Act & Assert: the farm, block and dates of the inspection
        assertEquals(List.of(blockD.getId()), find(1, null, null, null));
        assertEquals(List.of(early.getId(), earlySecond.getId(), late.getId()), find(null, "C", null, null));
        assertEquals(List.of(blockD.getId(), late.getId()), find(null, null, LocalDate.of(2026, 9, 2), null));
        assertEquals(List.of(early.getId(), earlySecond.getId()), find(null, null, null, LocalDate.of(2026, 9, 1)));
    }

    private List<Long> find(Integer farmId, String blockCode, LocalDate from, LocalDate to) {
        return healthFindingRepository.findByOptionalFilters(farmId, blockCode, from, to).stream()
                .map(HealthFinding::getId)
                .toList();
    }
}
