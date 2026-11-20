package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Loads at startup, in every profile, the varieties of the catalogue
 * ({@link VarietyCatalog}) that producttransformation and salesmarketing
 * reference by key: {@code KEITT}, {@code KENT}, {@code AMELIE}, {@code TOMMY}
 * and the catch-all {@code OTHER}, which producttransformation records for a
 * mango of a variety outside the catalogue. Their tables have a foreign key to
 * the catalogue, so without these rows they could record no variety.
 *
 * <p>Each row is also the agronomic reference of its variety: the yield per
 * tree and the harvest season, with their sources, read by the yield forecast.
 * The values of Tommy Atkins and Other are placeholders to validate, flagged by
 * their source {@value #ASSUMPTION}.
 *
 * <p>This is reference data, not demo data: product transformation and sales
 * reference these keys in every profile, production included. It is therefore
 * loaded when the application has started ({@link ApplicationStartedEvent}),
 * before every startup runner, so that the dev demo seed finds the keys it
 * references. It is not a dev-only seeder. A variety is
 * inserted only when its name is missing, ignoring case, accents and
 * surrounding spaces (see {@link VarietyReferenceMatcher}), and its key too. An
 * existing row is never overwritten, even when its value differs from the
 * default below: a value corrected since is the one that counts. Restarting
 * the application therefore never duplicates nor resets a row.
 */
@Component
public class VarietyCatalogLoader {

    /** Default yield and harvest season of a variety, with their sources. */
    private record VarietyDefault(String varietyName, String varietyKey, double yieldPerTreeKg, String yieldSource,
                                  int harvestStartMonth, int harvestEndMonth, String seasonSource) {
    }

    /** Source of a placeholder value, to replace by a sourced one. */
    static final String ASSUMPTION = "assumption_to_validate";

    private static final List<VarietyDefault> VARIETY_DEFAULTS = List.of(
            new VarietyDefault("Keitt", "KEITT", 220.0, "Zalka_2025", 5, 7, "varietal_guide_west_africa"),
            new VarietyDefault("Kent", "KENT", 200.0, "Zalka_2025", 4, 5, "FAO_mango_burkina"),
            new VarietyDefault("Amelie", "AMELIE", 160.0, "Zalka_2025", 2, 4, "FAO_mango_burkina"),
            new VarietyDefault("Tommy Atkins", "TOMMY", 180.0, ASSUMPTION, 4, 6, ASSUMPTION),
            new VarietyDefault("Other", "OTHER", 160.0, ASSUMPTION, 4, 6, ASSUMPTION));

    private final VarietyReferenceRepository varietyReferenceRepository;

    public VarietyCatalogLoader(VarietyReferenceRepository varietyReferenceRepository) {
        this.varietyReferenceRepository = varietyReferenceRepository;
    }

    /** Loads the missing varieties once the application has started. */
    @EventListener(ApplicationStartedEvent.class)
    @Order(VarietyCatalog.LOAD_ORDER)
    @Transactional
    public void loadOnStartup() {
        load(Instant.now());
    }

    /**
     * Inserts the missing varieties, at an explicit write time so the
     * {@code lastUpdated} values can be tested.
     *
     * @param now insertion time, stored as {@code lastUpdated} of each inserted row
     */
    void load(Instant now) {
        List<VarietyReference> references = varietyReferenceRepository.findAll();
        for (VarietyDefault variety : VARIETY_DEFAULTS) {
            boolean present = VarietyReferenceMatcher.find(variety.varietyName(), references).isPresent()
                    || references.stream().anyMatch(reference -> variety.varietyKey().equals(reference.getVarietyKey()));
            if (!present) {
                varietyReferenceRepository.save(new VarietyReference(
                        null,
                        variety.varietyName(),
                        variety.varietyKey(),
                        Decimals.of(variety.yieldPerTreeKg(), VarietyReference.YIELD_SCALE),
                        variety.yieldSource(),
                        variety.harvestStartMonth(),
                        variety.harvestEndMonth(),
                        variety.seasonSource(),
                        now));
            }
        }
    }
}
