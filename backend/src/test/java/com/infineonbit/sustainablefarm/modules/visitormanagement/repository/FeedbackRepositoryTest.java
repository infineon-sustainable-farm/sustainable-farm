package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Feedback;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.FeedbackChannel;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveySend;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveyStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FeedbackRepositoryTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private SurveySendRepository surveySendRepository;

    @Autowired
    private VisitorRepository visitorRepository;

    private Visitor visitor;
    private Visitor otherVisitor;

    @BeforeEach
    void setUp() {
        visitor = new Visitor();
        visitor.setFullName("Alice Dupont");
        visitor.setGroupSize(2);
        visitor = visitorRepository.save(visitor);

        otherVisitor = new Visitor();
        otherVisitor.setFullName("Bob Martin");
        otherVisitor.setGroupSize(1);
        otherVisitor = visitorRepository.save(otherVisitor);
    }

    private Feedback createFeedback(Visitor v, int rating, Instant submittedAt) {
        Feedback f = new Feedback();
        f.setVisitor(v);
        f.setOrigin(FeedbackChannel.ON_SITE);
        f.setRating(rating);
        f.setSubmittedAt(submittedAt);
        return feedbackRepository.save(f);
    }

    private SurveySend createSurvey(Visitor v, FeedbackChannel channel, SurveyStatus status) {
        SurveySend s = new SurveySend();
        s.setVisitor(v);
        s.setChannel(channel);
        s.setMessageTemplate("Thank you for visiting Sustainable Farm! Share your feedback: [link]");
        s.setSentAt(Instant.now());
        s.setStatus(status);
        return surveySendRepository.save(s);
    }

    @Test
    void findByVisitorId_returnsMatchingFeedback() {
        createFeedback(visitor, 5, Instant.now());
        createFeedback(otherVisitor, 3, Instant.now());

        List<Feedback> result = feedbackRepository.findByVisitorId(visitor.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isEqualTo(5);
    }

    @Test
    void findBySubmittedAtBetween_filtersByRange() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T00:00:00Z");
        createFeedback(visitor, 5, Instant.parse("2026-08-15T10:00:00Z"));
        createFeedback(visitor, 3, Instant.parse("2026-09-01T10:00:00Z"));

        List<Feedback> result = feedbackRepository.findBySubmittedAtBetween(from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isEqualTo(5);
    }

    @Test
    void findAllByOrderBySubmittedAtDesc_ordersNewestFirst() {
        createFeedback(visitor, 5, Instant.parse("2026-08-10T10:00:00Z"));
        createFeedback(visitor, 3, Instant.parse("2026-08-20T10:00:00Z"));

        List<Feedback> result = feedbackRepository.findAllByOrderBySubmittedAtDesc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRating()).isEqualTo(3);
    }

    @Test
    void countBySubmittedAtBetween_countsRange() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T00:00:00Z");
        createFeedback(visitor, 5, Instant.parse("2026-08-15T10:00:00Z"));
        createFeedback(visitor, 4, Instant.parse("2026-08-16T10:00:00Z"));
        createFeedback(visitor, 3, Instant.parse("2026-09-01T10:00:00Z"));

        assertThat(feedbackRepository.countBySubmittedAtBetween(from, to)).isEqualTo(2);
    }

    @Test
    void surveySend_findByVisitorId() {
        createSurvey(visitor, FeedbackChannel.EMAIL, SurveyStatus.SENT);
        createSurvey(otherVisitor, FeedbackChannel.SMS, SurveyStatus.SENT);

        List<SurveySend> result = surveySendRepository.findByVisitorId(visitor.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChannel()).isEqualTo(FeedbackChannel.EMAIL);
    }

    @Test
    void surveySend_findByStatus() {
        createSurvey(visitor, FeedbackChannel.EMAIL, SurveyStatus.SENT);
        createSurvey(visitor, FeedbackChannel.SMS, SurveyStatus.RECEIVED);

        List<SurveySend> result = surveySendRepository.findByStatus(SurveyStatus.SENT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChannel()).isEqualTo(FeedbackChannel.EMAIL);
    }

    @Test
    void surveySend_feedbackLink_persists() {
        SurveySend survey = createSurvey(visitor, FeedbackChannel.EMAIL, SurveyStatus.SENT);

        Feedback f = new Feedback();
        f.setVisitor(visitor);
        f.setOrigin(FeedbackChannel.EMAIL);
        f.setRating(4);
        f.setSubmittedAt(Instant.now());
        f.setSurveySend(survey);
        f = feedbackRepository.save(f);

        survey.setStatus(SurveyStatus.RECEIVED);
        SurveySend reloaded = surveySendRepository.findById(survey.getId()).orElseThrow();
        reloaded.setFeedback(f);
        surveySendRepository.save(reloaded);

        Feedback linked = feedbackRepository.findById(f.getId()).orElseThrow();
        assertThat(linked.getSurveySend().getId()).isEqualTo(survey.getId());
        assertThat(surveySendRepository.findById(survey.getId()).orElseThrow().getFeedback())
                .isNotNull();
    }
}