package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request to create a registration (link a visitor to a time slot).
 */
public class RegistrationRequest {

    @NotNull(message = "visitorId is required")
    private Long visitorId;

    @NotNull(message = "timeSlotId is required")
    private Long timeSlotId;

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

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
