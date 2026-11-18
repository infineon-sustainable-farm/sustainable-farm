package com.infineonbit.sustainablefarm.modules.staff.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDto;
import com.infineonbit.sustainablefarm.modules.staff.entity.Person;
import com.infineonbit.sustainablefarm.modules.staff.repository.PersonRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongPredicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link PersonDirectory} implementation over the {@code person} table. */
@Service
@Transactional
public class PersonDirectoryService implements PersonDirectory {

    private final PersonRepository personRepository;

    public PersonDirectoryService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonDto> findById(Long personId) {
        return personId == null ? Optional.empty() : personRepository.findById(personId).map(PersonDirectoryService::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long personId) {
        return personId != null && personRepository.existsById(personId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, PersonDto> findAllById(Collection<Long> personIds) {
        List<Long> ids = personIds == null ? List.of()
                : personIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, PersonDto> people = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            personRepository.findAllById(ids).forEach(person -> people.put(person.getId(), toDto(person)));
        }
        return people;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonDto> findByEmail(String email) {
        String normalized = normalize(email);
        return normalized == null ? Optional.empty()
                : personRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(normalized).map(PersonDirectoryService::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonDto> listActive() {
        return personRepository.findAllByActiveTrueOrderByFullNameAscIdAsc().stream()
                .map(PersonDirectoryService::toDto).toList();
    }

    @Override
    public PersonDto findOrCreateByName(String fullName) {
        return findOrCreate(PersonDetails.named(fullName), id -> true);
    }

    @Override
    public PersonDto findOrCreate(PersonDetails details, LongPredicate eligible) {
        String fullName = requireName(details.fullName());
        String email = normalize(details.email());
        String phone = normalize(details.phone());
        if (email != null) {
            Optional<Person> byEmail = personRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email);
            if (byEmail.isPresent()) {
                return toDto(byEmail.get());
            }
        }
        for (Person candidate : personRepository.findAllByFullNameIgnoreCaseOrderByIdAsc(fullName)) {
            boolean sameEmail = candidate.getEmail() == null || email == null || candidate.getEmail().equalsIgnoreCase(email);
            if (sameEmail && eligible.test(candidate.getId())) {
                if (candidate.getEmail() == null && email != null) {
                    candidate.setEmail(email);
                }
                if (candidate.getPhone() == null && phone != null) {
                    candidate.setPhone(phone);
                }
                return toDto(personRepository.save(candidate));
            }
        }
        Person person = new Person();
        person.setFullName(fullName);
        person.setEmail(email);
        person.setPhone(phone);
        person.setActive(details.active() == null || details.active());
        return toDto(personRepository.save(person));
    }

    @Override
    public PersonDto update(Long personId, PersonDetails details) {
        Person person = personId == null ? null : personRepository.findById(personId).orElse(null);
        if (person == null) {
            throw new ResourceNotFoundException("Person " + personId + " not found");
        }
        if (details.fullName() != null) {
            person.setFullName(requireName(details.fullName()));
        }
        if (details.email() != null) {
            String email = normalize(details.email());
            if (email != null) {
                personRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email)
                        .filter(other -> !other.getId().equals(personId))
                        .ifPresent(other -> {
                            throw new ConflictException("Email already used by another person");
                        });
            }
            person.setEmail(email);
        }
        if (details.phone() != null) {
            person.setPhone(normalize(details.phone()));
        }
        if (details.active() != null) {
            person.setActive(details.active());
        }
        return toDto(personRepository.saveAndFlush(person));
    }

    private static String requireName(String fullName) {
        String name = normalize(fullName);
        if (name == null) {
            throw new IllegalArgumentException("A person needs a full name");
        }
        return name;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static PersonDto toDto(Person person) {
        return new PersonDto(person.getId(), person.getFullName(), person.getEmail(), person.getPhone(),
                person.isActive(), person.getCreatedAt(), person.getUpdatedAt());
    }
}
