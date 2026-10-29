package com.infineonbit.sustainablefarm.modules.watersupply.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Rainwater harvest sent by a client (module 5.1).
 *
 * <p>The client provides neither an id nor a creation date: they belong to the server.
 * The volume is computed by the backend ({@code area x rainfall x runoff coefficient})
 * when it is not provided.</p>
 */
public record RainwaterHarvestRequest(
        UUID sourceId,
        Double catchmentAreaM2,
        Double rainfallMm,
        Double runoffCoefficient,
        Double harvestedLiters,
        Instant captureDate) {
}
