package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * A certification held by a customer: one row per customer and certification, deleted with
 * either of them.
 */
@Entity
@Table(name = "customer_certifications", uniqueConstraints = @UniqueConstraint(
        name = "customer_certifications_customer_id_certification_id_key",
        columnNames = {"customer_id", "certification_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CustomerCertification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "customer_certifications_customer_id_fkey"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certification_id", nullable = false,
            foreignKey = @ForeignKey(name = "customer_certifications_certification_id_fkey"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Certification certification;

    public CustomerCertification(Customer customer, Certification certification) {
        this.customer = customer;
        this.certification = certification;
    }
}
