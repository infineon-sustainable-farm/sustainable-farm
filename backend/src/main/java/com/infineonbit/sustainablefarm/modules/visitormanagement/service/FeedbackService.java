package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackSummaryResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RouteFeedbackRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.SurveyRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.SurveyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveyStatus;

import java.time.Instant;
import java.util.List;

/**
 * Business logic for visitor feedback collection: on-site tablet responses,
 * post-visit surveys sent by email/SMS/WhatsApp, routing to module owners
 * and compiled satisfaction statistics.
 */
public interface FeedbackService {

    List<FeedbackResponse> listFeedback(Long visitorId, Instant from, Instant to);

    FeedbackResponse submitFeedback(FeedbackRequest request);

    FeedbackSummaryResponse summary(Instant from, Instant to);

    FeedbackResponse routeFeedback(Long id, RouteFeedbackRequest request);

    List<SurveyResponse> listSurveys(Long visitorId, SurveyStatus status);

    SurveyResponse sendSurvey(SurveyRequest request);
}