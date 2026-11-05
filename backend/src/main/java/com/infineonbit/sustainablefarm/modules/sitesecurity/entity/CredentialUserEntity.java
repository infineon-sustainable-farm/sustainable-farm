package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
