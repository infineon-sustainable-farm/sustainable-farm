package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
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
class TimeSlotRepositoryTest {

    @Autowired
    private TimeSlotRepository repository;

    private LocalDate monday;

    @BeforeEach
    void setUp() {
        monday = LocalDate.of(2026, 9, 7); // Monday
    }

    private TimeSlot createSlot(LocalDate date, LocalTime start, LocalTime end, TimeSlotStatus status) {
        TimeSlot slot = new TimeSlot();
        slot.setDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setMaxCapacity(10);
        slot.setStatus(status);
        return repository.save(slot);
    }

    @Test
    void findByDate_returnsMatchingSlots() {
        createSlot(monday, LocalTime.of(9, 0), LocalTime.of(11, 0), TimeSlotStatus.AVAILABLE);
        createSlot(monday, LocalTime.of(14, 0), LocalTime.of(16, 0), TimeSlotStatus.AVAILABLE);
        LocalDate otherDay = monday.plusDays(1);
        createSlot(otherDay, LocalTime.of(9, 0), LocalTime.of(11, 0), TimeSlotStatus.AVAILABLE);

        List<TimeSlot> result = repository.findByDate(monday);
        assertThat(result).hasSize(2);
    }

    @Test
    void existsByDateAndStartTime_true() {
        createSlot(monday, LocalTime.of(9, 0), LocalTime.of(11, 0), TimeSlotStatus.AVAILABLE);

        assertThat(repository.existsByDateAndStartTime(monday, LocalTime.of(9, 0))).isTrue();
    }

    @Test
    void existsByDateAndStartTime_false() {
        assertThat(repository.existsByDateAndStartTime(monday, LocalTime.of(9, 0))).isFalse();
    }

    @Test
    void countByDateAndStatusNot_excludesCancelled() {
        createSlot(monday, LocalTime.of(9, 0), LocalTime.of(11, 0), TimeSlotStatus.AVAILABLE);
        createSlot(monday, LocalTime.of(14, 0), LocalTime.of(16, 0), TimeSlotStatus.CANCELLED);

        long count = repository.countByDateAndStatusNot(monday, TimeSlotStatus.CANCELLED);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void findActiveByDate_excludesCancelled() {
        createSlot(monday, LocalTime.of(9, 0), LocalTime.of(11, 0), TimeSlotStatus.AVAILABLE);
        createSlot(monday, LocalTime.of(14, 0), LocalTime.of(16, 0), TimeSlotStatus.CANCELLED);

        List<TimeSlot> active = repository.findActiveByDate(monday, TimeSlotStatus.CANCELLED);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isNotEqualTo(TimeSlotStatus.CANCELLED);
    }
}
