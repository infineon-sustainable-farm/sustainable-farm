package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BriefingRepositoryTest {

    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private VisitorRepository visitorRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private BriefingRepository repository;

    private Registration registration;

    @BeforeEach
    void setUp() {
        TimeSlot slot = new TimeSlot();
        slot.setDate(LocalDate.of(2026, 9, 9));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setMaxCapacity(10);
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slot = timeSlotRepository.save(slot);

        Visitor visitor = new Visitor();
        visitor.setFullName("Briefing Visitor");
        visitor.setGroupSize(1);
        visitor.setType(VisitorType.INDIVIDUAL);
        visitor = visitorRepository.save(visitor);

        registration = new Registration();
        registration.setVisitor(visitor);
        registration.setTimeSlot(slot);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration = registrationRepository.save(registration);
    }

    @Test
    void findByRegistrationId_present() {
        Briefing briefing = new Briefing();
        briefing.setRegistration(registration);
        briefing.setStatus(BriefingStatus.PENDING);
        repository.save(briefing);

        assertThat(repository.findByRegistrationId(registration.getId())).isPresent();
    }

    @Test
    void findByRegistrationId_absent() {
        assertThat(repository.findByRegistrationId(999L)).isEmpty();
    }
}
