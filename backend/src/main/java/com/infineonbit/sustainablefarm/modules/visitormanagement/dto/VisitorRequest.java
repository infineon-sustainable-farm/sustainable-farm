package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitorType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create or update a visitor.
 */
public class VisitorRequest {

    @NotBlank(message = "fullName is required")
    @Size(max = 150, message = "fullName must be at most 150 characters")
    private String fullName;

    @Min(value = 1, message = "groupSize must be at least 1")
    private Integer groupSize = 1;

    @Email(message = "email must be a valid email address")
    @Size(max = 200)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 40)
    private String language;

    @NotNull(message = "type is required")
    private VisitorType type;

    @Size(max = 500)
    private String specialNeeds;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(Integer groupSize) {
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
