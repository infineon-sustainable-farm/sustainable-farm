package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Collecte d'eau de pluie envoyee par un client (module 5.1).
 *
 * <p>Le client ne fournit ni identifiant ni date de creation : ils appartiennent au serveur.
 * Le volume est calcule par le backend ({@code surface x pluie x coefficient de ruissellement})
 * quand il n'est pas fourni.</p>
 */
public record RainwaterHarvestRequest(
        UUID sourceId,
        Double catchmentAreaM2,
        Double rainfallMm,
        Double runoffCoefficient,
        Double harvestedLiters,
        Instant captureDate) {
}
