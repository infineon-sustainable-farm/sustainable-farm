package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/** A customer of the farm, with its business code. */
@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(name = "customers_customer_code_key", columnNames = "customer_code"),
        indexes = {
                @Index(name = "idx_customers_country", columnList = "country"),
                @Index(name = "idx_customers_status", columnList = "status")
        })
@Getter
@Setter
public class Customer extends BaseEntity {

    @Column(name = "customer_code", length = 20)
    private String customerCode;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Convert(converter = CustomerType.Converter.class)
    @Column(name = "customer_type", nullable = false, length = 30)
    private CustomerType customerType;

    @Column(length = 80)
    private String country;

    @Column(length = 80)
    private String region;

    @Column(length = 80)
    private String city;

    @ColumnDefault("'prospect'")
    @Convert(converter = CustomerStatus.Converter.class)
    @Column(nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.PROSPECT;

    @Column(name = "payment_terms", length = 50)
    private String paymentTerms;
}
