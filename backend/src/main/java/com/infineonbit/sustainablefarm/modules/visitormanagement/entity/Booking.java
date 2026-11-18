package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.CurrencyCode;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * An agritourism booking linked to an activity and a tour time slot.
 *
 * <p>Business rules: a booking can never exceed the activity capacity (nor the
 * slot capacity); confirmations require payment. The visitor is a {@link Visitor} row,
 * not a copy of its name, email and phone.</p>
 */
@Entity
@Table(name = "booking", indexes = @Index(name = "idx_booking_visitor", columnList = "visitor_id"))
public class Booking extends BaseEntity {

    @NotNull(message = "activity is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agri_activity_id", nullable = false)
    private AgriActivity activity;

    @NotNull(message = "timeSlot is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    /** The visitor who booked: found by email (or by name without email) or created on booking. */
    @NotNull(message = "visitor is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @NotNull(message = "peopleCount is required")
    @Min(value = 1, message = "peopleCount must be at least 1")
    @Column(name = "people_count", nullable = false)
    private Integer peopleCount;

    /** Price of the activity times the number of people, in XOF (FCFA). */
    @NotNull
    @Embedded
    @AttributeOverride(name = "amount",
            column = @Column(name = "total_amount", nullable = false, precision = 14, scale = 2))
    @AttributeOverride(name = "currency",
            column = @Column(name = "total_currency", nullable = false, length = 3))
    private Money total = Money.zero(CurrencyCode.XOF);

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

    @Column(name = "confirmation_sent_at")
    private Instant confirmationSentAt;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

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

    public Visitor getVisitor() {
        return visitor;
    }

    public void setVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    /** Full name of the linked visitor, as the booking JSON shows it. */
    public String getVisitorFullName() {
        return visitor == null ? null : visitor.getFullName();
    }

    /** Email of the linked visitor, where confirmations and reminders go. */
    public String getVisitorEmail() {
        return visitor == null ? null : visitor.getEmail();
    }

    /** Phone of the linked visitor. */
    public String getVisitorPhone() {
        return visitor == null ? null : visitor.getPhone();
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public Money getTotal() {
        return total;
    }

    public void setTotal(Money total) {
        this.total = total;
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

    public Instant getConfirmationSentAt() {
        return confirmationSentAt;
    }

    public void setConfirmationSentAt(Instant confirmationSentAt) {
        this.confirmationSentAt = confirmationSentAt;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(Instant reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }
}