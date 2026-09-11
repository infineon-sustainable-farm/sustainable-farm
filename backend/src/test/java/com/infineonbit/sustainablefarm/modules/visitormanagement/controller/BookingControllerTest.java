package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.AgriActivityResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingOccupancyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BookingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentMethod;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    private AgriActivityResponse buildActivityResponse() {
        AgriActivityResponse r = new AgriActivityResponse();
        r.setId(1L);
        r.setName("Solar workshop");
        r.setPrice(new BigDecimal("5000"));
        r.setCapacity(15);
        r.setDurationMinutes(45);
        r.setActive(true);
        return r;
    }

    private BookingResponse buildBookingResponse() {
        BookingResponse r = new BookingResponse();
        r.setId(1L);
        r.setReference("BK-00001");
        r.setActivityId(1L);
        r.setActivityName("Solar workshop");
        r.setActivityPrice(new BigDecimal("5000"));
        r.setTimeSlotId(2L);
        r.setSlotDate(LocalDate.of(2026, 8, 26));
        r.setSlotStartTime(LocalTime.of(14, 0));
        r.setSlotEndTime(LocalTime.of(16, 0));
        r.setVisitorFullName("Lucas Weber");
        r.setVisitorEmail("lucas@example.com");
        r.setPeopleCount(3);
        r.setTotalAmount(new BigDecimal("15000"));
        r.setPaymentMethod(BookingPaymentMethod.ORANGE_MONEY);
        r.setPaymentStatus(BookingPaymentStatus.PAID);
        r.setStatus(BookingStatus.CONFIRMED);
        return r;
    }

    private String activityJson() {
        return "{\"name\":\"Solar workshop\",\"price\":5000,\"capacity\":15,"
                + "\"durationMinutes\":45,\"description\":\"Guided tour of the plant\"}";
    }

    private String bookingJson() {
        return "{\"activityId\":1,\"timeSlotId\":2,\"visitorFullName\":\"Lucas Weber\","
                + "\"visitorEmail\":\"lucas@example.com\",\"peopleCount\":3,"
                + "\"paymentMethod\":\"ORANGE_MONEY\"}";
    }

    @Test
    void activities_returns200() throws Exception {
        when(bookingService.listActivities()).thenReturn(List.of(buildActivityResponse()));

        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Solar workshop"));
    }

    @Test
    void createActivity_returns201() throws Exception {
        when(bookingService.createActivity(any(AgriActivityRequest.class)))
                .thenReturn(buildActivityResponse());

        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activityJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(5000));
    }

    @Test
    void createActivity_missingPrice_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Solar workshop\",\"capacity\":15}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateActivity_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/activities/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void bookings_returns200() throws Exception {
        when(bookingService.listBookings(isNull(), isNull(), isNull()))
                .thenReturn(List.of(buildBookingResponse()));

        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reference").value("BK-00001"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void bookings_byStatus_returns200() throws Exception {
        when(bookingService.listBookings(eq(BookingStatus.PENDING), isNull(), isNull()))
                .thenReturn(List.of(buildBookingResponse()));

        mockMvc.perform(get("/api/v1/bookings").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void createBooking_returns201() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenReturn(buildBookingResponse());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.peopleCount").value(3));
    }

    @Test
    void createBooking_missingVisitorEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1,\"timeSlotId\":2,"
                                + "\"visitorFullName\":\"Lucas Weber\",\"peopleCount\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_exceedsCapacity_returns409() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new ConflictException("Not enough remaining capacity"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void markPaid_returns200() throws Exception {
        when(bookingService.markPaid(1L)).thenReturn(buildBookingResponse());

        mockMvc.perform(post("/api/v1/bookings/1/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    void confirmBooking_unpaid_returns422() throws Exception {
        when(bookingService.confirmBooking(1L)).thenThrow(
                new BusinessRuleException("Booking 1 must be paid before confirmation"));

        mockMvc.perform(post("/api/v1/bookings/1/confirm"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelBooking_returns200() throws Exception {
        BookingResponse cancelled = buildBookingResponse();
        cancelled.setStatus(BookingStatus.CANCELLED);
        cancelled.setPaymentStatus(BookingPaymentStatus.REFUNDED);
        when(bookingService.cancelBooking(1L)).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/bookings/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void occupancy_returns200() throws Exception {
        BookingOccupancyResponse occ = new BookingOccupancyResponse();
        occ.setActivityId(1L);
        occ.setActivityName("Solar workshop");
        occ.setCapacity(15);
        occ.setBooked(5);
        occ.setOccupancyPercent(33);
        when(bookingService.getOccupancy()).thenReturn(List.of(occ));

        mockMvc.perform(get("/api/v1/bookings/occupancy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].occupancyPercent").value(33));
    }
}