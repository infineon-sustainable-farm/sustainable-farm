package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackSummaryResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RouteFeedbackRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.SurveyRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.SurveyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Feedback;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.FeedbackChannel;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveySend;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveyStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.FeedbackRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.SurveySendRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.VisitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private SurveySendRepository surveySendRepository;
    @Mock
    private VisitorRepository visitorRepository;
    @InjectMocks
    private FeedbackServiceImpl service;

    private Long visitorId = 10L;

    private Visitor buildVisitor() {
        Visitor visitor = new Visitor();
        visitor.setId(visitorId);
        visitor.setFullName("Alice Dupont");
        visitor.setGroupSize(2);
        return visitor;
    }

    private Feedback buildFeedback(int rating, String recommend) {
        Feedback f = new Feedback();
        f.setId((long) rating);
        f.setVisitor(buildVisitor());
        f.setRating(rating);
        f.setRecommend(recommend);
        f.setSubmittedAt(Instant.now());
        return f;
    }

    private SurveySend buildSurvey(FeedbackChannel channel, SurveyStatus status, Long id) {
        SurveySend s = new SurveySend();
        s.setId(id);
        s.setVisitor(buildVisitor());
        s.setChannel(channel);
        s.setStatus(status);
        s.setSentAt(Instant.now());
        return s;
    }

    private FeedbackRequest buildRequest() {
        FeedbackRequest req = new FeedbackRequest();
        req.setVisitorId(visitorId);
        req.setRating(4);
        req.setComment("Great visit");
        return req;
    }

    @Test
    void submitFeedback_onSite_success() {
        when(visitorRepository.findById(visitorId)).thenReturn(Optional.of(buildVisitor()));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackResponse response = service.submitFeedback(buildRequest());

        assertThat(response.getOrigin()).isEqualTo(FeedbackChannel.ON_SITE);
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getSurveyId()).isNull();
        assertThat(response.getComment()).isEqualTo("Great visit");
    }

    @Test
    void submitFeedback_viaSurvey_success() {
        SurveySend survey = buildSurvey(FeedbackChannel.EMAIL, SurveyStatus.SENT, 7L);
        when(visitorRepository.findById(visitorId)).thenReturn(Optional.of(buildVisitor()));
        when(surveySendRepository.findById(7L)).thenReturn(Optional.of(survey));
        when(surveySendRepository.save(any(SurveySend.class))).thenAnswer(inv -> inv.getArgument(0));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackRequest req = buildRequest();
        req.setSurveyId(7L);
        FeedbackResponse response = service.submitFeedback(req);

        assertThat(response.getOrigin()).isEqualTo(FeedbackChannel.EMAIL);
        assertThat(response.getSurveyId()).isEqualTo(7L);
        assertThat(survey.getStatus()).isEqualTo(SurveyStatus.RECEIVED);
        assertThat(survey.getFeedback()).isNotNull();
    }

    @Test
    void submitFeedback_surveyAlreadyReceived_throws() {
        SurveySend survey = buildSurvey(FeedbackChannel.SMS, SurveyStatus.RECEIVED, 7L);
        when(visitorRepository.findById(visitorId)).thenReturn(Optional.of(buildVisitor()));
        when(surveySendRepository.findById(7L)).thenReturn(Optional.of(survey));

        FeedbackRequest req = buildRequest();
        req.setSurveyId(7L);

        assertThatThrownBy(() -> service.submitFeedback(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already received");
    }

    @Test
    void submitFeedback_surveyVisitorMismatch_throws() {
        Visitor other = new Visitor();
        other.setId(99L);
        SurveySend survey = buildSurvey(FeedbackChannel.EMAIL, SurveyStatus.SENT, 7L);
        survey.setVisitor(other);
        when(visitorRepository.findById(visitorId)).thenReturn(Optional.of(buildVisitor()));
        when(surveySendRepository.findById(7L)).thenReturn(Optional.of(survey));

        FeedbackRequest req = buildRequest();
        req.setSurveyId(7L);

        assertThatThrownBy(() -> service.submitFeedback(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different visitor");
    }

    @Test
    void submitFeedback_visitorNotFound_throws() {
        when(visitorRepository.findById(visitorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitFeedback(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void routeFeedback_success() {
        Feedback feedback = buildFeedback(4, null);
        when(feedbackRepository.findById(5L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        RouteFeedbackRequest req = new RouteFeedbackRequest();
        req.setRoutedTo("Sales (Mariata)");
        FeedbackResponse response = service.routeFeedback(5L, req);

        assertThat(response.getRoutedTo()).isEqualTo("Sales (Mariata)");
    }

    @Test
    void routeFeedback_notFound_throws() {
        when(feedbackRepository.findById(999L)).thenReturn(Optional.empty());

        RouteFeedbackRequest req = new RouteFeedbackRequest();
        req.setRoutedTo("Team");

        assertThatThrownBy(() -> service.routeFeedback(999L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void summary_computesStats() {
        when(feedbackRepository.findAll()).thenReturn(List.of(
                buildFeedback(5, "Definitely"),
                buildFeedback(4, "Yes"),
                buildFeedback(4, null),
                buildFeedback(3, "Maybe")));

        FeedbackSummaryResponse summary = service.summary(null, null);

        assertThat(summary.getTotal()).isEqualTo(4);
        assertThat(summary.getAverageRating()).isEqualTo(4.0);
        assertThat(summary.getRecommendPct()).isEqualTo(75.0);
        assertThat(summary.getDistribution().get(1)).isEqualTo(0L);
        assertThat(summary.getDistribution().get(2)).isEqualTo(0L);
        assertThat(summary.getDistribution().get(3)).isEqualTo(1L);
        assertThat(summary.getDistribution().get(4)).isEqualTo(2L);
        assertThat(summary.getDistribution().get(5)).isEqualTo(1L);
    }

    @Test
    void summary_empty() {
        when(feedbackRepository.findAll()).thenReturn(List.of());

        FeedbackSummaryResponse summary = service.summary(null, null);

        assertThat(summary.getTotal()).isZero();
        assertThat(summary.getAverageRating()).isZero();
        assertThat(summary.getDistribution().values()).containsOnly(0L);
    }

    @Test
    void sendSurvey_success() {
        when(visitorRepository.findById(visitorId)).thenReturn(Optional.of(buildVisitor()));
        when(surveySendRepository.save(any(SurveySend.class))).thenAnswer(inv -> inv.getArgument(0));

        SurveyRequest req = new SurveyRequest();
        req.setVisitorId(visitorId);
        req.setChannel(FeedbackChannel.EMAIL);
        SurveyResponse response = service.sendSurvey(req);

        assertThat(response.getChannel()).isEqualTo(FeedbackChannel.EMAIL);
        assertThat(response.getStatus()).isEqualTo(SurveyStatus.SENT);
        assertThat(response.getRating()).isNull();
    }

    @Test
    void sendSurvey_onSite_throws() {
        SurveyRequest req = new SurveyRequest();
        req.setVisitorId(visitorId);
        req.setChannel(FeedbackChannel.ON_SITE);

        assertThatThrownBy(() -> service.sendSurvey(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ON_SITE");
    }

    @Test
    void sendSurvey_visitorNotFound_throws() {
        when(visitorRepository.findById(visitorId)).thenReturn(Optional.empty());

        SurveyRequest req = new SurveyRequest();
        req.setVisitorId(visitorId);
        req.setChannel(FeedbackChannel.SMS);

        assertThatThrownBy(() -> service.sendSurvey(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}