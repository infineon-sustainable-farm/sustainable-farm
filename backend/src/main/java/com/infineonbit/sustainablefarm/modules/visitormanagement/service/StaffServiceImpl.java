package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDto;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Staff;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Staff members and guides: the visitor management role of a person of the staff
 * referential ({@link PersonDirectory}). Full name, email, phone and active status are read
 * from and written to the person, so a guide named like a machinery or product transformation
 * operator is the same person.
 */
@Service
public class StaffServiceImpl implements StaffService {

    private static final Comparator<StaffResponse> BY_NAME = Comparator
            .comparing(StaffResponse::getFullName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(StaffResponse::getId);

    private final StaffRepository staffRepository;
    private final PersonDirectory personDirectory;

    public StaffServiceImpl(StaffRepository staffRepository, PersonDirectory personDirectory) {
        this.staffRepository = staffRepository;
        this.personDirectory = personDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> list() {
        List<Staff> staff = staffRepository.findAll();
        Map<Long, PersonDto> people = personDirectory.findAllById(staff.stream().map(Staff::getPersonId).toList());
        return staff.stream()
                .filter(s -> people.containsKey(s.getPersonId()))
                .map(s -> StaffResponse.from(s, people.get(s.getPersonId())))
                .sorted(BY_NAME)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffResponse> list(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        List<StaffResponse> all = list();
        int from = (int) Math.min(pageable.getOffset(), all.size());
        int to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse get(Long id) {
        Staff staff = getEntity(id);
        return StaffResponse.from(staff, person(staff));
    }

    @Override
    @Transactional
    public StaffResponse create(StaffRequest request) {
        PersonDto person = personDirectory.findOrCreate(
                new PersonDetails(request.getFullName(), request.getEmail(), request.getPhone(), request.getActive()),
                personId -> !staffRepository.existsByPersonId(personId));
        staffRepository.findByPersonId(person.id()).ifPresent(existing -> {
            throw new ConflictException("Email already used by staff member " + existing.getId());
        });
        boolean activeChanges = request.getActive() != null && request.getActive() != person.active();
        boolean phoneMissing = person.phone() == null && request.getPhone() != null && !request.getPhone().isBlank();
        if (activeChanges || phoneMissing) {
            person = personDirectory.update(person.id(),
                    new PersonDetails(null, null, phoneMissing ? request.getPhone() : null, request.getActive()));
        }
        Staff staff = new Staff();
        staff.setPersonId(person.id());
        staff.setRole(request.getRole());
        return StaffResponse.from(staffRepository.save(staff), person);
    }

    @Override
    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = getEntity(id);
        PersonDto person;
        try {
            person = personDirectory.update(staff.getPersonId(), new PersonDetails(request.getFullName(),
                    request.getEmail() == null ? "" : request.getEmail(),
                    request.getPhone() == null ? "" : request.getPhone(),
                    request.getActive()));
        } catch (ConflictException e) {
            throw new ConflictException(staffRepository.findByPersonId(
                            personDirectory.findByEmail(request.getEmail()).map(PersonDto::id).orElse(null))
                    .map(existing -> "Email already used by staff member " + existing.getId())
                    .orElse("Email already used by another person"));
        }
        staff.setRole(request.getRole());
        staffRepository.save(staff);
        staffRepository.flush();
        return StaffResponse.from(staff, person);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Staff staff = getEntity(id);
        personDirectory.update(staff.getPersonId(), new PersonDetails(null, null, null, false));
    }

    private PersonDto person(Staff staff) {
        return personDirectory.findById(staff.getPersonId())
                .orElseThrow(() -> new ResourceNotFoundException("Person " + staff.getPersonId() + " not found"));
    }

    private Staff getEntity(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member " + id + " not found"));
    }
}
