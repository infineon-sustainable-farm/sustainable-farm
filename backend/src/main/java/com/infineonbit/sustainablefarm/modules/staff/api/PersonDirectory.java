package com.infineonbit.sustainablefarm.modules.staff.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongPredicate;

/**
 * The people referential for the other modules (ADR 0003, ADR 0004): a person is entered once
 * and every module references the same {@code person} row.
 *
 * <p>A module keeps the roles of a person (machinery assignment, product transformation
 * operator, visitor guide) in its own table, with a plain {@code Long} column holding the
 * person id and a {@code core.schema.CrossModuleForeignKey} bean toward {@link #TABLE}.
 * A free-text name of a module (an operator name in a log) becomes such a column, resolved by
 * {@link #idOf(String)}: the same name gives the same person.</p>
 *
 * <p>Matching rules: names are trimmed and compared ignoring case, emails likewise; when
 * several people carry the same name, the oldest (lowest id) is used.</p>
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
     * The person carrying this full name, created when nobody does.
     *
     * @throws IllegalArgumentException for a blank name
     */
    PersonDto findOrCreateByName(String fullName);

    /**
     * The person matching these details, created when none matches.
     *
     * <ul>
     *   <li>An email given: the person with this email, if any.</li>
     *   <li>Otherwise the oldest person with the same full name whose email is empty or equal,
     *       and accepted by {@code eligible} (a module refuses, for example, a person who
     *       already holds its role).</li>
     *   <li>Otherwise a new person with these details.</li>
     * </ul>
     * A person found by name receives the email and phone of the details when it has none.
     * A person found by email is returned as it is, even if {@code eligible} refuses it: the
     * caller decides (an email names one person).
     *
     * @throws IllegalArgumentException for a blank name
     */
    PersonDto findOrCreate(PersonDetails details, LongPredicate eligible);

    /**
     * Updates a person. A {@code null} attribute is kept; a blank email or phone clears it
     * (the full name cannot be cleared).
     *
     * @throws com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException for an unknown id
     * @throws com.infineonbit.sustainablefarm.core.exception.ConflictException when the email belongs to another person
     */
    PersonDto update(Long personId, PersonDetails details);

    /** Free-text name to person id: {@code null} for a blank name, else {@link #findOrCreateByName}. */
    default Long idOf(String fullName) {
        return fullName == null || fullName.isBlank() ? null : findOrCreateByName(fullName).id();
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
