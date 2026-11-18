package com.infineonbit.sustainablefarm.modules.farm.api;

import java.time.Instant;

/** Read view of a farm. */
public record FarmDto(Long id, String name, String description, String address,
                      Double latitude, Double longitude, Double areaHectares, Instant createdAt) {
}
