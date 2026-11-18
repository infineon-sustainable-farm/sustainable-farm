package com.infineonbit.sustainablefarm.modules.plants.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Agronomic reference of a mango variety: what one tree yields in full
 * production, and when the variety is harvested.
 *
 * <p>A catalogue row, attached to no farm and no block: it says what a variety
 * is worth in general, not what an orchard holds. Each value carries its source,
 * and the user enters and corrects it through
 * {@code /api/plants/variety-references}; the yield forecast reads it on every
 * request and keeps no copy of it in code.
 *
 * <p>It is also the variety catalogue of the application, exposed to the other
 * modules by {@code plants.api.VarietyCatalog}: producttransformation and
 * salesmarketing reference a row by its {@code variety_key}, a stable code such
 * as {@code KEITT}, through a foreign key. The key is set once, when the row is
 * written, and never changes, even when the name is corrected.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "variety_reference",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_variety_reference_variety_name",
                        columnNames = "variety_name"),
                @UniqueConstraint(
                        name = "uk_variety_reference_variety_key",
                        columnNames = "variety_key")
        },
        check = {
                @CheckConstraint(
                        name = "ck_variety_reference_harvest_start_month",
                        constraint = "harvest_start_month BETWEEN 1 AND 12"),
                @CheckConstraint(
                        name = "ck_variety_reference_harvest_end_month",
                        constraint = "harvest_end_month BETWEEN 1 AND 12")
        })
@Getter
@Setter
@NoArgsConstructor
public class VarietyReference extends BaseEntity {

    /** Maximum length of a variety key. */
    public static final int KEY_LENGTH = VarietyCatalog.KEY_LENGTH;

    /** Decimals kept of {@code yieldPerTreeKg}, as the API accepts them. */
    public static final int YIELD_SCALE = 1;

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]+");

    /**
     * Variety name as written in the catalogue, for example {@code "Amelie"}. A
     * planted variety finds its row ignoring case, accents and surrounding spaces.
     */
    @Column(name = "variety_name", nullable = false)
    private String varietyName;

    /**
     * Stable code of the variety in the catalogue, for example {@code KEITT}:
     * upper case, without accents, words joined by {@code _}. Derived from the
     * name when the row is first written without one, and never updated.
     */
    @Column(name = "variety_key", nullable = false, updatable = false, length = KEY_LENGTH)
    private String varietyKey;

    /** Yield of one tree in full production, per year, in kg. */
    @Column(name = "yield_per_tree_kg", nullable = false, precision = 5, scale = YIELD_SCALE)
    private BigDecimal yieldPerTreeKg;

    @Column(name = "yield_source", nullable = false)
    private String yieldSource;

    /** First month of the harvest season, from 1 (January) to 12. */
    @Column(name = "harvest_start_month", nullable = false)
    private Integer harvestStartMonth;

    /**
     * Last month of the harvest season, from 1 to 12. It is lower than the start
     * month when the season runs over the new year, for example November to February.
     */
    @Column(name = "harvest_end_month", nullable = false)
    private Integer harvestEndMonth;

    @Column(name = "season_source", nullable = false)
    private String seasonSource;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /**
     * A reference whose key is derived from its name when it is first written
     * (see {@link #keyOf(String)}).
     */
    public VarietyReference(Long id, String varietyName, BigDecimal yieldPerTreeKg, String yieldSource,
                            Integer harvestStartMonth, Integer harvestEndMonth, String seasonSource,
                            Instant lastUpdated) {
        this(id, varietyName, null, yieldPerTreeKg, yieldSource, harvestStartMonth, harvestEndMonth,
                seasonSource, lastUpdated);
    }

    /** A reference with an explicit catalogue key, or {@code null} to derive it from the name. */
    public VarietyReference(Long id, String varietyName, String varietyKey, BigDecimal yieldPerTreeKg,
                            String yieldSource, Integer harvestStartMonth, Integer harvestEndMonth,
                            String seasonSource, Instant lastUpdated) {
        setId(id);
        this.varietyName = varietyName;
        this.varietyKey = varietyKey;
        this.yieldPerTreeKg = yieldPerTreeKg;
        this.yieldSource = yieldSource;
        this.harvestStartMonth = harvestStartMonth;
        this.harvestEndMonth = harvestEndMonth;
        this.seasonSource = seasonSource;
        this.lastUpdated = lastUpdated;
    }

    /** Derives the key from the name when none was given. */
    @PrePersist
    void deriveKey() {
        if (varietyKey == null || varietyKey.isBlank()) {
            varietyKey = keyOf(varietyName);
        }
    }

    /**
     * The catalogue key of a variety name: without accents, in upper case,
     * every run of other characters than letters and digits replaced by
     * {@code _}, for example {@code TOMMY_ATKINS} for {@code " Tommy Atkins "}.
     *
     * @param name a variety name, not {@code null}
     * @return the key, cut to {@link #KEY_LENGTH} characters
     */
    public static String keyOf(String name) {
        String decomposed = Normalizer.normalize(name.strip(), Normalizer.Form.NFD);
        String upper = COMBINING_MARKS.matcher(decomposed).replaceAll("").toUpperCase(Locale.ROOT);
        String key = NOT_ALPHANUMERIC.matcher(upper).replaceAll("_").replaceAll("^_+|_+$", "");
        return key.length() > KEY_LENGTH ? key.substring(0, KEY_LENGTH) : key;
    }
}
