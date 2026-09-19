package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for an agritourism booking.
 */
public class BookingRequest {

    @NotNull(message = "activityId is required")
    private Long activityId;

    @NotNull(message = "timeSlotId is required")
    private Long timeSlotId;

    @NotBlank(message = "visitorFullName is required")
    @Size(max = 150, message = "visitorFullName must be at most 150 characters")
    private String visitorFullName;

    @NotBlank(message = "visitorEmail is required")
    @Email(message = "visitorEmail must be a valid email")
    @Size(max = 255, message = "visitorEmail must be at most 255 characters")
    private String visitorEmail;

    @Size(max = 30, message = "visitorPhone must be at most 30 characters")
    private String visitorPhone;

    @NotNull(message = "peopleCount is required")
    @Min(value = 1, message = "peopleCount must be at least 1")
    private Integer peopleCount;

    private BookingPaymentMethod paymentMethod;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(Long timeSlotId) {
        this.timeSlotId = timeSlotId;
    }

    public String getVisitorFullName() {
        return visitorFullName;
    }

    public void setVisitorFullName(String visitorFullName) {
        this.visitorFullName = visitorFullName;
    }

    public String getVisitorEmail() {
        return visitorEmail;
    }

    public void setVisitorEmail(String visitorEmail) {
        this.visitorEmail = visitorEmail;
    }

    public String getVisitorPhone() {
        return visitorPhone;
    }

    public void setVisitorPhone(String visitorPhone) {
        this.visitorPhone = visitorPhone;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public BookingPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(BookingPaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}