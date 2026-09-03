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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * Links a visitor to a time slot. A confirmed registration triggers the
 * generation of a safety briefing (Registration sub-module).
 */
@Entity
@Table(name = "registration",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_registration_visitor_timeslot",
                columnNames = {"visitor_id", "time_slot_id"}))
public class Registration extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Column(name = "event_id")
    private Long eventId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visit_purpose", nullable = false, length = 20)
    private VisitPurpose visitPurpose;

    @Column(name = "is_prospect", nullable = false)
    private boolean isProspect = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @OneToOne(mappedBy = "registration", fetch = FetchType.LAZY)
    private Briefing briefing;

    public Visitor getVisitor() {
        return visitor;
    }

    public void setVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public VisitPurpose getVisitPurpose() {
        return visitPurpose;
    }

    public void setVisitPurpose(VisitPurpose visitPurpose) {
        this.visitPurpose = visitPurpose;
    }

    public boolean isProspect() {
        return isProspect;
    }

    public void setProspect(boolean isProspect) {
        this.isProspect = isProspect;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public Briefing getBriefing() {
        return briefing;
    }

    public void setBriefing(Briefing briefing) {
        this.briefing = briefing;
    }
}
