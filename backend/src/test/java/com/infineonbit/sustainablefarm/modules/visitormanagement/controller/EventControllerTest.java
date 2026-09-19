package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.EventResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.EventType;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitPurpose;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private Long eventId = 1L;
    private LocalDateTime start = LocalDateTime.of(2026, 10, 4, 10, 0);

    @BeforeEach
    void setUp() {
    }

    private EventResponse buildResponse(Long id) {
        EventResponse r = new EventResponse();
        r.setId(id);
        r.setTitle("Open Farm Day");
        r.setType(EventType.OPEN_DAY);
        r.setStartDateTime(start);
        r.setEndDateTime(start.plusHours(6));
        r.setMaxCapacity(60);
        r.setBooked(0);
        r.setStatus(EventStatus.DRAFT);
        return r;
    }

    @Test
    void list_returns200() throws Exception {
        when(eventService.listEvents(null, null)).thenReturn(List.of(buildResponse(eventId)));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void list_withType_filters() throws Exception {
        when(eventService.listEvents(EventType.SCHOOL, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/events").param("type", "SCHOOL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void get_returns200() throws Exception {
        when(eventService.getEvent(eventId)).thenReturn(buildResponse(eventId));

        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Open Farm Day"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(eventService.getEvent(999L)).thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201() throws Exception {
        when(eventService.createEvent(any(EventRequest.class))).thenReturn(buildResponse(eventId));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Open Farm Day\",\"type\":\"OPEN_DAY\","
                                + "\"startDateTime\":\"2026-10-04T10:00:00\","
                                + "\"endDateTime\":\"2026-10-04T16:00:00\",\"maxCapacity\":60}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(eventId));
    }

    @Test
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"OPEN_DAY\","
                                + "\"startDateTime\":\"2026-10-04T10:00:00\","
                                + "\"endDateTime\":\"2026-10-04T16:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200() throws Exception {
        when(eventService.updateEvent(eq(eventId), any(EventRequest.class)))
                .thenReturn(buildResponse(eventId));

        mockMvc.perform(put("/api/v1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"type\":\"OPEN_DAY\","
                                + "\"startDateTime\":\"2026-10-04T10:00:00\","
                                + "\"endDateTime\":\"2026-10-04T16:00:00\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/events/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void publish_returns200() throws Exception {
        EventResponse published = buildResponse(eventId);
        published.setStatus(EventStatus.PUBLISHED);
        when(eventService.publishEvent(eventId)).thenReturn(published);

        mockMvc.perform(post("/api/v1/events/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publish_notDraft_returns422() throws Exception {
        when(eventService.publishEvent(eventId)).thenThrow(
                new BusinessRuleException("Only DRAFT events can be published"));

        mockMvc.perform(post("/api/v1/events/1/publish"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void registrations_returns200() throws Exception {
        RegistrationResponse reg = new RegistrationResponse();
        reg.setId(5L);
        reg.setVisitorId(10L);
        reg.setEventId(eventId);
        reg.setStatus(RegistrationStatus.PENDING);
        reg.setVisitPurpose(VisitPurpose.PURCHASE);
        when(eventService.getRegistrations(eventId)).thenReturn(List.of(reg));

        mockMvc.perform(get("/api/v1/events/1/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(eventId));
    }

    @Test
    void register_returns201() throws Exception {
        RegistrationResponse reg = new RegistrationResponse();
        reg.setId(5L);
        reg.setVisitorId(10L);
        reg.setEventId(eventId);
        reg.setStatus(RegistrationStatus.PENDING);
        reg.setVisitPurpose(VisitPurpose.PURCHASE);
        reg.setIsProspect(true);
        when(eventService.registerVisitor(eq(eventId), any(EventRegistrationRequest.class)))
                .thenReturn(reg);

        mockMvc.perform(post("/api/v1/events/1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":10,\"visitPurpose\":\"PURCHASE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitorId").value(10))
                .andExpect(jsonPath("$.prospect").value(true));
    }

    @Test
    void register_missingPurpose_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events/1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":10}"))
                .andExpect(status().isBadRequest());
    }
}