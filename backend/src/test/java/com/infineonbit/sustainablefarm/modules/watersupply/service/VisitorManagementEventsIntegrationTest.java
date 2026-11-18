package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.infineonbit.sustainablefarm.modules.visitormanagement.api.VisitScheduled;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.AgriActivityRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.BookingRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.RegistrationRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.TimeSlotRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.SchedulingRules;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorEvent;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.WaterVisitorEventRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A visit scheduled in Visitor Management reaches Water Supply in process, once committed: no test
 * transaction here, the after-commit listener has to run for real.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class VisitorManagementEventsIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WaterVisitorEventRepository visitorEventRepository;
    @Autowired
    private AlertBoard alertBoard;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private AgriActivityRepository activityRepository;
    @Autowired
    private VisitorRepository visitorRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final String marker = UUID.randomUUID().toString().substring(0, 8);
    private final List<String> visitIds = new ArrayList<>();
    private final List<Long> registrationIds = new ArrayList<>();
    private final List<Long> bookingIds = new ArrayList<>();
    private Long activityId;
    private Long visitorId;
    private Long slotId;

    @AfterEach
    void removeCreatedRows() {
        visitIds.forEach(id -> visitorEventRepository.findByVisitId(id).ifPresent(visitorEventRepository::delete));
        alertBoard.list(AlertService.SOURCE, null).stream()
                .filter(n -> n.title() != null && n.title().contains(marker))
                .forEach(n -> alertBoard.delete(AlertService.SOURCE, n.id()));
        registrationIds.forEach(registrationRepository::deleteById);
        bookingIds.forEach(bookingRepository::deleteById);
        if (activityId != null) {
            activityRepository.deleteById(activityId);
        }
        if (visitorId != null) {
            visitorRepository.deleteById(visitorId);
        }
        if (slotId != null) {
            timeSlotRepository.deleteById(slotId);
        }
    }

    @Test
    void aRegistrationIsAnnouncedToWaterSupplyMovedWithItsSlotAndWithdrawnOnCancel() throws Exception {
        LocalDate day = freeOpenDay();
        slotId = id(mockMvc.perform(post("/api/v1/time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"%s","startTime":"09:00","endTime":"11:00","maxCapacity":10}
                                """.formatted(day)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String group = "School " + marker;
        visitorId = id(mockMvc.perform(post("/api/v1/visitors").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"%s","groupSize":4,"type":"GROUP"}
                                """.formatted(group)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        Long registrationId = id(mockMvc.perform(post("/api/v1/registrations").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visitorId":%d,"timeSlotId":%d,"visitPurpose":"TOURISM"}
                                """.formatted(visitorId, slotId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        registrationIds.add(registrationId);
        String visitId = "registration-" + registrationId;
        visitIds.add(visitId);

        WaterVisitorEvent announced = visitorEventRepository.findByVisitId(visitId).orElseThrow();
        assertThat(announced.getGroupName()).isEqualTo(group);
        assertThat(announced.getExpectedAt()).isEqualTo(day.atTime(9, 0).toInstant(ZoneOffset.UTC));
        assertThat(announced.getConflict()).isFalse();
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Visitor group near water facilities - " + group)));

        mockMvc.perform(put("/api/v1/time-slots/{id}", slotId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"%s","startTime":"14:00","endTime":"16:00","maxCapacity":10}
                                """.formatted(day)))
                .andExpect(status().isOk());
        WaterVisitorEvent moved = visitorEventRepository.findByVisitId(visitId).orElseThrow();
        assertThat(moved.getId()).isEqualTo(announced.getId());
        assertThat(moved.getExpectedAt()).isEqualTo(day.atTime(14, 0).toInstant(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/registrations/{id}/cancel", registrationId)).andExpect(status().isOk());
        assertThat(visitorEventRepository.findByVisitId(visitId)).isEmpty();
    }

    @Test
    void aBookingIsAnnouncedToWaterSupplyAndWithdrawnOnCancel() throws Exception {
        LocalDate day = freeOpenDay();
        slotId = id(mockMvc.perform(post("/api/v1/time-slots").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"%s","startTime":"09:00","endTime":"11:00","maxCapacity":10}
                                """.formatted(day)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        activityId = id(mockMvc.perform(post("/api/v1/activities").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tour %s","price":10,"capacity":10,"durationMinutes":60}
                                """.formatted(marker)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String group = "Family " + marker;

        Long bookingId = id(mockMvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activityId":%d,"timeSlotId":%d,"visitorFullName":"%s",
                                 "visitorEmail":"family@example.com","peopleCount":3}
                                """.formatted(activityId, slotId, group)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        bookingIds.add(bookingId);
        String visitId = "booking-" + bookingId;
        visitIds.add(visitId);

        WaterVisitorEvent announced = visitorEventRepository.findByVisitId(visitId).orElseThrow();
        assertThat(announced.getGroupName()).isEqualTo(group);
        assertThat(announced.getExpectedAt()).isEqualTo(day.atTime(9, 0).toInstant(ZoneOffset.UTC));
        mockMvc.perform(get("/api/notifications"))
                .andExpect(jsonPath("$[*].title", hasItem("Visitor group near water facilities - " + group)));

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", bookingId)).andExpect(status().isOk());
        assertThat(visitorEventRepository.findByVisitId(visitId)).isEmpty();
    }

    @Test
    void anEventDeliveredTwiceKeepsASingleAnnouncement() {
        String visitId = "registration-replay-" + marker;
        visitIds.add(visitId);
        Instant at = Instant.parse("2031-06-03T09:00:00Z");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status ->
                eventPublisher.publishEvent(new VisitScheduled(visitId, "Replay " + marker, at, 2)));
        transaction.executeWithoutResult(status ->
                eventPublisher.publishEvent(new VisitScheduled(visitId, "Replay " + marker, at, 2)));

        assertThat(visitorEventRepository.findAll().stream().filter(e -> visitId.equals(e.getVisitId())))
                .hasSize(1);
    }

    @Test
    void anEventOfARolledBackTransactionIsNotAnnounced() {
        String visitId = "registration-rollback-" + marker;
        visitIds.add(visitId);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new VisitScheduled(visitId, "Rollback " + marker,
                    Instant.parse("2031-06-03T09:00:00Z"), 2));
            status.setRollbackOnly();
        });

        assertThat(visitorEventRepository.findByVisitId(visitId)).isEmpty();
    }

    /** A future working day with no slot yet, so the slot rules (overlap, slots per day) never collide. */
    private LocalDate freeOpenDay() {
        while (true) {
            LocalDate day = LocalDate.now().plusYears(5).plusDays(ThreadLocalRandom.current().nextInt(3000));
            if (day.getDayOfWeek() != SchedulingRules.CLOSED_DAY && timeSlotRepository.findByDate(day).isEmpty()) {
                return day;
            }
        }
    }

    private static Long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
