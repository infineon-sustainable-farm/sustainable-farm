package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_unavailability")
public class WaterUnavailability extends BaseEntity {
    @Column(nullable = false, length = 16)
    private String scope;
    @Column(nullable = false)
    private UUID scopeId;
    @Column(nullable = false)
    private Instant startAt;
    @Column(nullable = false)
    private Instant endAt;
    private String reason;
    @Column(nullable = false, length = 16)
    private String status;

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public UUID getScopeId() { return scopeId; }
    public void setScopeId(UUID scopeId) { this.scopeId = scopeId; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
