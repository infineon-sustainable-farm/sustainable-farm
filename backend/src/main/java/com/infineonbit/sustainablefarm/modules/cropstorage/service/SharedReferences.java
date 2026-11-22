package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.api.HarvestLookup;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The checks Crop Storage makes on concepts owned by other modules, through their api
 * packages only (ADR 0003): a variety of the Plants catalogue, a Plants harvest, a person
 * of the staff directory. An unknown reference is a 400, before the foreign key would
 * refuse the write.
 */
@Component("cropStorageSharedReferences")
@RequiredArgsConstructor
public class SharedReferences {

    private final VarietyCatalog varietyCatalog;
    private final HarvestLookup harvestLookup;
    private final PersonDirectory personDirectory;

    /** The catalogue key of a variety name, {@code null} for no variety. */
    public String varietyKeyOf(String varietyName) {
        if (varietyName == null || varietyName.isBlank()) {
            return null;
        }
        return varietyCatalog.resolve(varietyName)
                .map(CatalogVariety::key)
                .orElseThrow(() -> new BadRequestException("Unknown variety: " + varietyName));
    }

    /** The harvest id itself once Plants knows it, {@code null} for no harvest. */
    public Long requireHarvest(Long harvestId) {
        if (harvestId != null && harvestLookup.findHarvest(harvestId).isEmpty()) {
            throw new BadRequestException("Unknown Plants harvest: " + harvestId);
        }
        return harvestId;
    }

    /** The person id itself once the staff directory knows it, {@code null} for nobody. */
    public Long requirePerson(Long personId) {
        return personDirectory.requireExisting(personId);
    }
}
