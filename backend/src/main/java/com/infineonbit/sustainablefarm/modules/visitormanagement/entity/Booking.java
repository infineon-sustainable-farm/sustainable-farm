package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * An agritourism booking linked to an activity and a tour time slot.
 *
 * <p>Business rules: a booking can never exceed the activity capacity (nor the
 * slot capacity); confirmations require payment.</p>
 */
@Entity
@Table(name = "booking")
public class Booking extends BaseEntity {

    @NotNull(message = "activity is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agri_activity_id", nullable = false)
    private AgriActivity activity;

    @NotNull(message = "timeSlot is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @NotBlank(message = "visitorFullName is required")
    @Size(max = 150, message = "visitorFullName must be at most 150 characters")
    @Column(name = "visitor_full_name", nullable = false, length = 150)
    private String visitorFullName;

    @NotBlank(message = "visitorEmail is required")
    @Email(message = "visitorEmail must be a valid email")
    @Size(max = 255, message = "visitorEmail must be at most 255 characters")
    @Column(name = "visitor_email", nullable = false, length = 255)
    private String visitorEmail;

    @Size(max = 30, message = "visitorPhone must be at most 30 characters")
    @Column(name = "visitor_phone", length = 30)
    private String visitorPhone;

    @NotNull(message = "peopleCount is required")
    @Min(value = 1, message = "peopleCount must be at least 1")
    @Column(name = "people_count", nullable = false)
    private Integer peopleCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private BookingPaymentMethod paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private BookingPaymentStatus paymentStatus = BookingPaymentStatus.UNPAID;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "reminder_scheduled_at")
    private Instant reminderScheduledAt;

    public AgriActivity getActivity() {
        return activity;
    }

    public void setActivity(AgriActivity activity) {
        this.activity = activity;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
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

    public BookingPaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(BookingPaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Instant getReminderScheduledAt() {
        return reminderScheduledAt;
    }

    public void setReminderScheduledAt(Instant reminderScheduledAt) {
        this.reminderScheduledAt = reminderScheduledAt;
    }
}