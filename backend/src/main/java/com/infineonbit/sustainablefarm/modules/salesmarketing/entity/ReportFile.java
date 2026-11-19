package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** A file of a demand report, deleted with the report. */
@Entity
@Table(name = "report_files", indexes = @Index(name = "idx_report_files_report", columnList = "report_id"))
@Getter
@Setter
public class ReportFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, foreignKey = @ForeignKey(name = "report_files_report_id_fkey"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DemandReport report;

    @Column(name = "format", nullable = false, length = 10)
    private String format;

    @Column(name = "file_url")
    private String fileUrl;
}
