package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitPurpose;
import jakarta.validation.constraints.NotNull;

/**
 * Request to register a visitor for an event. The visitor's group size is
 * taken from the visitor record for capacity checks.
 */
public class EventRegistrationRequest {

    @NotNull(message = "visitorId is required")
    private Long visitorId;

    @NotNull(message = "visitPurpose is required")
    private VisitPurpose visitPurpose;

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public VisitPurpose getVisitPurpose() {
        return visitPurpose;
    }

    public void setVisitPurpose(VisitPurpose visitPurpose) {
        this.visitPurpose = visitPurpose;
    }
}