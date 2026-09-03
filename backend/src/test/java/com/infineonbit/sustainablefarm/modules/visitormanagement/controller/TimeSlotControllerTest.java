package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TimeSlotResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TimeSlotStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.SchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeSlotController.class)
@Import(GlobalExceptionHandler.class)
class TimeSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchedulingService schedulingService;

    private TimeSlotResponse buildResponse(Long id) {
        TimeSlotResponse r = new TimeSlotResponse();
        r.setId(id);
        r.setDate(LocalDate.of(2026, 9, 8));
        r.setStartTime(LocalTime.of(9, 0));
        r.setEndTime(LocalTime.of(11, 0));
        r.setMaxCapacity(10);
        r.setBooked(0);
        r.setStatus(TimeSlotStatus.AVAILABLE);
        return r;
    }

    @Test
    void list_returns200() throws Exception {
        when(schedulingService.findAll()).thenReturn(List.of(buildResponse(1L)));

        mockMvc.perform(get("/api/v1/time-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void list_withDate_filtersByDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 8);
        when(schedulingService.findByDate(date)).thenReturn(List.of(buildResponse(1L)));

        mockMvc.perform(get("/api/v1/time-slots").param("date", "2026-09-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void get_byId_returns200() throws Exception {
        when(schedulingService.findById(1L)).thenReturn(buildResponse(1L));

        mockMvc.perform(get("/api/v1/time-slots/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(schedulingService.findById(999L)).thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/time-slots/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_valid_returns201() throws Exception {
        TimeSlotResponse resp = buildResponse(1L);
        when(schedulingService.create(any(TimeSlotRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-09-08\",\"startTime\":\"09:00\",\"endTime\":\"11:00\",\"maxCapacity\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_missingDate_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"09:00\",\"endTime\":\"11:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200() throws Exception {
        TimeSlotResponse resp = buildResponse(1L);
        when(schedulingService.update(any(Long.class), any(TimeSlotRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/v1/time-slots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-09-08\",\"startTime\":\"09:00\",\"endTime\":\"11:00\",\"maxCapacity\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void cancel_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/time-slots/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void assignGuide_returns200() throws Exception {
        TimeSlotResponse resp = buildResponse(1L);
        resp.setGuideId(42L);
        when(schedulingService.assignGuide(1L, 42L)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/time-slots/1/assign-guide").param("guideId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guideId").value(42));
    }
}
