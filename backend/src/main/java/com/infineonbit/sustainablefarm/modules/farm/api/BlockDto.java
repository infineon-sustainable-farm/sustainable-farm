package com.infineonbit.sustainablefarm.modules.farm.api;

import java.time.Instant;

/**
 * Read view of a block of a farm (a "field" in Water Supply).
 *
 * @param code Plants block code in upper case ("A", "B2"), or {@code null} while the block is not linked to Plants
 */
public record BlockDto(Long id, Long farmId, String code, String name, Double areaHectares,
                       String cropType, String soilType, String coordinates, Instant createdAt) {
}
