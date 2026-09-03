package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RegistrationRepositoryTest {

    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private VisitorRepository visitorRepository;
    @Autowired
    private RegistrationRepository repository;

    private TimeSlot slot;
    private Visitor visitor;

    @BeforeEach
    void setUp() {
        slot = new TimeSlot();
        slot.setDate(LocalDate.of(2026, 9, 8));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setMaxCapacity(10);
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slot = timeSlotRepository.save(slot);

        visitor = new Visitor();
        visitor.setFullName("Test Visitor");
        visitor.setGroupSize(2);
        visitor.setEmail("test@test.com");
        visitor.setType(VisitorType.INDIVIDUAL);
        visitor = visitorRepository.save(visitor);
    }

    private Registration createReg(Visitor v, TimeSlot s, RegistrationStatus status) {
        Registration r = new Registration();
        r.setVisitor(v);
        r.setTimeSlot(s);
        r.setVisitPurpose(VisitPurpose.TOURISM);
        r.setStatus(status);
        return repository.save(r);
    }

    private Registration createRegWithPurpose(Visitor v, TimeSlot s, RegistrationStatus status, VisitPurpose purpose, boolean isProspect) {
        Registration r = new Registration();
        r.setVisitor(v);
        r.setTimeSlot(s);
        r.setVisitPurpose(purpose);
        r.setProspect(isProspect);
        r.setStatus(status);
        return repository.save(r);
    }

    @Test
    void existsByVisitorIdAndTimeSlotId_duplicate() {
        createReg(visitor, slot, RegistrationStatus.PENDING);
        assertThat(repository.existsByVisitorIdAndTimeSlotId(visitor.getId(), slot.getId())).isTrue();
    }

    @Test
    void existsByVisitorIdAndTimeSlotId_notDuplicate() {
        assertThat(repository.existsByVisitorIdAndTimeSlotId(visitor.getId(), slot.getId())).isFalse();
    }

    @Test
    void countByTimeSlotIdAndStatusNotIn_countsActiveOnly() {
        createReg(visitor, slot, RegistrationStatus.PENDING);

        Visitor v2 = new Visitor();
        v2.setFullName("V2");
        v2.setGroupSize(1);
        v2.setType(VisitorType.GROUP);
        v2 = visitorRepository.save(v2);
        createReg(v2, slot, RegistrationStatus.REJECTED);

        long count = repository.countByTimeSlotIdAndStatusNotIn(
                slot.getId(), List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED));
        assertThat(count).isEqualTo(1);
    }

    @Test
    void findAllByTimeSlotDate_ordersBySlotThenName() {
        createReg(visitor, slot, RegistrationStatus.CONFIRMED);

        TimeSlot later = new TimeSlot();
        later.setDate(slot.getDate());
        later.setStartTime(LocalTime.of(14, 0));
        later.setEndTime(LocalTime.of(16, 0));
        later.setMaxCapacity(10);
        later.setStatus(TimeSlotStatus.AVAILABLE);
        later = timeSlotRepository.save(later);

        Visitor v2 = new Visitor();
        v2.setFullName("Z Visitor");
        v2.setGroupSize(1);
        v2.setType(VisitorType.GROUP);
        v2 = visitorRepository.save(v2);
        createReg(v2, later, RegistrationStatus.CONFIRMED);

        List<Registration> all = repository.findAllByTimeSlotDate(slot.getDate());
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getTimeSlot().getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(all.get(1).getTimeSlot().getStartTime()).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void findByIsProspectTrue_returnsOnlyProspects() {
        createRegWithPurpose(visitor, slot, RegistrationStatus.CONFIRMED, VisitPurpose.PURCHASE, true);

        Visitor v2 = new Visitor();
        v2.setFullName("Tourist");
        v2.setGroupSize(1);
        v2.setType(VisitorType.INDIVIDUAL);
        v2 = visitorRepository.save(v2);
        createRegWithPurpose(v2, slot, RegistrationStatus.CONFIRMED, VisitPurpose.TOURISM, false);

        List<Registration> prospects = repository.findByIsProspectTrue();
        assertThat(prospects).hasSize(1);
        assertThat(prospects.get(0).getVisitPurpose()).isEqualTo(VisitPurpose.PURCHASE);
        assertThat(prospects.get(0).isProspect()).isTrue();
    }
}
