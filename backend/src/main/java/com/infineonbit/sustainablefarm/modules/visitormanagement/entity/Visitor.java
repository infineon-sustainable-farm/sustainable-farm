package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A visitor or visiting group — the single source of truth on who comes
 * (Registration sub-module).
 */
@Entity
@Table(name = "visitor")
public class Visitor extends BaseEntity {

    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Min(1)
    @Column(name = "group_size", nullable = false)
    private int groupSize = 1;

    @Email
    @Size(max = 200)
    @Column(name = "email", length = 200)
    private String email;

    @Size(max = 30)
    @Column(name = "phone", length = 30)
    private String phone;

    @Size(max = 40)
    @Column(name = "language", length = 40)
    private String language;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "visitor_type", nullable = false, length = 20)
    private VisitorType type = VisitorType.INDIVIDUAL;

    @Size(max = 500)
    @Column(name = "special_needs", length = 500)
    private String specialNeeds;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int groupSize) {
        this.groupSize = groupSize;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public VisitorType getType() {
        return type;
    }

    public void setType(VisitorType type) {
        this.type = type;
    }

    public String getSpecialNeeds() {
        return specialNeeds;
    }

    public void setSpecialNeeds(String specialNeeds) {
        this.specialNeeds = specialNeeds;
    }
}
