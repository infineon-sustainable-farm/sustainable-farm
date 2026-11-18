package com.infineonbit.sustainablefarm.modules.notification.api;

import java.time.Instant;

/**
 * The writable attributes of an alert, to raise it or to replace them. Every attribute is
 * optional except where noted; a module fills the ones its alerts carry.
 *
 * @param code              business code, unique among all alerts (sales {@code alertCode})
 * @param type              kind of alert, free text of the module ("High Demand", "warning")
 * @param severity          how serious the alert is
 * @param status            where the alert stands; {@code null} means {@link AlertStatus#OPEN}
 * @param title             short title
 * @param message           detailed message
 * @param relatedEntityType table or kind of the entity the alert is about ("energy_component")
 * @param relatedEntityId   key of that entity, as text so that any key type fits
 * @param region            market region (sales)
 * @param origin            product origin (sales)
 * @param recipientPersonId the person of the staff referential the alert is addressed to
 * @param read              whether the recipient has read it; {@code null} means unread
 * @param actionUrl         application route that handles the alert
 * @param raisedAt          when the alert happened; {@code null} means now
 * @param resolvedAt        when the alert was resolved
 */
public record AlertDetails(
        String code,
        String type,
        AlertSeverity severity,
        AlertStatus status,
        String title,
        String message,
        String relatedEntityType,
        String relatedEntityId,
        String region,
        String origin,
        Long recipientPersonId,
        Boolean read,
        String actionUrl,
        Instant raisedAt,
        Instant resolvedAt) {

    public static Builder builder() {
        return new Builder();
    }

    /** Builds {@link AlertDetails} attribute by attribute. */
    public static final class Builder {
        private String code;
        private String type;
        private AlertSeverity severity;
        private AlertStatus status;
        private String title;
        private String message;
        private String relatedEntityType;
        private String relatedEntityId;
        private String region;
        private String origin;
        private Long recipientPersonId;
        private Boolean read;
        private String actionUrl;
        private Instant raisedAt;
        private Instant resolvedAt;

        private Builder() {
        }

        public Builder code(String code) { this.code = code; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder severity(AlertSeverity severity) { this.severity = severity; return this; }
        public Builder status(AlertStatus status) { this.status = status; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public Builder relatedEntity(String type, String id) {
            this.relatedEntityType = type;
            this.relatedEntityId = id;
            return this;
        }

        public Builder region(String region) { this.region = region; return this; }
        public Builder origin(String origin) { this.origin = origin; return this; }
        public Builder recipientPersonId(Long personId) { this.recipientPersonId = personId; return this; }
        public Builder read(Boolean read) { this.read = read; return this; }
        public Builder actionUrl(String actionUrl) { this.actionUrl = actionUrl; return this; }
        public Builder raisedAt(Instant raisedAt) { this.raisedAt = raisedAt; return this; }
        public Builder resolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; return this; }

        public AlertDetails build() {
            return new AlertDetails(code, type, severity, status, title, message, relatedEntityType,
                    relatedEntityId, region, origin, recipientPersonId, read, actionUrl, raisedAt, resolvedAt);
        }
    }
}
