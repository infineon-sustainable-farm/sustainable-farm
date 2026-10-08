package com.infineonbit.sustainablefarm.modules.watersupply.config;

import java.util.UUID;

/**
 * Technical identities of the watersupply module.
 *
 * <p>Authentication is delegated to the global software: the module therefore has no logged-in
 * user. Two needs are still covered by a fixed id (row created at startup by
 * {@code SystemUserSeeder}, with no login credentials, hence no way to log in):</p>
 * <ul>
 *   <li>{@code irrigation_schedules.created_by}: the column is mandatory;</li>
 *   <li>{@code notifications.user_id}: the column is mandatory, alerts coming from the
 *       IoT sensors (leak, out-of-range quality, rain postponement) must have a
 *       recipient, which the global platform can reassign afterwards.</li>
 * </ul>
 */
public final class SystemUsers {

    /** Technical "IoT system" account - never used to log in. */
    public static final UUID IOT_SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private SystemUsers() {
    }
}
