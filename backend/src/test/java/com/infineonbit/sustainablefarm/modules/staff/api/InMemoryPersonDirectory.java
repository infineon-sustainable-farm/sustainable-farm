package com.infineonbit.sustainablefarm.modules.staff.api;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongPredicate;

/**
 * A {@link PersonDirectory} kept in memory, for the unit tests of the modules that reference
 * people: the same name gives the same id, as with the real referential.
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
    public PersonDto findOrCreateByName(String fullName) {
        return findOrCreate(PersonDetails.named(fullName), id -> true);
    }

    @Override
    public PersonDto findOrCreate(PersonDetails details, LongPredicate eligible) {
        String name = blankToNull(details.fullName());
        if (name == null) {
            throw new IllegalArgumentException("A person needs a full name");
        }
        String email = blankToNull(details.email());
        Optional<PersonDto> byEmail = findByEmail(email);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }
        for (PersonDto candidate : people.values()) {
            boolean sameEmail = candidate.email() == null || email == null || candidate.email().equalsIgnoreCase(email);
            if (candidate.fullName().equalsIgnoreCase(name) && sameEmail && eligible.test(candidate.id())) {
                PersonDto merged = new PersonDto(candidate.id(), candidate.fullName(),
                        candidate.email() != null ? candidate.email() : email,
                        candidate.phone() != null ? candidate.phone() : blankToNull(details.phone()),
                        candidate.active(), candidate.createdAt(), Instant.now());
                people.put(merged.id(), merged);
                return merged;
            }
        }
        Instant now = Instant.now();
        PersonDto person = new PersonDto(nextId++, name, email, blankToNull(details.phone()),
                details.active() == null || details.active(), now, now);
        people.put(person.id(), person);
        return person;
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
