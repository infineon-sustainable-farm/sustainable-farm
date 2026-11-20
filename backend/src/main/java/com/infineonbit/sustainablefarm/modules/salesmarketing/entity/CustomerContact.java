package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** A person to contact at a customer, deleted with the customer. */
@Entity
@Table(name = "customer_contacts", indexes = @Index(name = "idx_customer_contacts_cust", columnList = "customer_id"))
@Getter
@Setter
public class CustomerContact extends BaseEntity {

    /** The customer; the API still accepts a contact without one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "customer_contacts_customer_id_fkey"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Customer customer;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "role", length = 80)
    private String role;

    /** {@code false} when none is sent. */
    @ColumnDefault("false")
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;
}
