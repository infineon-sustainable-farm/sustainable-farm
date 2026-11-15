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
import lombok.ToString;

@Entity
@Table(name = "site_security_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_site_security_users_name", columnNames = "name"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialUserEntity {
    @Id
    private String id;
    private String name;
    private String role;
    private String type;
    private String level;
    private String validUntil;
    private String status;
    private String initials;
    private String lastActive;

    /** Empty for credentials created before emails were collected. */
    @Column(length = 254)
    @ToString.Exclude
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private CredentialNotificationStatus notificationStatus;
}
