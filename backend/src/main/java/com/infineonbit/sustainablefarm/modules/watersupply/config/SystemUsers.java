package com.infineonbit.sustainablefarm.modules.watersupply.config;

import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.stereotype.Component;

/**
 * Technical identities of the watersupply module.
 *
 * <p>Authentication is delegated to the global software: the module therefore has no logged-in
 * user. Two needs are still covered by a well-known person of the staff referential, the
 * "IoT system" person (created at startup by {@code SystemUserSeeder}, inactive, with no login
 * credentials, hence no way to log in):</p>
 * <ul>
 *   <li>{@code irrigation_schedules.created_by}: the column is mandatory;</li>
 *   <li>{@code alert.recipient_person_id} of the module alerts (notification module): alerts coming from the
 *       IoT sensors (leak, out-of-range quality, rain postponement) must have a
 *       recipient, which the global platform can reassign afterwards.</li>
 * </ul>
 *
 * <p>The person is identified by its email {@link #IOT_SYSTEM_EMAIL}; its id, a
 * {@code person.id}, is looked up once and kept.</p>
 */
@Component
public class SystemUsers {

    /** Email of the technical "IoT system" person - never used to log in. */
    public static final String IOT_SYSTEM_EMAIL = "systeme.iot@watersupply.local";

    /** Full name of the technical "IoT system" person. */
    public static final String IOT_SYSTEM_NAME = "Systeme IoT";

    private final PersonDirectory personDirectory;
    private volatile Long iotSystemUserId;

    public SystemUsers(PersonDirectory personDirectory) {
        this.personDirectory = personDirectory;
    }

    /** Person id of the technical IoT account, created in the staff referential when missing. */
    public Long iotSystemUserId() {
        Long id = iotSystemUserId;
        if (id == null || !personDirectory.exists(id)) {
            id = personDirectory.findByEmail(IOT_SYSTEM_EMAIL)
                    .orElseGet(() -> personDirectory.findOrCreate(
                            new PersonDetails(IOT_SYSTEM_NAME, IOT_SYSTEM_EMAIL, null, false), candidate -> true))
                    .id();
            iotSystemUserId = id;
        }
        return id;
    }

    /** Whether a person id is the technical IoT account. */
    public boolean isIotSystemUser(Long personId) {
        return personId != null && personId.equals(iotSystemUserId());
    }
}
