package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Staff;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;

    public StaffServiceImpl(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> list() {
        return staffRepository.findAllByOrderByFullNameAsc().stream()
                .map(StaffResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffResponse> list(int page, int size) {
        return staffRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fullName")))
                .map(StaffResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse get(Long id) {
        return StaffResponse.from(getEntity(id));
    }

    @Override
    @Transactional
    public StaffResponse create(StaffRequest request) {
        assertEmailFree(request.getEmail(), null);
        Staff staff = new Staff();
        apply(staff, request);
        return StaffResponse.from(staffRepository.save(staff));
    }

    @Override
    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = getEntity(id);
        assertEmailFree(request.getEmail(), id);
        apply(staff, request);
        staffRepository.save(staff);
        staffRepository.flush();
        return StaffResponse.from(staff);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Staff staff = getEntity(id);
        staff.setActive(false);
        staffRepository.save(staff);
    }

    private void apply(Staff staff, StaffRequest request) {
        staff.setFullName(request.getFullName());
        staff.setRole(request.getRole());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        if (request.getActive() != null) {
            staff.setActive(request.getActive());
        }
    }

    private void assertEmailFree(String email, Long excludeId) {
        if (email == null || email.isBlank()) {
            return;
        }
        staffRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new ConflictException("Email already used by staff member " + existing.getId());
                });
    }

    private Staff getEntity(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member " + id + " not found"));
    }
}