package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Staff;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.StaffRole;

import java.time.Instant;

/**
 * API representation of a staff member / guide (A5).
 */
public class StaffResponse {

    private Long id;
    private String fullName;
    private StaffRole role;
    private String email;
    private String phone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static StaffResponse from(Staff s) {
        StaffResponse r = new StaffResponse();
        r.id = s.getId();
        r.fullName = s.getFullName();
        r.role = s.getRole();
        r.email = s.getEmail();
        r.phone = s.getPhone();
        r.active = s.isActive();
        r.createdAt = s.getCreatedAt();
        r.updatedAt = s.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public StaffRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(StaffRole role) { this.role = role; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setActive(boolean active) { this.active = active; }
}