package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Safety briefing attached to each registration (1:1). Delivered by staff
 * before site access; gates access strictly per the Site Security module.
 */
@Entity
@Table(name = "briefing",
        indexes = @Index(name = "idx_briefing_staff_member_person_id", columnList = "staff_member_person_id"))
public class Briefing extends BaseEntity {

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private Registration registration;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /** Person of the staff referential who delivered the briefing. */
    @Column(name = "staff_member_person_id")
    private Long staffMemberPersonId;

    @Size(max = 500)
    @Column(name = "signature", length = 500)
    private String signature;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BriefingStatus status = BriefingStatus.PENDING;

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Long getStaffMemberPersonId() {
        return staffMemberPersonId;
    }

    public void setStaffMemberPersonId(Long staffMemberPersonId) {
        this.staffMemberPersonId = staffMemberPersonId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public BriefingStatus getStatus() {
        return status;
    }

    public void setStatus(BriefingStatus status) {
        this.status = status;
    }
}
