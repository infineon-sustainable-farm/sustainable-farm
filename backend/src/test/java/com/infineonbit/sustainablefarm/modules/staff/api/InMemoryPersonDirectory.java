package com.infineonbit.sustainablefarm.modules.staff.api;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link PersonDirectory} kept in memory, for the unit tests of the modules that reference
 * people; {@link #add} registers a person as the owner module would.
 */
public class InMemoryPersonDirectory implements PersonDirectory {

    private final Map<Long, PersonDto> people = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Optional<PersonDto> findById(Long personId) {
        return Optional.ofNullable(personId == null ? null : people.get(personId));
    }

    @Override
    public boolean exists(Long personId) {
        return personId != null && people.containsKey(personId);
    }

    @Override
    public Map<Long, PersonDto> findAllById(Collection<Long> personIds) {
        Map<Long, PersonDto> found = new LinkedHashMap<>();
        personIds.stream().filter(Objects::nonNull).filter(people::containsKey)
                .forEach(id -> found.put(id, people.get(id)));
        return found;
    }

    @Override
    public Optional<PersonDto> findByEmail(String email) {
        return email == null || email.isBlank() ? Optional.empty()
                : people.values().stream().filter(p -> email.trim().equalsIgnoreCase(p.email())).findFirst();
    }

    @Override
    public List<PersonDto> listActive() {
        return people.values().stream().filter(PersonDto::active)
                .sorted(java.util.Comparator.comparing(PersonDto::fullName)).toList();
    }

    @Override
    public List<PersonDto> searchActive(String search) {
        String term = blankToNull(search);
        return listActive().stream().filter(p -> term == null
                || p.fullName().toLowerCase().contains(term.toLowerCase())
                || (p.email() != null && p.email().toLowerCase().contains(term.toLowerCase()))).toList();
    }

    @Override
    public PersonDto create(PersonDetails details) {
        String name = blankToNull(details.fullName());
        if (name == null) {
            throw new IllegalArgumentException("A person needs a full name");
        }
        String email = blankToNull(details.email());
        if (findByEmail(email).isPresent()) {
            throw new com.infineonbit.sustainablefarm.core.exception.ConflictException("Email already used by another person");
        }
        Instant now = Instant.now();
        PersonDto person = new PersonDto(nextId++, name, email, blankToNull(details.phone()),
                details.active() == null || details.active(), now, now);
        people.put(person.id(), person);
        return person;
    }

    /**
     * Test helper: the id of the person carrying this name, registered on first use as the owner
     * module would (the real directory never does this); {@code null} for a blank name.
     */
    public Long idOf(String fullName) {
        String name = blankToNull(fullName);
        if (name == null) {
            return null;
        }
        return people.values().stream().filter(p -> p.fullName().equalsIgnoreCase(name)).findFirst()
                .map(PersonDto::id).orElseGet(() -> add(name).id());
    }

    /** Test helper: an active person with this name and no email nor phone. */
    public PersonDto add(String fullName) {
        return create(new PersonDetails(fullName, null, null, null));
    }

    @Override
    public PersonDto update(Long personId, PersonDetails details) {
        PersonDto current = findById(personId).orElseThrow(
                () -> new com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException("Person " + personId + " not found"));
        if (details.email() != null) {
            findByEmail(details.email()).filter(other -> !other.id().equals(personId)).ifPresent(other -> {
                throw new com.infineonbit.sustainablefarm.core.exception.ConflictException("Email already used by another person");
            });
        }
        PersonDto updated = new PersonDto(personId,
                details.fullName() != null ? details.fullName().trim() : current.fullName(),
                details.email() != null ? blankToNull(details.email()) : current.email(),
                details.phone() != null ? blankToNull(details.phone()) : current.phone(),
                details.active() != null ? details.active() : current.active(),
                current.createdAt(), Instant.now());
        people.put(personId, updated);
        return updated;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
