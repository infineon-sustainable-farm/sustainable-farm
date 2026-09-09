package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Feedback;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.FeedbackChannel;

import java.time.Instant;

/**
 * API representation of a feedback response, with lightweight reference to
 * the visitor and, when present, the linked sent survey.
 */
public class FeedbackResponse {

    private Long id;
    private Long visitorId;
    private String visitorName;
    private Long surveyId;
    private FeedbackChannel origin;
    private int rating;
    private String briefingClear;
    private String educationalValue;
    private String recommend;
    private String comment;
    private String routedTo;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static FeedbackResponse from(Feedback feedback) {
        FeedbackResponse r = new FeedbackResponse();
        r.id = feedback.getId();
        r.visitorId = feedback.getVisitor().getId();
        r.visitorName = feedback.getVisitor().getFullName();
        r.surveyId = feedback.getSurveySend() == null ? null : feedback.getSurveySend().getId();
        r.origin = feedback.getOrigin();
        r.rating = feedback.getRating();
        r.briefingClear = feedback.getBriefingClear();
        r.educationalValue = feedback.getEducationalValue();
        r.recommend = feedback.getRecommend();
        r.comment = feedback.getComment();
        r.routedTo = feedback.getRoutedTo();
        r.submittedAt = feedback.getSubmittedAt();
        r.createdAt = feedback.getCreatedAt();
        r.updatedAt = feedback.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public FeedbackChannel getOrigin() {
        return origin;
    }

    public int getRating() {
        return rating;
    }

    public String getBriefingClear() {
        return briefingClear;
    }

    public String getEducationalValue() {
        return educationalValue;
    }

    public String getRecommend() {
        return recommend;
    }

    public String getComment() {
        return comment;
    }

    public String getRoutedTo() {
        return routedTo;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setVisitorId(Long visitorId) { this.visitorId = visitorId; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public void setOrigin(FeedbackChannel origin) { this.origin = origin; }
    public void setRating(int rating) { this.rating = rating; }
    public void setBriefingClear(String briefingClear) { this.briefingClear = briefingClear; }
    public void setEducationalValue(String educationalValue) { this.educationalValue = educationalValue; }
    public void setRecommend(String recommend) { this.recommend = recommend; }
    public void setComment(String comment) { this.comment = comment; }
    public void setRoutedTo(String routedTo) { this.routedTo = routedTo; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}