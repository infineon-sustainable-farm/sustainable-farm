package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.DashboardResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.UpcomingTask;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void dashboard_returnsAggregatedKpis() throws Exception {
        UpcomingTask task = UpcomingTask.of(UpcomingTask.Type.CONFIRM_BOOKING,
                "Confirm booking BK-00001 - Lucas Weber", Instant.parse("2026-09-10T12:00:00Z"));
        DashboardResponse response = DashboardResponse.of(5, 3, 2, 4.5,
                Collections.emptyList(), List.of(task));

        when(dashboardService.getDashboard()).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorsThisWeek").value(5))
                .andExpect(jsonPath("$.slotsBooked").value(3))
                .andExpect(jsonPath("$.pendingBriefings").value(2))
                .andExpect(jsonPath("$.averageSatisfaction").value(4.5))
                .andExpect(jsonPath("$.upcomingTasks[0].type").value("CONFIRM_BOOKING"))
                .andExpect(jsonPath("$.upcomingTasks[0].label").value("Confirm booking BK-00001 - Lucas Weber"));
    }
}