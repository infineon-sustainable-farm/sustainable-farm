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
 * One wrong sign-in code, for one operator. Only the operator id and the time
 * are kept: never the code that was tried. Rows older than 24 hours are removed
 * when the operator next asks for a code.
 */
@Entity
@Table(name = "site_security_failed_sign_ins",
        indexes = @Index(name = "idx_site_security_failed_sign_ins_operator", columnList = "operatorId,occurredAt"))
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedSignInEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String operatorId;

    @Column(nullable = false)
    private Instant occurredAt;

    /** CODE (email code) or PASSWORD. Empty on rows written before passwords existed: those were codes. */
    @Column(length = 16)
    private String kind;

    public static final String CODE = "CODE";
    public static final String PASSWORD = "PASSWORD";
}
