package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.GlobalExceptionHandler;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackSummaryResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RouteFeedbackRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.SurveyRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.SurveyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.FeedbackChannel;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveyStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedbackController.class)
@Import(GlobalExceptionHandler.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    private FeedbackResponse buildFeedbackResponse(Long id) {
        FeedbackResponse r = new FeedbackResponse();
        r.setId(id);
        r.setVisitorId(10L);
        r.setVisitorName("Alice Dupont");
        r.setOrigin(FeedbackChannel.ON_SITE);
        r.setRating(4);
        r.setComment("Great visit");
        return r;
    }

    private SurveyResponse buildSurveyResponse(Long id) {
        SurveyResponse r = new SurveyResponse();
        r.setId(id);
        r.setVisitorId(10L);
        r.setVisitorName("Alice Dupont");
        r.setChannel(FeedbackChannel.EMAIL);
        r.setStatus(SurveyStatus.SENT);
        r.setSentAt(Instant.now());
        return r;
    }

    @Test
    void list_returns200() throws Exception {
        when(feedbackService.listFeedback(isNull(), isNull(), isNull()))
                .thenReturn(List.of(buildFeedbackResponse(1L)));

        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(4))
                .andExpect(jsonPath("$[0].origin").value("ON_SITE"));
    }

    @Test
    void list_withVisitorId_filters() throws Exception {
        when(feedbackService.listFeedback(eq(10L), isNull(), isNull()))
                .thenReturn(List.of(buildFeedbackResponse(1L)));

        mockMvc.perform(get("/api/v1/feedback").param("visitorId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitorId").value(10));
    }

    @Test
    void summary_returns200() throws Exception {
        FeedbackSummaryResponse summary = FeedbackSummaryResponse.of(4.2, 18, 92.0,
                Map.of(1, 0L, 2, 0L, 3, 2L, 4, 6L, 5, 8L));
        when(feedbackService.summary(isNull(), isNull())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/feedback/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.2))
                .andExpect(jsonPath("$.total").value(18));
    }

    @Test
    void submit_returns201() throws Exception {
        when(feedbackService.submitFeedback(any(FeedbackRequest.class)))
                .thenReturn(buildFeedbackResponse(1L));

        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":10,\"rating\":4,\"comment\":\"Great visit\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void submit_missingRating_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_ratingOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":10,\"rating\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void route_returns200() throws Exception {
        FeedbackResponse routed = buildFeedbackResponse(1L);
        routed.setRoutedTo("Sales (Mariata)");
        when(feedbackService.routeFeedback(eq(1L), any(RouteFeedbackRequest.class)))
                .thenReturn(routed);

        mockMvc.perform(patch("/api/v1/feedback/1/route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routedTo\":\"Sales (Mariata)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routedTo").value("Sales (Mariata)"));
    }

    @Test
    void surveys_returns200() throws Exception {
        when(feedbackService.listSurveys(isNull(), isNull()))
                .thenReturn(List.of(buildSurveyResponse(1L)));

        mockMvc.perform(get("/api/v1/surveys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    void sendSurvey_returns201() throws Exception {
        when(feedbackService.sendSurvey(any(SurveyRequest.class)))
                .thenReturn(buildSurveyResponse(1L));

        mockMvc.perform(post("/api/v1/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":10,\"channel\":\"EMAIL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("EMAIL"));
    }

    @Test
    void sendSurvey_onSiteChannel_returns422() throws Exception {
        when(feedbackService.sendSurvey(any(SurveyRequest.class)))
                .thenThrow(new BusinessRuleException("ON_SITE surveys cannot be sent"));

        mockMvc.perform(post("/api/v1/surveys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":10,\"channel\":\"ON_SITE\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}