package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Staff;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.StaffRole;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceImplTest {

    @Mock
    private StaffRepository staffRepository;
    @InjectMocks
    private StaffServiceImpl service;

    private Staff staff;

    @BeforeEach
    void setUp() {
        staff = new Staff();
        staff.setId(1L);
        staff.setFullName("Awa Diallo");
        staff.setRole(StaffRole.GUIDE);
        staff.setEmail("awa@farm.io");
        staff.setPhone("+22670010203");
        staff.setActive(true);
    }

    private StaffRequest buildRequest() {
        StaffRequest req = new StaffRequest();
        req.setFullName("Awa Diallo");
        req.setRole(StaffRole.GUIDE);
        req.setEmail("awa@farm.io");
        req.setPhone("+22670010203");
        return req;
    }

    @Test
    void create_success() {
        when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> {
            Staff s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        StaffResponse resp = service.create(buildRequest());

        assertThat(resp.getFullName()).isEqualTo("Awa Diallo");
        assertThat(resp.getRole()).isEqualTo(StaffRole.GUIDE);
        assertThat(resp.isActive()).isTrue();
    }

    @Test
    void create_duplicateEmail_throws() {
        Staff other = new Staff();
        other.setId(2L);
        other.setFullName("Other");
        when(staffRepository.findByEmail("awa@farm.io")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void list_nonPaged() {
        when(staffRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of(staff));

        List<StaffResponse> responses = service.list();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFullName()).isEqualTo("Awa Diallo");
    }

    @Test
    void list_paged() {
        when(staffRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(staff)));

        Page<StaffResponse> page = service.list(0, 20);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void get_notFound_throws() {
        when(staffRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void update_toAnActiveState() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(staffRepository.findByEmail("awa@farm.io")).thenReturn(Optional.empty());
        when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffRequest req = buildRequest();
        req.setActive(false);

        StaffResponse resp = service.update(1L, req);

        assertThat(resp.isActive()).isFalse();
    }

    @Test
    void deactivate_setsInactive() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate(1L);

        assertThat(staff.isActive()).isFalse();
        verify(staffRepository, never()).findByEmail(any());
    }
}