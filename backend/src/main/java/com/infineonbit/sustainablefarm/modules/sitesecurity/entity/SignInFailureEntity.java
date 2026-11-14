package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * One failed password sign-in, for the sign-in limits. Neither the email nor the
 * client address is stored: only keyed hashes of them (HMAC with a key that lives
 * in memory and changes at every start, so the rows cannot be reversed and are
 * meaningless after a restart, which only shortens a 15-minute window). The same
 * row is written whether or not the email has an account. Rows older than a day
 * are deleted.
 */
@Entity
@Table(name = "site_security_sign_in_failures", indexes = {
        @Index(name = "idx_site_security_sign_in_failures_email", columnList = "emailKey,occurredAt"),
        @Index(name = "idx_site_security_sign_in_failures_client", columnList = "clientKey,occurredAt")})
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInFailureEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 64)
    private String emailKey;

    @Column(nullable = false, length = 64)
    private String clientKey;

    /** The trusted device the attempt came from, if any. */
    @Column(length = 36)
    private String deviceId;

    @Column(nullable = false)
    private Instant occurredAt;
}
