package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitorType;

import java.time.Instant;

/**
 * API representation of a visitor.
 */
public class VisitorResponse {

    private Long id;
    private String fullName;
    private int groupSize;
    private String email;
    private String phone;
    private String language;
    private VisitorType type;
    private String specialNeeds;
    private Instant createdAt;
    private Instant updatedAt;

    public static VisitorResponse from(Visitor v) {
        VisitorResponse r = new VisitorResponse();
        r.id = v.getId();
        r.fullName = v.getFullName();
        r.groupSize = v.getGroupSize();
        r.email = v.getEmail();
        r.phone = v.getPhone();
        r.language = v.getLanguage();
        r.type = v.getType();
        r.specialNeeds = v.getSpecialNeeds();
        r.createdAt = v.getCreatedAt();
        r.updatedAt = v.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getLanguage() {
        return language;
    }

    public VisitorType getType() {
        return type;
    }

    public String getSpecialNeeds() {
        return specialNeeds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setGroupSize(int groupSize) { this.groupSize = groupSize; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setLanguage(String language) { this.language = language; }
    public void setType(VisitorType type) { this.type = type; }
    public void setSpecialNeeds(String specialNeeds) { this.specialNeeds = specialNeeds; }
}
