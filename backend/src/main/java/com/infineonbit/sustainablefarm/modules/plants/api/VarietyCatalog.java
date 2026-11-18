package com.infineonbit.sustainablefarm.modules.plants.api;

import java.util.List;
import java.util.Optional;

/**
 * The variety catalogue, owned by plants (docs/adr/0004-referentiels-partages.md).
 *
 * <p>Another module that names a variety stores its {@link CatalogVariety#key() key}
 * in a column with a foreign key to {@link #TABLE}({@link #KEY_COLUMN}), and reads
 * the catalogue through this interface only.
 */
public interface VarietyCatalog {

    /** Table of the catalogue, the target of the foreign keys of the other modules. */
    String TABLE = "variety_reference";

    /** Unique column of {@link #TABLE} that the foreign keys of the other modules reference. */
    String KEY_COLUMN = "variety_key";

    /** Maximum length of a key, for the columns that store one. */
    int KEY_LENGTH = 40;

    /**
     * Order of the loader that fills the catalogue at startup, in every
     * profile. A seeder of another module that references the catalogue runs
     * after it (a higher {@code @Order} value, or none).
     */
    int LOAD_ORDER = 0;

    /**
     * Lists the catalogue.
     *
     * @return every variety, ordered by name ignoring case and accents; possibly empty
     */
    List<CatalogVariety> listVarieties();

    /**
     * Finds a variety by its exact key.
     *
     * @param key the key, for example {@code KENT}
     * @return the variety, or empty when no variety has this key or the key is {@code null}
     */
    Optional<CatalogVariety> findByKey(String key);

    /**
     * Finds a variety by its name, ignoring case, accents and surrounding spaces.
     *
     * @param name the name, for example {@code "amélie"}
     * @return the variety, or empty when none has this name or the name is {@code null}
     */
    Optional<CatalogVariety> findByName(String name);

    /**
     * Resolves a free variety name, as typed or received from another system,
     * to a catalogue entry: it matches a key or a name, ignoring case, accents,
     * surrounding spaces, and the difference between spaces, hyphens and
     * underscores. {@code "keitt"}, {@code "Amélie"}, {@code "AMÉLIE"} and
     * {@code "tommy atkins"} are all resolved.
     *
     * @param freeName the name, possibly {@code null}
     * @return the variety, or empty when nothing matches
     */
    Optional<CatalogVariety> resolve(String freeName);
}
