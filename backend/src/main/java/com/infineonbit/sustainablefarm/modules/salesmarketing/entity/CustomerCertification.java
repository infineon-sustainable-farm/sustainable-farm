package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Maps to the "customer_certifications" junction table.
 * The primary key is the combination of customer_id and certification_id.
 */
@Entity
@Table(name = "customer_certifications")
@IdClass(CustomerCertificationId.class)
public class CustomerCertification {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @Id
    @Column(name = "certification_id")
    private Integer certificationId;

    public CustomerCertification() {}

    public CustomerCertification(Integer customerId, Integer certificationId) {
        this.customerId = customerId;
        this.certificationId = certificationId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getCertificationId() {
        return certificationId;
    }

    public void setCertificationId(Integer certificationId) {
        this.certificationId = certificationId;
    }
}