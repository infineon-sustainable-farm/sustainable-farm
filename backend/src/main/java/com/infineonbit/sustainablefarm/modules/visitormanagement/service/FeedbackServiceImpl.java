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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SurveySendRepository surveySendRepository;
    private final VisitorRepository visitorRepository;

    public FeedbackServiceImpl(FeedbackRepository feedbackRepository,
                               SurveySendRepository surveySendRepository,
                               VisitorRepository visitorRepository) {
        this.feedbackRepository = feedbackRepository;
        this.surveySendRepository = surveySendRepository;
        this.visitorRepository = visitorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> listFeedback(Long visitorId, Instant from, Instant to) {
        List<Feedback> feedback;
        if (visitorId != null) {
            feedback = feedbackRepository.findByVisitorId(visitorId);
        } else if (from != null && to != null) {
            feedback = feedbackRepository.findBySubmittedAtBetween(from, to);
        } else {
            feedback = feedbackRepository.findAllByOrderBySubmittedAtDesc();
        }
        return feedback.stream()
                .map(FeedbackResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FeedbackResponse submitFeedback(FeedbackRequest request) {
        Visitor visitor = getVisitorEntity(request.getVisitorId());

        Feedback feedback = new Feedback();
        feedback.setVisitor(visitor);
        feedback.setRating(request.getRating());
        feedback.setBriefingClear(request.getBriefingClear());
        feedback.setEducationalValue(request.getEducationalValue());
        feedback.setRecommend(request.getRecommend());
        feedback.setComment(request.getComment());
        feedback.setSubmittedAt(Instant.now());

        if (request.getSurveyId() != null) {
            SurveySend survey = getSurveyEntity(request.getSurveyId());
            if (!survey.getVisitor().getId().equals(visitor.getId())) {
                throw new BusinessRuleException("Survey " + survey.getId()
                        + " was sent to a different visitor");
            }
            if (survey.getStatus() != SurveyStatus.SENT) {
                throw new BusinessRuleException("Survey " + survey.getId()
                        + " is already received (status: " + survey.getStatus() + ")");
            }
            feedback.setOrigin(survey.getChannel());
            feedback.setSurveySend(survey);
            survey.setStatus(SurveyStatus.RECEIVED);
            survey.setFeedback(feedback);
            surveySendRepository.save(survey);
        } else {
            feedback.setOrigin(FeedbackChannel.ON_SITE);
            feedback.setSurveySend(null);
        }

        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackSummaryResponse summary(Instant from, Instant to) {
        List<Feedback> feedback;
        if (from != null && to != null) {
            feedback = feedbackRepository.findBySubmittedAtBetween(from, to);
        } else {
            feedback = feedbackRepository.findAll();
        }
        if (feedback.isEmpty()) {
            return FeedbackSummaryResponse.of(0.0, 0, 0.0, emptyDistribution());
        }

        double average = feedback.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);

        long withRecommendation = feedback.stream()
                .filter(f -> f.getRecommend() != null && !f.getRecommend().isBlank())
                .count();

        Map<Integer, Long> distribution = new TreeMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        feedback.forEach(f -> distribution.merge(f.getRating(), 1L, Long::sum));

        return FeedbackSummaryResponse.of(
                Math.round(average * 100.0) / 100.0,
                feedback.size(),
                Math.round(withRecommendation * 100.0 / feedback.size() * 100.0) / 100.0,
                distribution);
    }

    @Override
    @Transactional
    public FeedbackResponse routeFeedback(Long id, RouteFeedbackRequest request) {
        Feedback feedback = getFeedbackEntity(id);
        feedback.setRoutedTo(request.getRoutedTo());
        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurveyResponse> listSurveys(Long visitorId, SurveyStatus status) {
        List<SurveySend> surveys;
        if (visitorId != null) {
            surveys = surveySendRepository.findByVisitorId(visitorId);
        } else if (status != null) {
            surveys = surveySendRepository.findByStatus(status);
        } else {
            surveys = surveySendRepository.findAll();
        }
        return surveys.stream()
                .map(SurveyResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SurveyResponse sendSurvey(SurveyRequest request) {
        if (request.getChannel() == FeedbackChannel.ON_SITE) {
            throw new BusinessRuleException("ON_SITE surveys cannot be sent: "
                    + "on-site responses are submitted directly via the feedback endpoint");
        }
        Visitor visitor = getVisitorEntity(request.getVisitorId());

        SurveySend survey = new SurveySend();
        survey.setVisitor(visitor);
        survey.setChannel(request.getChannel());
        survey.setMessageTemplate(request.getMessageTemplate());
        survey.setSentAt(Instant.now());
        survey.setStatus(SurveyStatus.SENT);
        return SurveyResponse.from(surveySendRepository.save(survey));
    }

    private Feedback getFeedbackEntity(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback " + id + " not found"));
    }

    private SurveySend getSurveyEntity(Long id) {
        return surveySendRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey " + id + " not found"));
    }

    private Visitor getVisitorEntity(Long id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor " + id + " not found"));
    }

    private Map<Integer, Long> emptyDistribution() {
        Map<Integer, Long> map = new TreeMap<>();
        for (int i = 1; i <= 5; i++) {
            map.put(i, 0L);
        }
        return Collections.unmodifiableMap(map);
    }
}