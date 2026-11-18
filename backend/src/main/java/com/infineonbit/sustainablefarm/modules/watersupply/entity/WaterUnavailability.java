package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "water_unavailability", indexes = {
        @Index(name = "idx_water_unavailability_scope", columnList = "scope, scope_id"),
        @Index(name = "idx_water_unavailability_period", columnList = "start_at, end_at")
})
@Check(name = "chk_water_unavailability_scope", constraints = "scope IN ('farm', 'source', 'zone')")
@Check(name = "chk_water_unavailability_status", constraints = "status IN ('planned', 'active', 'cancelled')")
@Check(name = "chk_water_unavailability_period", constraints = "end_at > start_at")
public class WaterUnavailability extends BaseEntity {
    @Convert(converter = UnavailabilityScope.Converter.class)
    @Column(nullable = false, length = 16)
    private UnavailabilityScope scope;
    /**
     * Id of the farm, water source or zone named by {@link #scope}: polymorphic, so no foreign key;
     * the service checks the target exists on every write.
     */
    @Column(nullable = false)
    private Long scopeId;
    @Column(nullable = false)
    private Instant startAt;
    @Column(nullable = false)
    private Instant endAt;
    private String reason;
    @Convert(converter = UnavailabilityStatus.Converter.class)
    @Column(nullable = false, length = 16)
    private UnavailabilityStatus status;

    public UnavailabilityScope getScope() { return scope; }
    public void setScope(UnavailabilityScope scope) { this.scope = scope; }
    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long scopeId) { this.scopeId = scopeId; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public UnavailabilityStatus getStatus() { return status; }
    public void setStatus(UnavailabilityStatus status) { this.status = status; }
}
