package com.infineonbit.sustainablefarm.modules.producttransformation.variety;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The mango variety of producttransformation, between its API value and the
 * plants catalogue key stored in its tables.
 *
 * <p>The tables store the catalogue key ({@link VarietyCatalog}), with a foreign
 * key to the catalogue. The API keeps the values it had when the variety was an
 * enum of this module: {@code KEITT}, {@code KENT}, {@code TOMMY}, {@code AMÉLIE}
 * and {@code OTHER}. They are the catalogue keys, except {@code AMÉLIE}, whose key
 * is {@code AMELIE}; a variety added to the catalogue since is sent by its key.
 */
@Component
public class MangoVarietyCodes {

    /** Catalogue key of a mango of a variety outside the catalogue. */
    public static final String OTHER = "OTHER";

    /** API values that differ from the catalogue key, by key. */
    private static final Map<String, String> API_VALUE_BY_KEY = Map.of("AMELIE", "AMÉLIE");

    private final VarietyCatalog varietyCatalog;

    public MangoVarietyCodes(VarietyCatalog varietyCatalog) {
        this.varietyCatalog = varietyCatalog;
    }

    /**
     * The catalogue key of a variety sent to the API.
     *
     * @param apiValue the value sent, for example {@code KENT} or {@code AMÉLIE};
     *                 a catalogue name such as {@code Kent} is accepted too
     * @return the catalogue key, or {@code null} when the value is {@code null}
     * @throws BadRequestException if the catalogue has no such variety
     */
    public String toKey(String apiValue) {
        if (apiValue == null) {
            return null;
        }
        return varietyCatalog.resolve(apiValue)
                .map(CatalogVariety::key)
                .orElseThrow(() -> new BadRequestException("Unknown mango variety: " + apiValue));
    }

    /**
     * The API value of a stored catalogue key.
     *
     * @param key the catalogue key, possibly {@code null}
     * @return the API value, {@code AMÉLIE} for {@code AMELIE}, the key otherwise
     */
    public static String toApi(String key) {
        return key == null ? null : API_VALUE_BY_KEY.getOrDefault(key, key);
    }

    /**
     * The catalogue key of a variety name read from plants, for example
     * {@code "Keitt"}, {@code "keitt"} or {@code "Amélie"}: unknown or missing names
     * give {@link #OTHER} instead of failing the whole harvest import.
     *
     * @param plantsName the planted name, possibly {@code null}
     * @return the catalogue key
     */
    public String keyOfPlantsName(String plantsName) {
        return varietyCatalog.resolve(plantsName).map(CatalogVariety::key).orElse(OTHER);
    }
}
