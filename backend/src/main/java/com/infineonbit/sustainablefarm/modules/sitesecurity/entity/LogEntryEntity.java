package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "site_security_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntryEntity {
    @Id
    private String id;
    private String ref;
    private String time;
    private String date;
    @Column(name = "user_name")
    private String user;
    private String initials;
    private String type;
    private String zone;
    private String zoneName;
    private String action;
    private String status;
    private String gate;
    private String method;
    private String note;
}
