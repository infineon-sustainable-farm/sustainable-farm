package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to submit a feedback response. If {@code surveyId} is provided the
 * response is linked to a previously sent post-visit survey (and that survey
 * becomes RECEIVED); otherwise the response is recorded as an on-site tablet
 * submission. An on-site response is identified either by {@code visitorId}
 * (a known visitor) or by {@code visitorName} (typed by the visitor on the
 * tablet).
 */
public class FeedbackRequest {

    private Long visitorId;

    @Size(max = 150, message = "visitorName must be at most 150 characters")
    private String visitorName;

    private Long surveyId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 1000, message = "briefingClear must be at most 1000 characters")
    private String briefingClear;

    @Size(max = 1000, message = "educationalValue must be at most 1000 characters")
    private String educationalValue;

    @Size(max = 1000, message = "recommend must be at most 1000 characters")
    private String recommend;

    @Size(max = 1000, message = "comment must be at most 1000 characters")
    private String comment;

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getBriefingClear() {
        return briefingClear;
    }

    public void setBriefingClear(String briefingClear) {
        this.briefingClear = briefingClear;
    }

    public String getEducationalValue() {
        return educationalValue;
    }

    public void setEducationalValue(String educationalValue) {
        this.educationalValue = educationalValue;
    }

    public String getRecommend() {
        return recommend;
    }

    public void setRecommend(String recommend) {
        this.recommend = recommend;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}