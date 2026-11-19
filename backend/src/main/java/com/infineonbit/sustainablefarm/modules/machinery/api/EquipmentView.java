package com.infineonbit.sustainablefarm.modules.machinery.api;

import java.time.Instant;

/**
 * A piece of equipment of the farm register, as seen by the other modules.
 *
 * @param id        the Long id of the register, the target of the other modules' foreign keys
 * @param code      the unique readable business code (for example {@code EQ-00012})
 * @param name      the unique name
 * @param category  agricultural machinery, agricultural equipment or processing machinery
 * @param stage     the production stage the equipment serves
 * @param status    operational, under maintenance or out of service
 * @param createdAt when the equipment was registered
 * @param updatedAt when the equipment was last changed
 */
public record EquipmentView(
        Long id,
        String code,
        String name,
        Category category,
        Stage stage,
        Status status,
        Instant createdAt,
        Instant updatedAt) {
}
