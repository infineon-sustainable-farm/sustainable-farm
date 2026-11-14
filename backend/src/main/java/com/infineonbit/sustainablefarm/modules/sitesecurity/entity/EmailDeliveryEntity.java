package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * One email of the accounts feature. Two independent questions are tracked:
 * did our mail provider accept it (submission), and did it reach the mailbox
 * (delivery: only provider evidence or an admin's attestation may say so).
 * Neither changes the account or invitation status.
 */
@Entity
@Table(name = "site_security_email_deliveries",
        indexes = {
                @Index(name = "idx_site_security_email_deliveries_account", columnList = "accountId,createdAt"),
                @Index(name = "idx_site_security_email_deliveries_retry", columnList = "submissionStatus,nextAttemptAt")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDeliveryEntity {
    /** Also the correlation id put in the message (Message-ID and X-Farm-Security-Delivery). */
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String accountId;

    /** The link the email carries, when it carries one. */
    @Column(length = 36)
    private String tokenId;

    @Column(nullable = false, length = 24)
    private String kind;

    /** The exact address the email was sent to. */
    @ToString.Exclude
    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(nullable = false, length = 24)
    private String submissionStatus;

    @Column(nullable = false)
    private int attempts;

    private Instant lastAttemptAt;

    private Instant nextAttemptAt;

    /** A class of error (for example SMTP_4XX, AUTH, CONNECTION), never the server's text. */
    @Column(length = 32)
    private String lastError;

    @Column(length = 255)
    private String messageId;

    private Instant submittedAt;

    @Column(nullable = false, length = 24)
    private String deliveryStatus;

    private Instant deliveryConfirmedAt;

    @Column(length = 36)
    private String deliveryConfirmedBy;

    @Column(length = 200)
    private String deliveryEvidence;

    @Column(nullable = false)
    private Instant createdAt;
}
