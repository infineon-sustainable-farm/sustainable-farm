package com.infineonbit.sustainablefarm.modules.plants.api;

/**
 * A variety of the catalogue, as the other modules see it.
 *
 * @param key  stable code of the variety, for example {@code KEITT}; the other
 *             modules store it as a foreign key to {@link VarietyCatalog#TABLE}
 * @param name name of the variety as written in the catalogue, for example {@code "Keitt"}
 */
public record CatalogVariety(String key, String name) {
}
