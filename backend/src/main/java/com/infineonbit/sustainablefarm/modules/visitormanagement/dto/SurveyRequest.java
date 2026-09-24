package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.FeedbackChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to send a post-visit survey to a visitor over a given channel.
 * ON_SITE is not a valid channel here: on-site responses are submitted
 * directly through the feedback endpoint.
 */
public class SurveyRequest {

    @NotNull(message = "visitorId is required")
    private Long visitorId;

    @NotNull(message = "channel is required")
    private FeedbackChannel channel;

    @Size(max = 500, message = "messageTemplate must be at most 500 characters")
    private String messageTemplate;

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public FeedbackChannel getChannel() {
        return channel;
    }

    public void setChannel(FeedbackChannel channel) {
        this.channel = channel;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }
}