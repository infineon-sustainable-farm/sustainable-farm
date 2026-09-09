package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Booking;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentMethod;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingPaymentStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * API representation of an agritourism booking.
 */
public class BookingResponse {

    private Long id;
    private String reference;
    private Long activityId;
    private String activityName;
    private BigDecimal activityPrice;
    private Long timeSlotId;
    private LocalDate slotDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private String visitorFullName;
    private String visitorEmail;
    private String visitorPhone;
    private Integer peopleCount;
    private BigDecimal totalAmount;
    private BookingPaymentMethod paymentMethod;
    private BookingPaymentStatus paymentStatus;
    private BookingStatus status;
    private Instant reminderScheduledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static BookingResponse from(Booking booking) {
        BookingResponse r = new BookingResponse();
        r.id = booking.getId();
        r.reference = "BK-" + String.format("%05d", booking.getId() == null ? 0 : booking.getId());
        r.activityId = booking.getActivity().getId();
        r.activityName = booking.getActivity().getName();
        r.activityPrice = booking.getActivity().getPrice();
        r.timeSlotId = booking.getTimeSlot().getId();
        r.slotDate = booking.getTimeSlot().getDate();
        r.slotStartTime = booking.getTimeSlot().getStartTime();
        r.slotEndTime = booking.getTimeSlot().getEndTime();
        r.visitorFullName = booking.getVisitorFullName();
        r.visitorEmail = booking.getVisitorEmail();
        r.visitorPhone = booking.getVisitorPhone();
        r.peopleCount = booking.getPeopleCount();
        r.totalAmount = booking.getActivity().getPrice().multiply(BigDecimal.valueOf(booking.getPeopleCount()));
        r.paymentMethod = booking.getPaymentMethod();
        r.paymentStatus = booking.getPaymentStatus();
        r.status = booking.getStatus();
        r.reminderScheduledAt = booking.getReminderScheduledAt();
        r.createdAt = booking.getCreatedAt();
        r.updatedAt = booking.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Long getActivityId() {
        return activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public BigDecimal getActivityPrice() {
        return activityPrice;
    }

    public Long getTimeSlotId() {
        return timeSlotId;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public LocalTime getSlotStartTime() {
        return slotStartTime;
    }

    public LocalTime getSlotEndTime() {
        return slotEndTime;
    }

    public String getVisitorFullName() {
        return visitorFullName;
    }

    public String getVisitorEmail() {
        return visitorEmail;
    }

    public String getVisitorPhone() {
        return visitorPhone;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BookingPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BookingPaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getReminderScheduledAt() {
        return reminderScheduledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) { this.id = id; }
    public void setReference(String reference) { this.reference = reference; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public void setActivityPrice(BigDecimal activityPrice) { this.activityPrice = activityPrice; }
    public void setTimeSlotId(Long timeSlotId) { this.timeSlotId = timeSlotId; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public void setSlotStartTime(LocalTime slotStartTime) { this.slotStartTime = slotStartTime; }
    public void setSlotEndTime(LocalTime slotEndTime) { this.slotEndTime = slotEndTime; }
    public void setVisitorFullName(String visitorFullName) { this.visitorFullName = visitorFullName; }
    public void setVisitorEmail(String visitorEmail) { this.visitorEmail = visitorEmail; }
    public void setVisitorPhone(String visitorPhone) { this.visitorPhone = visitorPhone; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setPaymentMethod(BookingPaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPaymentStatus(BookingPaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setReminderScheduledAt(Instant reminderScheduledAt) { this.reminderScheduledAt = reminderScheduledAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}