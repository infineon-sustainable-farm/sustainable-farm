package com.infineonbit.sustainablefarm.modules.staff.api;

import java.time.Instant;

/** Read view of a person of the staff referential. */
public record PersonDto(Long id, String fullName, String email, String phone, boolean active,
                        Instant createdAt, Instant updatedAt) {
}
