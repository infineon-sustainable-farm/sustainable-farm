package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingOccupancyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Agritourism booking system: paid activities catalogue + reservations.
 */
public interface BookingService {

    List<AgriActivityResponse> listActivities();

    AgriActivityResponse createActivity(AgriActivityRequest request);

    AgriActivityResponse updateActivity(Long id, AgriActivityRequest request);

    void deactivateActivity(Long id);

    List<BookingResponse> listBookings(BookingStatus status, Long activityId, LocalDate date);

    BookingResponse createBooking(BookingRequest request);

    BookingResponse updateBooking(Long id, BookingRequest request);

    BookingResponse markPaid(Long id);

    BookingResponse confirmBooking(Long id);

    BookingResponse completeBooking(Long id);

    BookingResponse cancelBooking(Long id);

    List<BookingOccupancyResponse> getOccupancy();
}