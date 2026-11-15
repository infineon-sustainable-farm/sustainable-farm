package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A pest or disease of the mango orchard that an inspection can report, such as
 * "Anthracnose" or "Fruit flies".
 *
 * <p>A reference row, sourced and editable in the database. Outside the dev
 * profile the catalogue starts empty: the user adds the pests and diseases with
 * {@code POST /api/plants/health-issues}, and the service adds "Other" the first
 * time an inspection or a treatment uses it. In the dev profile,
 * {@code HealthIssueReferenceLoader} loads a default catalogue at startup. The
 * catalogue is a table and not a Java enum: an enum stored as text gets a CHECK
 * constraint that {@code ddl-auto=update} never widens, so a new issue would
 * need a migration. Here a new issue is one more row, and the code column has
 * no CHECK.
 *
 * <p>{@code code} is the stable key the API receives, upper case, such as
 * {@code ANTHRACNOSE}; the code of an issue added through the API is derived
 * from its name by {@link #codeOf(String)}. {@code eppoCode} is the EPPO code of
 * the organism when it has one, the reference of plant protection services.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "health_issue_reference",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_health_issue_reference_code",
                columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthIssueReference {

    /** Code of "Other", the row that stands for any problem the catalogue does not name. */
    public static final String OTHER_CODE = "OTHER";

    /** Longest code: the length of the column, and of the code pattern of a finding. */
    public static final int CODE_MAX_LENGTH = 50;

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern OTHER_CHARACTERS = Pattern.compile("[^A-Z0-9]+");
    private static final Pattern EDGE_UNDERSCORES = Pattern.compile("^_|_$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = CODE_MAX_LENGTH)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private HealthIssueKind kind;

    @Column(name = "scientific_name")
    private String scientificName;

    @Column(name = "eppo_code", length = 10)
    private String eppoCode;

    /** Where the row comes from, as a code such as {@code "Dianda_2025"}. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /**
     * The code of an issue added under this name: accents removed, upper case,
     * and one {@code _} for each run of characters other than letters and
     * digits, with none at either end. "Powdery mildew" gives
     * {@code POWDERY_MILDEW}, and " Mildiou poudré " gives
     * {@code MILDIOU_POUDRE}.
     *
     * <p>A code longer than {@value #CODE_MAX_LENGTH} characters is cut. A name
     * of at most that length gives a longer code only through the rare letters
     * that become two, such as "ß", which becomes "SS".
     *
     * @param name the name as received, not {@code null}
     * @return the code, empty when the name has neither a letter from A to Z,
     *         accented or not, nor a digit
     */
    public static String codeOf(String name) {
        String withoutAccents = COMBINING_MARKS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD))
                .replaceAll("");
        String code = EDGE_UNDERSCORES.matcher(
                OTHER_CHARACTERS.matcher(withoutAccents.toUpperCase(Locale.ROOT)).replaceAll("_")).replaceAll("");
        if (code.length() > CODE_MAX_LENGTH) {
            code = EDGE_UNDERSCORES.matcher(code.substring(0, CODE_MAX_LENGTH)).replaceAll("");
        }
        return code;
    }
}
