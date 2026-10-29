package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "water_safety_briefing")
public class WaterSafetyBriefing extends BaseEntity {
    @Column(nullable = false, length = 64)
    private String version;
    @Column(columnDefinition = "text")
    private String allowedZones;
    @Column(columnDefinition = "text")
    private String forbiddenZones;
    @Column(columnDefinition = "text")
    private String risks;
    @Column(name = "briefing_text", columnDefinition = "text")
    private String text;
    private Integer maxCapacity;
    @Column(nullable = false)
    private Boolean guideRequired = true;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getAllowedZones() { return allowedZones; }
    public void setAllowedZones(String allowedZones) { this.allowedZones = allowedZones; }
    public String getForbiddenZones() { return forbiddenZones; }
    public void setForbiddenZones(String forbiddenZones) { this.forbiddenZones = forbiddenZones; }
    public String getRisks() { return risks; }
    public void setRisks(String risks) { this.risks = risks; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    public Boolean getGuideRequired() { return guideRequired; }
    public void setGuideRequired(Boolean guideRequired) { this.guideRequired = guideRequired; }
}
