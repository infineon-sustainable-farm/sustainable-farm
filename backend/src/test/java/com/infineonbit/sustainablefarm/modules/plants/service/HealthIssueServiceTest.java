package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthIssueRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthIssueServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T10:00:00Z");

    @Mock
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    /** Left unstubbed: it opens no real transaction, and records the calls of the service. */
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private HealthIssueService healthIssueService;

    static HealthIssueReference issue(long id, String code, String name, HealthIssueKind kind) {
        return new HealthIssueReference(id, code, name, kind, null, null, "by_definition", NOW);
    }

    /** Saving gives the row an identifier, as the database would. */
    private void savesAssignId(long id) {
        when(healthIssueReferenceRepository.saveAndFlush(any(HealthIssueReference.class))).thenAnswer(invocation -> {
            HealthIssueReference issue = invocation.getArgument(0);
            issue.setId(id);
            return issue;
        });
    }

    private HealthIssueReference savedIssue() {
        ArgumentCaptor<HealthIssueReference> captor = ArgumentCaptor.forClass(HealthIssueReference.class);
        verify(healthIssueReferenceRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static HealthIssueRequest disease(String name) {
        return new HealthIssueRequest(name, "DISEASE", null, null, null);
    }

    @Test
    void getAllIssues_shouldListPestsThenDiseasesThenOther_eachByName() {
        // Arrange: stored in no particular order, with an issue added after "Other"
        when(healthIssueReferenceRepository.findAll()).thenReturn(List.of(
                issue(8, "OTHER", "Other", HealthIssueKind.OTHER),
                issue(5, "ANTHRACNOSE", "Anthracnose", HealthIssueKind.DISEASE),
                issue(3, "TERMITES", "Termites", HealthIssueKind.PEST),
                issue(9, "POWDERY_MILDEW", "Powdery mildew", HealthIssueKind.DISEASE),
                issue(1, "FRUIT_FLY", "Fruit flies", HealthIssueKind.PEST)));
        // Act
        List<HealthIssueResponse> issues = healthIssueService.getAllIssues();
        // Assert
        assertEquals(List.of("FRUIT_FLY", "TERMITES", "ANTHRACNOSE", "POWDERY_MILDEW", "OTHER"),
                issues.stream().map(HealthIssueResponse::code).toList());
        assertEquals(new HealthIssueResponse(1L, "FRUIT_FLY", "Fruit flies", HealthIssueKind.PEST, null, null,
                "by_definition"), issues.getFirst());
    }

    @Test
    void createIssue_shouldDeriveTheCodeFromTheName_andRecordAUserEntry() {
        // Arrange
        when(healthIssueReferenceRepository.findAll()).thenReturn(List.of());
        savesAssignId(9);
        // Act
        HealthIssueResponse response = healthIssueService.createIssue(disease("Powdery mildew"), NOW);
        // Assert
        HealthIssueReference saved = savedIssue();
        assertEquals("POWDERY_MILDEW", saved.getCode());
        assertEquals("Powdery mildew", saved.getName());
        assertEquals(HealthIssueKind.DISEASE, saved.getKind());
        assertNull(saved.getScientificName());
        assertNull(saved.getEppoCode());
        assertEquals("user_entry", saved.getSource());
        assertEquals(NOW, saved.getLastUpdated());
        assertEquals(new HealthIssueResponse(9L, "POWDERY_MILDEW", "Powdery mildew", HealthIssueKind.DISEASE,
                null, null, "user_entry"), response);
    }

    @Test
    void createIssue_shouldTrimEveryText_andUpperCaseTheKindAndTheEppoCode() {
        // Arrange
        when(healthIssueReferenceRepository.findAll()).thenReturn(List.of());
        savesAssignId(5);
        // Act
        healthIssueService.createIssue(new HealthIssueRequest(" Anthracnose ", " disease ",
                " Colletotrichum gloeosporioides ", " collgl ", " Dianda_2025 "), NOW);
        // Assert
        HealthIssueReference saved = savedIssue();
        assertEquals("ANTHRACNOSE", saved.getCode());
        assertEquals("Anthracnose", saved.getName());
        assertEquals(HealthIssueKind.DISEASE, saved.getKind());
        assertEquals("Colletotrichum gloeosporioides", saved.getScientificName());
        assertEquals("COLLGL", saved.getEppoCode());
        assertEquals("Dianda_2025", saved.getSource());
    }

    @Test
    void createIssue_shouldTreatBlankOptionalTextsAsMissing() {
        // Arrange
        when(healthIssueReferenceRepository.findAll()).thenReturn(List.of());
        savesAssignId(3);
        // Act
        healthIssueService.createIssue(new HealthIssueRequest("Termites", "pest", "  ", " ", "   "), NOW);
        // Assert
        HealthIssueReference saved = savedIssue();
        assertEquals(HealthIssueKind.PEST, saved.getKind());
        assertNull(saved.getScientificName());
        assertNull(saved.getEppoCode());
        assertEquals("user_entry", saved.getSource());
    }

    @Test
    void createIssue_shouldThrow409_whenTheNameExists_ignoringCaseAccentsAndSurroundingSpaces() {
        // Arrange
        when(healthIssueReferenceRepository.findAll()).thenReturn(List.of(
                issue(9, "POWDERY_MILDEW", "Powdery mildew", HealthIssueKind.DISEASE)));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> healthIssueService.createIssue(disease(" POWDERY MILDÉW "), NOW));
        // Assert: the name as stored
        assertEquals("A health issue named Powdery mildew already exists", exception.getMessage());
        verify(healthIssueReferenceRepository, never()).saveAndFlush(any());
    }

    @Test
    void createIssue_shouldThrow409_whenAnotherNameGivesACodeAlreadyTaken() {
        // Arrange
        when(healthIssueReferenceRepository.findAll()).thenReturn(List.of(
                issue(9, "POWDERY_MILDEW", "Powdery mildew", HealthIssueKind.DISEASE)));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> healthIssueService.createIssue(disease("Powdery-mildew"), NOW));
        // Assert
        assertEquals("A health issue with code POWDERY_MILDEW already exists", exception.getMessage());
        verify(healthIssueReferenceRepository, never()).saveAndFlush(any());
    }

    @Test
    void createIssue_shouldThrow409_whenTheSameCodeIsAddedAtTheSameTime() {
        // Arrange: the checks pass, then the unique constraint refuses the insert
        when(healthIssueReferenceRepository.findAll()).thenReturn(List.of());
        when(healthIssueReferenceRepository.saveAndFlush(any(HealthIssueReference.class)))
                .thenThrow(new DataIntegrityViolationException("uk_health_issue_reference_code"));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> healthIssueService.createIssue(disease("Powdery mildew"), NOW));
        // Assert: the same answer as for a request sent after the other one
        assertEquals("A health issue with code POWDERY_MILDEW already exists", exception.getMessage());
    }

    @Test
    void codeOf_shouldRemoveAccents_upperCase_andPutOneUnderscoreBetweenWords() {
        assertEquals("POWDERY_MILDEW", HealthIssueReference.codeOf("Powdery mildew"));
        assertEquals("MILDIOU_POUDRE", HealthIssueReference.codeOf(" Mildiou poudré "));
        assertEquals("BLACK_SPOT_BACTERIAL", HealthIssueReference.codeOf("Black-spot (bacterial)"));
        assertEquals("STEM_BORER_2", HealthIssueReference.codeOf("--Stem   borer #2--"));
        assertEquals("OTHER", HealthIssueReference.codeOf("Ôther!"));
    }

    @Test
    void codeOf_shouldBeEmpty_whenTheNameHasNoLetterFromAToZAndNoDigit() {
        assertEquals("", HealthIssueReference.codeOf("!!!"));
        assertEquals("", HealthIssueReference.codeOf("  - _ -  "));
        assertEquals("", HealthIssueReference.codeOf("木瓜"));
    }

    @Test
    void codeOf_shouldCutACodeLongerThan50Characters_withoutEndingOnAnUnderscore() {
        // Arrange: 50 characters, and "ß" becomes "SS", so the code would have 51
        String name = "a".repeat(47) + "ß b";
        // Act
        String code = HealthIssueReference.codeOf(name);
        // Assert: cut after "SS_", then the underscore dropped
        assertEquals("A".repeat(47) + "SS", code);
    }

    @Test
    void getIssueByCode_shouldIgnoreCaseAndSurroundingSpaces() {
        // Arrange
        HealthIssueReference fruitFly = issue(1, "FRUIT_FLY", "Fruit flies", HealthIssueKind.PEST);
        when(healthIssueReferenceRepository.findByCode("FRUIT_FLY")).thenReturn(Optional.of(fruitFly));
        // Act & Assert
        assertSame(fruitFly, healthIssueService.getIssueByCode(" fruit_fly "));
    }

    @Test
    void getIssueByCode_shouldThrow422_whenTheCodeIsNotInTheCatalogue() {
        // Arrange
        when(healthIssueReferenceRepository.findByCode("POWDERY_MILDEW")).thenReturn(Optional.empty());
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthIssueService.getIssueByCode("powdery_mildew"));
        // Assert: the code as looked up
        assertEquals("No health issue with code POWDERY_MILDEW in the catalogue", exception.getMessage());
    }

    @Test
    void getIssuesByCode_shouldKeepTheOrderOfTheCodes_andLookUpEachCodeOnce() {
        // Arrange
        HealthIssueReference anthracnose = issue(5, "ANTHRACNOSE", "Anthracnose", HealthIssueKind.DISEASE);
        HealthIssueReference fruitFly = issue(1, "FRUIT_FLY", "Fruit flies", HealthIssueKind.PEST);
        when(healthIssueReferenceRepository.findByCode("ANTHRACNOSE")).thenReturn(Optional.of(anthracnose));
        when(healthIssueReferenceRepository.findByCode("FRUIT_FLY")).thenReturn(Optional.of(fruitFly));
        // Act
        List<HealthIssueReference> issues = healthIssueService.getIssuesByCode(
                List.of("anthracnose", " FRUIT_FLY ", "ANTHRACNOSE"), NOW);
        // Assert
        assertEquals(List.of(anthracnose, fruitFly, anthracnose), issues);
        verify(healthIssueReferenceRepository, times(1)).findByCode("ANTHRACNOSE");
    }

    @Test
    void getIssuesByCode_shouldAddOther_onItsFirstUse_inATransactionOfItsOwn() {
        // Arrange: no "Other" row until the service adds it
        HealthIssueReference other = issue(8, "OTHER", "Other", HealthIssueKind.OTHER);
        when(healthIssueReferenceRepository.findByCode("OTHER"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(other));
        // Act
        List<HealthIssueReference> issues = healthIssueService.getIssuesByCode(List.of(" other "), NOW);
        // Assert: the row of the dev catalogue, committed on its own
        HealthIssueReference saved = savedIssue();
        assertEquals("OTHER", saved.getCode());
        assertEquals("Other", saved.getName());
        assertEquals(HealthIssueKind.OTHER, saved.getKind());
        assertNull(saved.getScientificName());
        assertNull(saved.getEppoCode());
        assertEquals("by_definition", saved.getSource());
        assertEquals(NOW, saved.getLastUpdated());
        verify(transactionManager).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(transactionManager).commit(any());
        // Assert: the row as read again by the request
        assertEquals(List.of(other), issues);
    }

    @Test
    void getIssuesByCode_shouldUseTheOtherRowOfAnotherRequest_whenBothAddItAtTheSameTime() {
        // Arrange: the insert loses the race on the unique code
        HealthIssueReference other = issue(8, "OTHER", "Other", HealthIssueKind.OTHER);
        when(healthIssueReferenceRepository.findByCode("OTHER"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(other));
        when(healthIssueReferenceRepository.saveAndFlush(any(HealthIssueReference.class)))
                .thenThrow(new DataIntegrityViolationException("uk_health_issue_reference_code"));
        // Act
        List<HealthIssueReference> issues = healthIssueService.getIssuesByCode(List.of("OTHER"), NOW);
        // Assert: its own insert rolled back, the row of the other request used
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        assertEquals(List.of(other), issues);
    }

    @Test
    void getIssuesByCode_shouldThrow422BeforeAddingOther_whenAnotherCodeIsUnknown() {
        // Arrange
        when(healthIssueReferenceRepository.findByCode("OTHER")).thenReturn(Optional.empty());
        when(healthIssueReferenceRepository.findByCode("POWDERY_MILDEW")).thenReturn(Optional.empty());
        // Act
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> healthIssueService.getIssuesByCode(List.of("OTHER", "powdery_mildew"), NOW));
        // Assert: a refused request adds nothing, "Other" included
        assertEquals("No health issue with code POWDERY_MILDEW in the catalogue", exception.getMessage());
        verify(healthIssueReferenceRepository, never()).saveAndFlush(any());
        verify(transactionManager, never()).getTransaction(any());
    }
}
