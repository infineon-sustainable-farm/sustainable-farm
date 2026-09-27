package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Maps to the "order_items" table.
 * Foreign keys are kept as plain Integer id fields (not @ManyToOne)
 * to keep things simple while learning - same approach as the rest
 * of this backend.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Integer orderItemId;

    @NotNull(message = "orderId is required")
    @Column(name = "order_id")
    private Integer orderId;

    @NotNull(message = "productId is required")
    @Column(name = "product_id")
    private Integer productId;

    @NotNull(message = "quantityKg is required")
    @Positive(message = "quantityKg must be greater than zero")
    @Column(name = "quantity_kg")
    private BigDecimal quantityKg;

    @NotNull(message = "unitPriceEur is required")
    @PositiveOrZero
    @Column(name = "unit_price_eur")
    private BigDecimal unitPriceEur;

    public Integer getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Integer orderItemId) { this.orderItemId = orderItemId; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public BigDecimal getQuantityKg() { return quantityKg; }
    public void setQuantityKg(BigDecimal quantityKg) { this.quantityKg = quantityKg; }

    public BigDecimal getUnitPriceEur() { return unitPriceEur; }
    public void setUnitPriceEur(BigDecimal unitPriceEur) { this.unitPriceEur = unitPriceEur; }
}
