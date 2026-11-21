package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A physical site access granted to one person of the shared people directory (ADR 0004).
 * The name and email are those of the {@code person} row, read through
 * {@code staff.api.PersonDirectory}: they are not copied here.
 */
@Entity
@Table(name = "site_security_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_site_security_users_person_id", columnNames = "person_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialUserEntity {
    @Id
    private String id;

    /** The holder, a person of the shared directory. */
    @Column(name = "person_id", nullable = false)
    private Long personId;

    /** The holder's function on site, for example "Facilities Tech". */
    private String role;
    private String type;
    private String level;
    private String validUntil;
    private String status;
    private String lastActive;

    /** Result of the latest email; empty when the person has no email. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private CredentialNotificationStatus notificationStatus;
}
