package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Finds the agronomic reference of a variety from its name.
 *
 * <p>Names are compared ignoring case, accents and surrounding spaces, so
 * "Amélie", "amelie" and "AMELIE " all find "Amelie". The reference holds a
 * handful of rows, so the comparison runs in Java on the whole list rather than
 * in SQL, where accent folding would depend on the database.
 *
 * <p>This matching applies to the reference only. Variety rows are still looked
 * up ignoring case but not accents.
 */
final class VarietyReferenceMatcher {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private VarietyReferenceMatcher() {
    }

    /**
     * The form under which two names are equal: without surrounding spaces,
     * without accents and in lower case.
     *
     * @param name a variety name, not {@code null}
     * @return the comparison key, for example {@code "amelie"} for {@code " Amélie "}
     */
    static String matchKey(String name) {
        String decomposed = Normalizer.normalize(name.strip(), Normalizer.Form.NFD);
        return COMBINING_MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /**
     * The reference row of a variety name.
     *
     * <p>The loader and {@link VarietyReferenceService} check the key before
     * writing, so two rows share a key only after a manual edit of the table, or
     * when two names that differ only in case or accents are entered at the same
     * instant. The row with the smallest identifier, the oldest, is then taken.
     *
     * @param varietyName the variety name, as planted
     * @param references  the whole reference
     * @return the matching row, or empty when the variety has no reference
     */
    static Optional<VarietyReference> find(String varietyName, List<VarietyReference> references) {
        String key = matchKey(varietyName);
        return references.stream()
                .filter(reference -> matchKey(reference.getVarietyName()).equals(key))
                .min(Comparator.comparing(VarietyReference::getId));
    }
}
