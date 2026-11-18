package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.staff.api.InMemoryPersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceImplTest {

    @Mock
    private StaffRepository staffRepository;
    @Spy
    private PersonDirectory personDirectory = new InMemoryPersonDirectory();
    @InjectMocks
    private StaffServiceImpl service;

    private Staff staff;

    @BeforeEach
    void setUp() {
        Long personId = personDirectory.findOrCreate(
                new PersonDetails("Awa Diallo", "awa@farm.io", "+22670010203", true), id -> true).id();
        staff = new Staff();
        staff.setId(1L);
        staff.setPersonId(personId);
        staff.setRole(StaffRole.GUIDE);
    }

    private StaffRequest buildRequest(String fullName, String email) {
        StaffRequest req = new StaffRequest();
        req.setFullName(fullName);
        req.setRole(StaffRole.GUIDE);
        req.setEmail(email);
        req.setPhone("+22670010203");
        return req;
    }

    private void saveAssignsId(long id) {
        when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> {
            Staff s = inv.getArgument(0);
            s.setId(id);
            return s;
        });
    }

    @Test
    void create_success() {
        saveAssignsId(2L);

        StaffResponse resp = service.create(buildRequest("Binta Sow", "binta@farm.io"));

        assertThat(resp.getId()).isEqualTo(2L);
        assertThat(resp.getFullName()).isEqualTo("Binta Sow");
        assertThat(resp.getEmail()).isEqualTo("binta@farm.io");
        assertThat(resp.getRole()).isEqualTo(StaffRole.GUIDE);
        assertThat(resp.isActive()).isTrue();
    }

    @Test
    void create_reusesThePersonOfTheSameName() {
        Long operator = personDirectory.idOf("Issa Ouedraogo");
        saveAssignsId(3L);

        service.create(buildRequest("issa ouedraogo", null));

        verify(staffRepository).save(org.mockito.ArgumentMatchers.argThat(s -> operator.equals(s.getPersonId())));
    }

    @Test
    void create_duplicateEmail_throws() {
        when(staffRepository.findByPersonId(staff.getPersonId())).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.create(buildRequest("Other", "awa@farm.io")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already used");
        verify(staffRepository, never()).save(any());
    }

    @Test
    void list_nonPaged_sortedByName() {
        Staff other = new Staff();
        other.setId(2L);
        other.setPersonId(personDirectory.idOf("Abel"));
        when(staffRepository.findAll()).thenReturn(List.of(staff, other));

        List<StaffResponse> responses = service.list();

        assertThat(responses).extracting(StaffResponse::getFullName).containsExactly("Abel", "Awa Diallo");
        assertThat(responses.get(1).getEmail()).isEqualTo("awa@farm.io");
    }

    @Test
    void list_paged() {
        when(staffRepository.findAll()).thenReturn(List.of(staff));

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
    void update_toAnInactiveState_renamesThePersonButKeepsItActive() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        lenient().when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffRequest req = buildRequest("Awa Diallo Traore", "awa@farm.io");
        req.setActive(false);

        StaffResponse resp = service.update(1L, req);

        assertThat(resp.isActive()).isFalse();
        assertThat(resp.getFullName()).isEqualTo("Awa Diallo Traore");
        assertThat(personDirectory.nameOf(staff.getPersonId())).isEqualTo("Awa Diallo Traore");
        assertThat(personDirectory.findById(staff.getPersonId()).orElseThrow().active()).isTrue();
    }

    @Test
    void deactivate_setsTheGuideInactiveAndLeavesThePersonAlone() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));

        service.deactivate(1L);

        assertThat(staff.isActive()).isFalse();
        assertThat(personDirectory.findById(staff.getPersonId()).orElseThrow().active()).isTrue();
    }
}
