package com.infineonbit.sustainablefarm.modules.plants;

import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The variety catalogue rows that the tests of the other modules reference by
 * key, as the catalogue loader writes them: their tables have a foreign key to it.
 */
public final class VarietyCatalogTestData {

    private VarietyCatalogTestData() {
    }

    /** The catalogue of the loader: Keitt, Kent, Amelie, Tommy Atkins and Other. */
    public static List<VarietyReference> defaults() {
        return List.of(
                reference(1L, "Keitt", "KEITT"),
                reference(2L, "Kent", "KENT"),
                reference(3L, "Amelie", "AMELIE"),
                reference(4L, "Tommy Atkins", "TOMMY"),
                reference(5L, "Other", "OTHER"));
    }

    /** Writes the {@link #defaults()} rows that are missing, by key. */
    public static void ensureDefaults(VarietyReferenceRepository repository) {
        for (VarietyReference reference : defaults()) {
            if (repository.findByVarietyKey(reference.getVarietyKey()).isEmpty()) {
                reference.setId(null);
                repository.saveAndFlush(reference);
            }
        }
    }

    private static VarietyReference reference(Long id, String name, String key) {
        return new VarietyReference(id, name, key, BigDecimal.valueOf(200.0), "test", 4, 6, "test", Instant.parse("2026-01-01T00:00:00Z"));
    }
}
