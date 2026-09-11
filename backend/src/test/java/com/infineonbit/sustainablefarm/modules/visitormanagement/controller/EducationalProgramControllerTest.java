package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.EducationalProgramService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EducationalProgramController.class)
@Import(GlobalExceptionHandler.class)
class EducationalProgramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EducationalProgramService programService;

    private TourStopResponse buildStopResponse() {
        TourStopResponse r = new TourStopResponse();
        r.setId(1L);
        r.setName("Mango orchard");
        r.setPosition(2);
        r.setDurationMinutes(20);
        r.setMaxCapacity(15);
        r.setActive(true);
        return r;
    }

    private WorkshopResponse buildWorkshopResponse() {
        WorkshopResponse r = new WorkshopResponse();
        r.setId(1L);
        r.setName("Standard farm tour");
        r.setDurationMinutes(100);
        r.setTargetGroup("All");
        r.setStatus(WorkshopStatus.ACTIVE);
        return r;
    }

    private String stopJson() {
        return "{\"name\":\"Mango orchard\",\"position\":2,\"durationMinutes\":20,"
                + "\"maxCapacity\":15,\"location\":\"Orchard\"}";
    }

    private String workshopJson() {
        return "{\"name\":\"Standard farm tour\",\"durationMinutes\":100,\"targetGroup\":\"All\","
                + "\"facilitator\":\"Alix\"}";
    }

    @Test
    void stops_returns200() throws Exception {
        when(programService.listStops()).thenReturn(List.of(buildStopResponse()));

        mockMvc.perform(get("/api/v1/tour-stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value(2))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void createStop_returns201() throws Exception {
        when(programService.createStop(any(TourStopRequest.class))).thenReturn(buildStopResponse());

        mockMvc.perform(post("/api/v1/tour-stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stopJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mango orchard"));
    }

    @Test
    void createStop_missingDuration_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tour-stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mango orchard\",\"position\":2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStop_duplicatePosition_returns409() throws Exception {
        when(programService.createStop(any(TourStopRequest.class)))
                .thenThrow(new ConflictException("Another active stop already uses position 2"));

        mockMvc.perform(post("/api/v1/tour-stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stopJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStop_returns200() throws Exception {
        when(programService.updateStop(eq(1L), any(TourStopRequest.class)))
                .thenReturn(buildStopResponse());

        mockMvc.perform(put("/api/v1/tour-stops/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stopJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deactivateStop_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/tour-stops/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void workshops_returns200() throws Exception {
        when(programService.listWorkshops(isNull())).thenReturn(List.of(buildWorkshopResponse()));

        mockMvc.perform(get("/api/v1/workshops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void createWorkshop_returns201() throws Exception {
        when(programService.createWorkshop(any(WorkshopRequest.class))).thenReturn(buildWorkshopResponse());

        mockMvc.perform(post("/api/v1/workshops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workshopJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Standard farm tour"));
    }

    @Test
    void createWorkshop_missingTargetGroup_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/workshops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Standard farm tour\",\"durationMinutes\":100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishWorkshop_returns200() throws Exception {
        when(programService.publishWorkshop(1L)).thenReturn(buildWorkshopResponse());

        mockMvc.perform(post("/api/v1/workshops/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void publishWorkshop_notDraft_returns422() throws Exception {
        when(programService.publishWorkshop(1L)).thenThrow(
                new BusinessRuleException("Only DRAFT workshops can be published"));

        mockMvc.perform(post("/api/v1/workshops/1/publish"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deactivateWorkshop_returns200() throws Exception {
        WorkshopResponse inactivated = buildWorkshopResponse();
        inactivated.setStatus(WorkshopStatus.INACTIVE);
        when(programService.deactivateWorkshop(1L)).thenReturn(inactivated);

        mockMvc.perform(post("/api/v1/workshops/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}