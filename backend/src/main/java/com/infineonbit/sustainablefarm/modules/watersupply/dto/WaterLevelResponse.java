package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Niveau d'une source d'eau, lu en temps reel pour l'interface et les autres modules
 * (module 5.2 de la specification : « reservoir level in real time »).
 *
 * <p>La valeur provient de la derniere mesure envoyee par le capteur de niveau ; {@code readAt}
 * est l'instant de la lecture, {@code status} resume la situation par rapport a la capacite.</p>
 */
public record WaterLevelResponse(
        UUID sourceId,
        String name,
        String type,
        Double capacityLiters,
        Double currentLevelLiters,
        Double levelPercentage,
        String status,
        boolean rainwaterTank,
        Instant readAt) {
}
