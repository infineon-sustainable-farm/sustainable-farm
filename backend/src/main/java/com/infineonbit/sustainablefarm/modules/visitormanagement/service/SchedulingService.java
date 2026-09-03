package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AvailabilityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlot;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for the farm tour calendar (Scheduling sub-module).
 */
public interface SchedulingService {

    List<TimeSlotResponse> findByDate(LocalDate date);

    List<TimeSlotResponse> findAll();

    TimeSlotResponse findById(Long id);

    TimeSlotResponse create(TimeSlotRequest request);

    TimeSlotResponse update(Long id, TimeSlotRequest request);

    void cancel(Long id);

    TimeSlotResponse assignGuide(Long id, Long guideId);

    List<AvailabilityResponse> getAvailability(LocalDate date);

    /**
     * Recalculates the slot status based on its current bookings (RESERVED vs FULL).
     */
    void refreshStatus(TimeSlot slot);
}
