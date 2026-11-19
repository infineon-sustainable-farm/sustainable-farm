package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import com.infineonbit.sustainablefarm.core.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An order of a customer, through a sales channel. A customer or a channel that has orders
 * cannot be deleted. The total value is in euros; the status is the free label the API keeps.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_channel", columnList = "sales_channel_id"),
        @Index(name = "idx_orders_customer", columnList = "customer_id")
})
@Getter
@Setter
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "orders_customer_id_fkey"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_channel_id", foreignKey = @ForeignKey(name = "orders_sales_channel_id_fkey"))
    private SalesChannel salesChannel;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "total_volume_kg", precision = 12, scale = 2)
    private BigDecimal totalVolumeKg;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_value_amount", precision = 14, scale = 2))
    @AttributeOverride(name = "currency", column = @Column(name = "total_value_currency", length = 3))
    private Money totalValue;

    @ColumnDefault("'pending'")
    @Column(name = "status", length = 30)
    private String status;
}
