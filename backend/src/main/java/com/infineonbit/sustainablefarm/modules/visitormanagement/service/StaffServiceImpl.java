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
 * The farm people directory (Staff screen): the only place that creates, edits and deactivates
 * the people of the staff referential ({@link PersonDirectory}), each with an optional visitor
 * management role (guide, reception, manager). The other modules pick these people by id.
 * Deactivating an entry deactivates the person; the roles other modules give it (PT operator
 * status, machinery assignment) keep their own state.
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
        boolean active = request.getActive() == null || request.getActive();
        PersonDto person;
        try {
            person = personDirectory.create(new PersonDetails(request.getFullName(), request.getEmail(),
                    request.getPhone(), active));
        } catch (ConflictException e) {
            throw emailTaken(request.getEmail());
        }
        Staff staff = new Staff();
        staff.setPersonId(person.id());
        staff.setRole(request.getRole());
        staff.setActive(active);
        return StaffResponse.from(staffRepository.save(staff), person);
    }

    /**
     * Updates the person and the role. An email or phone left out of the request is kept; sent
     * empty, it is cleared (the Staff form sends the full record).
     */
    @Override
    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = getEntity(id);
        PersonDto person;
        try {
            person = personDirectory.update(staff.getPersonId(), new PersonDetails(request.getFullName(),
                    sentValue(request.isEmailSent(), request.getEmail()),
                    sentValue(request.isPhoneSent(), request.getPhone()),
                    request.getActive()));
        } catch (ConflictException e) {
            throw emailTaken(request.getEmail());
        }
        staff.setRole(request.getRole());
        if (request.getActive() != null) {
            staff.setActive(request.getActive());
        }
        staffRepository.save(staff);
        staffRepository.flush();
        return StaffResponse.from(staff, person);
    }

    /** Deactivates the person in the people directory: the other modules no longer list it. */
    @Override
    @Transactional
    public void deactivate(Long id) {
        Staff staff = getEntity(id);
        staff.setActive(false);
        staffRepository.save(staff);
        personDirectory.update(staff.getPersonId(), new PersonDetails(null, null, null, false));
    }

    /** {@code null} (keep) for a key the request left out, {@code ""} (clear) for a key sent empty. */
    private static String sentValue(boolean sent, String value) {
        if (!sent) {
            return null;
        }
        return value == null ? "" : value;
    }

    private ConflictException emailTaken(String email) {
        return new ConflictException(staffRepository.findByPersonId(
                        personDirectory.findByEmail(email).map(PersonDto::id).orElse(null))
                .map(existing -> "Email already used by staff member " + existing.getId())
                .orElse("Email already used by another person"));
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
