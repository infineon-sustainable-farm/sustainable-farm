package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demand_reports")
public class DemandReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Integer reportId;

    @Column(name = "report_type")
    private String reportType;

    @NotNull(message = "periodStart is required")
    @Column(name = "period_start")
    private LocalDate periodStart;

    @NotNull(message = "periodEnd is required")
    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "total_forecasted_volume_t")
    private BigDecimal totalForecastedVolumeT;

    @Column(name = "total_actual_volume_t")
    private BigDecimal totalActualVolumeT;

    @Column(name = "variance_pct")
    private BigDecimal variancePct;

    @Column(name = "status")
    private String status;

    @Column(name = "is_scheduled")
    private Boolean isScheduled;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "schedule_frequency")
    private String scheduleFrequency;

    @Column(name = "summary")
    private String summary;

    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getTotalForecastedVolumeT() {
        return totalForecastedVolumeT;
    }

    public void setTotalForecastedVolumeT(BigDecimal totalForecastedVolumeT) {
        this.totalForecastedVolumeT = totalForecastedVolumeT;
    }

    public BigDecimal getTotalActualVolumeT() {
        return totalActualVolumeT;
    }

    public void setTotalActualVolumeT(BigDecimal totalActualVolumeT) {
        this.totalActualVolumeT = totalActualVolumeT;
    }

    public BigDecimal getVariancePct() {
        return variancePct;
    }

    public void setVariancePct(BigDecimal variancePct) {
        this.variancePct = variancePct;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsScheduled() {
        return isScheduled;
    }

    public void setIsScheduled(Boolean isScheduled) {
        this.isScheduled = isScheduled;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public String getScheduleFrequency() {
        return scheduleFrequency;
    }

    public void setScheduleFrequency(String scheduleFrequency) {
        this.scheduleFrequency = scheduleFrequency;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}