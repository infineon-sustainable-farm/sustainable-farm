package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A demand report over a period. Its type and schedule frequency are the free labels the
 * interface sends ({@code Forecasting}, {@code Weekly}).
 */
@Entity
@Table(name = "demand_reports", indexes = @Index(name = "idx_reports_period", columnList = "period_start, period_end"))
@Getter
@Setter
public class DemandReport extends BaseEntity {

    /** {@code weekly} when none is sent. */
    @ColumnDefault("'weekly'")
    @Column(name = "report_type", nullable = false, length = 40)
    private String reportType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_forecasted_volume_t", precision = 10, scale = 2)
    private BigDecimal totalForecastedVolumeT;

    @Column(name = "total_actual_volume_t", precision = 10, scale = 2)
    private BigDecimal totalActualVolumeT;

    @Column(name = "variance_pct", precision = 5, scale = 2)
    private BigDecimal variancePct;

    @ColumnDefault("'ready'")
    @Convert(converter = DemandReportStatus.Converter.class)
    @Column(name = "status", length = 20)
    private DemandReportStatus status;

    @ColumnDefault("false")
    @Column(name = "is_scheduled")
    private Boolean isScheduled;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "schedule_frequency", length = 20)
    private String scheduleFrequency;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    /** Person of the people directory (staff module) who generated the report, or {@code null}. */
    @Column(name = "generated_by_person_id")
    private Long generatedByPersonId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    void fillGeneratedAt() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
