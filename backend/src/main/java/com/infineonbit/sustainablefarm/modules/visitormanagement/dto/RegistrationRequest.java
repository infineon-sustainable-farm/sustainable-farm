package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitPurpose;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create a registration (link a visitor to a time slot).
 */
public class RegistrationRequest {

    @NotNull(message = "visitorId is required")
    private Long visitorId;

    @NotNull(message = "timeSlotId is required")
    private Long timeSlotId;

    @NotNull(message = "visitPurpose is required")
    private VisitPurpose visitPurpose;

    private Long eventId;

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public Long getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(Long timeSlotId) {
        this.timeSlotId = timeSlotId;
    }

    public VisitPurpose getVisitPurpose() {
        return visitPurpose;
    }

    public void setVisitPurpose(VisitPurpose visitPurpose) {
        this.visitPurpose = visitPurpose;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
