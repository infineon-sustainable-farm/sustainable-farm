package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VarietyReferenceMatcherTest {

    private static VarietyReference reference(long id, String varietyName) {
        return new VarietyReference(id, varietyName, 160.0, "Zalka_2025", 2, 4, "FAO_mango_burkina", null);
    }

    private static final List<VarietyReference> REFERENCES = List.of(
            reference(1L, "Keitt"),
            reference(2L, "Kent"),
            reference(3L, "Amelie"));

    @Test
    void matchKey_shouldDropAccentsCaseAndSurroundingSpaces() {
        // Act & Assert
        assertEquals("amelie", VarietyReferenceMatcher.matchKey("Amélie"));
        assertEquals("amelie", VarietyReferenceMatcher.matchKey("amelie"));
        assertEquals("amelie", VarietyReferenceMatcher.matchKey("AMELIE "));
        assertEquals("amelie", VarietyReferenceMatcher.matchKey("  AMÉLIE\t"));
    }

    @Test
    void find_shouldFindAmelie_whateverTheCaseAccentsAndSpaces() {
        // Act & Assert
        for (String name : List.of("Amélie", "amelie", "AMELIE ", "Amelie")) {
            Optional<VarietyReference> found = VarietyReferenceMatcher.find(name, REFERENCES);
            assertEquals(3L, found.orElseThrow().getId(), name);
        }
    }

    @Test
    void find_shouldReturnEmpty_whenVarietyIsNotInTheReference() {
        // Act & Assert: never guessed from a close name
        assertTrue(VarietyReferenceMatcher.find("Palmer", REFERENCES).isEmpty());
        assertTrue(VarietyReferenceMatcher.find("Kei", REFERENCES).isEmpty());
        assertTrue(VarietyReferenceMatcher.find("Keitt B", REFERENCES).isEmpty());
    }

    @Test
    void find_shouldReturnEmpty_whenReferenceIsEmpty() {
        // Act & Assert
        assertTrue(VarietyReferenceMatcher.find("Keitt", List.of()).isEmpty());
    }

    @Test
    void find_shouldTakeTheOldestRow_whenTwoRowsShareTheKey() {
        // Arrange: only possible after a manual edit of the table
        List<VarietyReference> references = List.of(reference(7L, "Amélie"), reference(3L, "Amelie"));
        // Act & Assert
        assertEquals(3L, VarietyReferenceMatcher.find("amelie", references).orElseThrow().getId());
    }
}
