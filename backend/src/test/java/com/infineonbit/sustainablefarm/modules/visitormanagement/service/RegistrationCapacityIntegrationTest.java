package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitPurpose;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitorType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the capacity rule: a slot's remaining seats are people, not
 * registrations. Counting rows instead of group sizes would accept a group of
 * ten as a single visitor.
 */
@SpringBootTest
@ActiveProfiles("test")
class RegistrationCapacityIntegrationTest {

    private static final List<RegistrationStatus> INACTIVE =
            List.of(RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED);

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private VisitorRepository visitorRepository;
    @Autowired
    private RegistrationRepository registrationRepository;

    private final List<Long> registrationIds = new ArrayList<>();
    private final List<Long> visitorIds = new ArrayList<>();
    private Long slotId;

    @AfterEach
    void removeCreatedRows() {
        registrationIds.forEach(registrationRepository::deleteById);
        visitorIds.forEach(visitorRepository::deleteById);
        if (slotId != null) {
            timeSlotRepository.deleteById(slotId);
        }
    }

    private VisitorResponse createVisitor(String name, int groupSize) {
        VisitorRequest request = new VisitorRequest();
        request.setFullName(name);
        request.setGroupSize(groupSize);
        request.setType(groupSize > 1 ? VisitorType.GROUP : VisitorType.INDIVIDUAL);
        VisitorResponse created = registrationService.createVisitor(request);
        visitorIds.add(created.getId());
        return created;
    }

    private RegistrationResponse register(Long visitorId) {
        RegistrationRequest request = new RegistrationRequest();
        request.setVisitorId(visitorId);
        request.setTimeSlotId(slotId);
        request.setVisitPurpose(VisitPurpose.TOURISM);
        RegistrationResponse created = registrationService.register(request);
        registrationIds.add(created.getId());
        return created;
    }

    @Test
    void capacity_countsPeopleNotRegistrations() {
        TimeSlot slot = new TimeSlot();
        slot.setDate(LocalDate.of(2027, 3, 8)); // a Monday
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setMaxCapacity(10);
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slotId = timeSlotRepository.save(slot).getId();

        register(createVisitor("Capacity group A", 4).getId());
        register(createVisitor("Capacity group B", 5).getId());

        long booked = registrationRepository.sumGroupSizeByTimeSlotIdAndStatusNotIn(slotId, INACTIVE);
        assertThat(booked).isEqualTo(9);

        VisitorResponse tooMany = createVisitor("Capacity group C", 2);
        assertThatThrownBy(() -> register(tooMany.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacity");
    }
}