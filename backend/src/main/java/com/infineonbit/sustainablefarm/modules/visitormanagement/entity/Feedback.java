package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * A visitor's satisfaction response (on-site tablet wrap-up stop, or a
 * response to a post-visit survey sent by email/SMS/WhatsApp). Answers
 * mirror the confirmed mockup questions: overall rating (1-5), briefing
 * clarity, educational value, recommendation and free comment.
 */
@Entity
@Table(name = "feedback")
public class Feedback extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_send_id")
    private SurveySend surveySend;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20)
    private FeedbackChannel origin;

    @Min(1)
    @Max(5)
    @Column(name = "rating", nullable = false)
    private int rating;

    @Size(max = 1000)
    @Column(name = "briefing_clear", length = 1000)
    private String briefingClear;

    @Size(max = 1000)
    @Column(name = "educational_value", length = 1000)
    private String educationalValue;

    @Size(max = 1000)
    @Column(name = "recommend", length = 1000)
    private String recommend;

    @Size(max = 1000)
    @Column(name = "comment", length = 1000)
    private String comment;

    @Size(max = 60)
    @Column(name = "routed_to", length = 60)
    private String routedTo;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public Visitor getVisitor() {
        return visitor;
    }

    public void setVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    public SurveySend getSurveySend() {
        return surveySend;
    }

    public void setSurveySend(SurveySend surveySend) {
        this.surveySend = surveySend;
    }

    public FeedbackChannel getOrigin() {
        return origin;
    }

    public void setOrigin(FeedbackChannel origin) {
        this.origin = origin;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
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

    public String getRoutedTo() {
        return routedTo;
    }

    public void setRoutedTo(String routedTo) {
        this.routedTo = routedTo;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}