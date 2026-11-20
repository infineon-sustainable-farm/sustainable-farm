package com.infineonbit.sustainablefarm.modules.staff.api;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The people referential for the other modules (ADR 0003, ADR 0004): a person is entered once
 * and every module references the same {@code person} row.
 *
 * <p>Ownership (resource ownership audit, 2026-11-19): the people directory is administered by
 * the visitor management Staff screen, the only caller of {@link #create} and {@link #update}
 * (with the technical IoT person of watersupply, created at startup). Every other module only
 * reads: it stores the id of a person picked in a list ({@code GET /api/staff/persons}) in a
 * plain {@code Long} column with a {@code core.schema.CrossModuleForeignKey} bean toward
 * {@link #TABLE}, checks it with {@link #requireExisting}, and shows the name with
 * {@link #nameOf} or {@link #namesOf}. No module creates a person from a typed name.</p>
 */
public interface PersonDirectory {

    /** Table of the referential, referenced on {@link #KEY_COLUMN} by the other modules. */
    String TABLE = "person";

    /** Primary key column of {@link #TABLE}. */
    String KEY_COLUMN = "id";

    Optional<PersonDto> findById(Long personId);

    boolean exists(Long personId);

    /** People of several ids in one lookup, by id; unknown ids are absent. */
    Map<Long, PersonDto> findAllById(Collection<Long> personIds);

    Optional<PersonDto> findByEmail(String email);

    /** Active people, by full name. */
    List<PersonDto> listActive();

    /**
     * Active people whose full name or email contains {@code search} (ignoring case), by full
     * name; every active person for a blank search.
     */
    List<PersonDto> searchActive(String search);

    /**
     * Creates a person. Owner module only (visitor management Staff screen) and the technical
     * IoT person of watersupply.
     *
     * @throws IllegalArgumentException for a blank name
     * @throws com.infineonbit.sustainablefarm.core.exception.ConflictException when the email belongs to another person
     */
    PersonDto create(PersonDetails details);

    /**
     * Updates a person. Owner module only. A {@code null} attribute is kept; a blank email or
     * phone clears it (the full name cannot be cleared).
     *
     * @throws com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException for an unknown id
     * @throws com.infineonbit.sustainablefarm.core.exception.ConflictException when the email belongs to another person
     */
    PersonDto update(Long personId, PersonDetails details);

    /**
     * The id of a person a module references, checked: {@code null} stays {@code null} (no person).
     *
     * @throws BadRequestException for an id that names nobody
     */
    default Long requireExisting(Long personId) {
        if (personId != null && !exists(personId)) {
            throw new BadRequestException("Person " + personId + " not found in the people directory");
        }
        return personId;
    }

    /** Person id to full name: {@code null} for a {@code null} or unknown id. */
    default String nameOf(Long personId) {
        return personId == null ? null : findById(personId).map(PersonDto::fullName).orElse(null);
    }

    /** Full names of several ids in one lookup; unknown ids are absent. */
    default Map<Long, String> namesOf(Collection<Long> personIds) {
        Map<Long, String> names = new java.util.HashMap<>();
        findAllById(personIds).forEach((id, person) -> names.put(id, person.fullName()));
        return names;
    }
}
