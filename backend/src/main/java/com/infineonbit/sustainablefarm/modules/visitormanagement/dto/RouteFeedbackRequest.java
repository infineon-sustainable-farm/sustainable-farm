package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to route a feedback response to the responsible module owner
 * (e.g. Sales & Marketing, Energy, Plants). The target is a free-text tag
 * because the VM module must not be coupled to the other business modules.
 */
public class RouteFeedbackRequest {

    @NotBlank(message = "routedTo is required")
    @Size(max = 60, message = "routedTo must be at most 60 characters")
    private String routedTo;

    public String getRoutedTo() {
        return routedTo;
    }

    public void setRoutedTo(String routedTo) {
        this.routedTo = routedTo;
    }
}