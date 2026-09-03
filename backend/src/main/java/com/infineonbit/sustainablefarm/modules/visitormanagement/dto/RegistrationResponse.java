package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Registration;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.RegistrationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API representation of a registration, including lightweight references
 * to the linked visitor and time slot.
 */
public class RegistrationResponse {

    private Long id;
    private Long visitorId;
    private String visitorName;
    private int groupSize;
    private Long timeSlotId;
    private LocalDate slotDate;
    private LocalTime slotStart;
    private Long eventId;
    private RegistrationStatus status;
    private Long briefingId;
    private Instant createdAt;
    private Instant updatedAt;

    public static RegistrationResponse from(Registration reg) {
        RegistrationResponse r = new RegistrationResponse();
        r.id = reg.getId();
        r.visitorId = reg.getVisitor().getId();
        r.visitorName = reg.getVisitor().getFullName();
        r.groupSize = reg.getVisitor().getGroupSize();
        r.timeSlotId = reg.getTimeSlot().getId();
        r.slotDate = reg.getTimeSlot().getDate();
        r.slotStart = reg.getTimeSlot().getStartTime();
        r.eventId = reg.getEventId();
        r.status = reg.getStatus();
        if (reg.getBriefing() != null) {
            r.briefingId = reg.getBriefing().getId();
        }
        r.createdAt = reg.getCreatedAt();
        r.updatedAt = reg.getUpdatedAt();
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

    public int getGroupSize() {
        return groupSize;
    }

    public Long getTimeSlotId() {
        return timeSlotId;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public LocalTime getSlotStart() {
        return slotStart;
    }

    public Long getEventId() {
        return eventId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public Long getBriefingId() {
        return briefingId;
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
    public void setGroupSize(int groupSize) { this.groupSize = groupSize; }
    public void setTimeSlotId(Long timeSlotId) { this.timeSlotId = timeSlotId; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public void setSlotStart(LocalTime slotStart) { this.slotStart = slotStart; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public void setBriefingId(Long briefingId) { this.briefingId = briefingId; }
}
