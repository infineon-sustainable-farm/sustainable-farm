package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Visitor Management tour stop (identified by its name only) that passes by water facilities,
 * mapped by the water team to the zone and/or the source it shows.
 */
@Entity
@Table(name = "water_visitor_stops")
public class WaterVisitorStop extends BaseEntity {
    @Column(name = "stop_name", nullable = false, unique = true)
    private String stopName;
    private UUID zoneId;
    private UUID sourceId;

    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public UUID getZoneId() { return zoneId; }
    public void setZoneId(UUID zoneId) { this.zoneId = zoneId; }
    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
}
