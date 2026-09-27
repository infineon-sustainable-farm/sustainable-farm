package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maps to the "orders" table.
 * Foreign keys are kept as plain Integer id fields.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @NotNull(message = "customerId is required")
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "sales_channel_id")
    private Integer salesChannelId;

    @NotNull(message = "orderDate is required")
    @Column(name = "order_date")
    private LocalDate orderDate;

    @PositiveOrZero(message = "totalVolumeKg cannot be negative")
    @Column(name = "total_volume_kg")
    private BigDecimal totalVolumeKg;

    @PositiveOrZero(message = "totalValueEur cannot be negative")
    @Column(name = "total_value_eur")
    private BigDecimal totalValueEur;

    @Column(name = "status")
    private String status;

    // ---- Getters and setters ----

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public Integer getSalesChannelId() { return salesChannelId; }
    public void setSalesChannelId(Integer salesChannelId) { this.salesChannelId = salesChannelId; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public BigDecimal getTotalVolumeKg() { return totalVolumeKg; }
    public void setTotalVolumeKg(BigDecimal totalVolumeKg) { this.totalVolumeKg = totalVolumeKg; }

    public BigDecimal getTotalValueEur() { return totalValueEur; }
    public void setTotalValueEur(BigDecimal totalValueEur) { this.totalValueEur = totalValueEur; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
