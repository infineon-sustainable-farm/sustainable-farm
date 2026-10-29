package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "water_visitor_events")
public class WaterVisitorEvent extends BaseEntity {
    @Column(nullable = false, unique = true, length = 128)
    private String visitId;
    private String groupName;
    @Column(nullable = false)
    private Instant expectedAt;
    @Column(columnDefinition = "text")
    private String zones;
    /** Stop names as sent by Visitor Management, mapped or not. */
    @Column(columnDefinition = "text")
    private String stops;
    @Column(nullable = false)
    private Boolean conflict = false;

    public String getVisitId() { return visitId; }
    public void setVisitId(String visitId) { this.visitId = visitId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Instant getExpectedAt() { return expectedAt; }
    public void setExpectedAt(Instant expectedAt) { this.expectedAt = expectedAt; }
    public String getZones() { return zones; }
    public void setZones(String zones) { this.zones = zones; }
    public String getStops() { return stops; }
    public void setStops(String stops) { this.stops = stops; }
    public Boolean getConflict() { return conflict; }
    public void setConflict(Boolean conflict) { this.conflict = conflict; }
}
