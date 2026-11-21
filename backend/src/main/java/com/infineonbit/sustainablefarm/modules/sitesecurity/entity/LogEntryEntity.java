package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One passage at a gate. The person is a reference to the shared people directory, empty for
 * an unidentified subject (an invalid badge, for example); the name is never copied here.
 */
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

    /** The person who passed, or empty when the subject was not identified. */
    @Column(name = "person_id")
    private Long personId;

    private String type;
    private String zone;
    private String zoneName;
    private String action;
    private String status;
    private String gate;
    private String method;
    private String note;
}
