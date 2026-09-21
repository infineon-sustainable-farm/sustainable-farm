package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AvailabilityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Staff;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.StaffRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceImplTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @InjectMocks
    private SchedulingServiceImpl service;

    private LocalDate monday;

    @BeforeEach
    void setUp() {
        monday = LocalDate.of(2026, 9, 7);
    }

    private TimeSlot buildSlot(Long id, TimeSlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.setId(id);
        slot.setDate(monday);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setMaxCapacity(10);
        slot.setStatus(status);
        return slot;
    }

    private TimeSlotRequest buildRequest() {
        TimeSlotRequest req = new TimeSlotRequest();
        req.setDate(monday);
        req.setStartTime(LocalTime.of(9, 0));
        req.setEndTime(LocalTime.of(11, 0));
        req.setMaxCapacity(10);
        return req;
    }

    @Test
    void create_sunday_throws() {
        TimeSlotRequest req = buildRequest();
        req.setDate(LocalDate.of(2026, 9, 6)); // Sunday

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SUNDAY");
    }

    @Test
    void create_duplicateTime_throws() {
        when(timeSlotRepository.existsOverlapping(monday, LocalTime.of(9, 0), LocalTime.of(11, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_success() {
        when(timeSlotRepository.existsOverlapping(monday, LocalTime.of(9, 0), LocalTime.of(11, 0)))
                .thenReturn(false);
        when(timeSlotRepository.countByDateAndStatusNot(monday, TimeSlotStatus.CANCELLED)).thenReturn(0L);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> {
            TimeSlot ts = inv.getArgument(0);
            ts.setId(1L);
            return ts;
        });
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(eq(1L), any())).thenReturn(0L);

        TimeSlotResponse resp = service.create(buildRequest());
        assertThat(resp.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    void create_maxSlotsReached_throws() {
        when(timeSlotRepository.existsOverlapping(monday, LocalTime.of(9, 0), LocalTime.of(11, 0)))
                .thenReturn(false);
        when(timeSlotRepository.countByDateAndStatusNot(monday, TimeSlotStatus.CANCELLED))
                .thenReturn((long) SchedulingRules.MAX_SLOTS_PER_DAY);

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Maximum");
    }

    @Test
    void update_cancelledSlot_throws() {
        TimeSlot slot = buildSlot(1L, TimeSlotStatus.CANCELLED);
        when(timeSlotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.update(1L, buildRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void cancel_completedSlot_throws() {
        TimeSlot slot = buildSlot(1L, TimeSlotStatus.COMPLETED);
        when(timeSlotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void cancel_success_propagatesToRegistrationsAndBookings() {
        TimeSlot slot = buildSlot(1L, TimeSlotStatus.AVAILABLE);
        when(timeSlotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.findByTimeSlotIdAndStatusNotIn(eq(1L), any())).thenReturn(List.of());

        service.cancel(1L);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.CANCELLED);
        verify(bookingService).cancelBookingsForSlot(1L);
    }

    @Test
    void cancel_twice_isIdempotent() {
        TimeSlot slot = buildSlot(1L, TimeSlotStatus.CANCELLED);
        when(timeSlotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));

        service.cancel(1L);
        verify(bookingService, never()).cancelBookingsForSlot(anyLong());
    }

    @Test
    void assignGuide_success() {
        TimeSlot slot = buildSlot(1L, TimeSlotStatus.AVAILABLE);
        Staff guide = new Staff();
        guide.setId(42L);
        guide.setFullName("Awa");
        when(timeSlotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(slot));
        when(staffRepository.findById(42L)).thenReturn(Optional.of(guide));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(eq(1L), any())).thenReturn(0L);

        TimeSlotResponse resp = service.assignGuide(1L, 42L);
        assertThat(resp.getGuideId()).isEqualTo(42L);
    }

    @Test
    void findById_notFound_throws() {
        when(timeSlotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAvailability_filtersCancelled() {
        TimeSlot active = buildSlot(1L, TimeSlotStatus.AVAILABLE);
        TimeSlot cancelled = buildSlot(2L, TimeSlotStatus.CANCELLED);
        when(timeSlotRepository.findByDate(monday)).thenReturn(List.of(active, cancelled));
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(eq(1L), any())).thenReturn(0L);

        List<AvailabilityResponse> result = service.getAvailability(monday);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void refreshStatus_noBookings_setsAvailable() {
        TimeSlot slot = buildSlot(1L, TimeSlotStatus.AVAILABLE);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(eq(1L), any())).thenReturn(0L);

        service.refreshStatus(slot);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
    }

    @Test
    void refreshStatus_hasBookings_setsReserved() {
        TimeSlot slot = buildSlot(1L, TimeSlotStatus.AVAILABLE);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(eq(1L), any())).thenReturn(3L);

        service.refreshStatus(slot);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.RESERVED);
    }

    @Test
    void refreshStatus_full_setsFull() {
        TimeSlot slot = buildSlot(1L, TimeSlotStatus.RESERVED);
        when(registrationRepository.countByTimeSlotIdAndStatusNotIn(eq(1L), any())).thenReturn(10L);

        service.refreshStatus(slot);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.FULL);
    }
}
