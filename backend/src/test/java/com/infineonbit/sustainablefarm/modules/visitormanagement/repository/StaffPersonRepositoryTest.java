package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.repository.PersonRepository;
import com.infineonbit.sustainablefarm.modules.staff.service.PersonDirectoryService;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.StaffRole;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.StaffServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** A guide is the visitor management role of a person of the staff referential (ADR 0004). */
@DataJpaTest
@ActiveProfiles("test")
@Import({PersonDirectoryService.class, StaffServiceImpl.class})
class StaffPersonRepositoryTest {

    @Autowired
    private StaffServiceImpl staffService;
    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private PersonDirectory personDirectory;
    @Autowired
    private PersonRepository personRepository;

    @Test
    void aNewEntryEntersItsOwnPersonRow() {
        StaffRequest request = new StaffRequest();
        request.setFullName("Awa Traore");
        request.setRole(StaffRole.GUIDE);
        request.setPhone("+22670010203");
        StaffResponse guide = staffService.create(request);

        Long personId = staffRepository.findById(guide.getId()).orElseThrow().getPersonId();
        assertThat(personDirectory.nameOf(personId)).isEqualTo("Awa Traore");
        assertThat(personRepository.count()).isEqualTo(1);
        assertThat(guide.getPhone()).isEqualTo("+22670010203");
    }

    @Test
    void twoGuidesWithTheSameNameAndNoEmailAreTwoPeople() {
        StaffRequest request = new StaffRequest();
        request.setFullName("Moussa");
        request.setRole(StaffRole.GUIDE);
        StaffResponse first = staffService.create(request);
        StaffResponse second = staffService.create(request);

        assertThat(staffRepository.findById(first.getId()).orElseThrow().getPersonId())
                .isNotEqualTo(staffRepository.findById(second.getId()).orElseThrow().getPersonId());
    }
}
