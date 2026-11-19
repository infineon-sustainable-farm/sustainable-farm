package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.FeedbackChannel;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveySend;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveyStatus;

import java.time.Instant;

/**
 * API representation of a sent survey, including the visitor reference and,
 * once the visitor responded, the rating of the linked feedback.
 */
public class SurveyResponse {

    private Long id;
    private Long visitorId;
    private String visitorName;
    private FeedbackChannel channel;
    private String messageTemplate;
    private Instant sentAt;
    private SurveyStatus status;
    private Integer rating;
    private Instant createdAt;
    private Instant updatedAt;

    public static SurveyResponse from(SurveySend survey) {
        SurveyResponse r = new SurveyResponse();
        r.id = survey.getId();
        r.visitorId = survey.getVisitor().getId();
        r.visitorName = survey.getVisitor().getFullName();
        r.channel = survey.getChannel();
        r.messageTemplate = survey.getMessageTemplate();
        r.sentAt = survey.getSentAt();
        r.status = survey.getStatus();
        r.rating = survey.getFeedback() == null ? null : survey.getFeedback().getRating();
        r.createdAt = survey.getCreatedAt();
        r.updatedAt = survey.getUpdatedAt();
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

    public FeedbackChannel getChannel() {
        return channel;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public SurveyStatus getStatus() {
        return status;
    }

    public Integer getRating() {
        return rating;
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
    public void setChannel(FeedbackChannel channel) { this.channel = channel; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public void setStatus(SurveyStatus status) { this.status = status; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}