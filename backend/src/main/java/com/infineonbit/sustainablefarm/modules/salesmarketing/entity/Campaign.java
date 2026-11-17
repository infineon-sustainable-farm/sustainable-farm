package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "campaigns")
@Check(name = "campaigns_status_check", constraints = "status IN ('active', 'scheduled', 'ended', 'draft')")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    private Integer campaignId;

    @NotBlank(message = "Campaign name is required")
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @ColumnDefault("'scheduled'")
    @Column(name = "status", length = 20)
    private String status;

    @PositiveOrZero(message = "budgetEur cannot be negative")
    @Column(name = "budget_eur", precision = 10, scale = 2)
    private BigDecimal budgetEur;

    @Column(name = "budget_spent_eur", precision = 10, scale = 2)
    private BigDecimal budgetSpentEur;

    @Column(name = "estimated_reach")
    private Integer estimatedReach;

    @ColumnDefault("'EUR'")
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Integer campaignId) {
        this.campaignId = campaignId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getBudgetEur() {
        return budgetEur;
    }

    public void setBudgetEur(BigDecimal budgetEur) {
        this.budgetEur = budgetEur;
    }

    public BigDecimal getBudgetSpentEur() {
        return budgetSpentEur;
    }

    public void setBudgetSpentEur(BigDecimal budgetSpentEur) {
        this.budgetSpentEur = budgetSpentEur;
    }

    public Integer getEstimatedReach() {
        return estimatedReach;
    }

    public void setEstimatedReach(Integer estimatedReach) {
        this.estimatedReach = estimatedReach;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}