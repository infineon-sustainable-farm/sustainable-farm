package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import java.time.Instant;
import java.util.UUID;

/** Vue API d'une collecte d'eau de pluie : contrat stable, independant de l'entite JPA. */
public record RainwaterHarvestResponse(
        UUID id,
        UUID sourceId,
        Double catchmentAreaM2,
        Double rainfallMm,
        Double runoffCoefficient,
        Double harvestedLiters,
        Instant captureDate,
        Instant createdAt) {

    public static RainwaterHarvestResponse from(RainwaterHarvest harvest) {
        return new RainwaterHarvestResponse(harvest.getId(), harvest.getSourceId(), harvest.getCatchmentAreaM2(),
                harvest.getRainfallMm(), harvest.getRunoffCoefficient(), harvest.getHarvestedLiters(),
                harvest.getCaptureDate(), harvest.getCreatedAt());
    }
}
