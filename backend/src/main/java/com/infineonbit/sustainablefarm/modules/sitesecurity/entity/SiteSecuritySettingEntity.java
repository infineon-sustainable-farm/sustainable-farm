package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A module setting changed at runtime by an admin (for example the retirement of email-code sign-in). */
@Entity
@Table(name = "site_security_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSecuritySettingEntity {
    @Id
    @Column(name = "setting_key", length = 64)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 64)
    private String value;

    @Column(nullable = false, length = 36)
    private String updatedBy;

    @Column(nullable = false)
    private Instant updatedAt;
}
