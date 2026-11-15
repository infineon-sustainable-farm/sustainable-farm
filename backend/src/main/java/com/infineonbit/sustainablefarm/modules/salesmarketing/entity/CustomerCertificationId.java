package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import java.io.Serializable;
import java.util.Objects;

public class CustomerCertificationId implements Serializable {

    private Integer customerId;
    private Integer certificationId;

    public CustomerCertificationId() {}

    public CustomerCertificationId(Integer customerId, Integer certificationId) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerCertificationId that)) return false;
        return Objects.equals(customerId, that.customerId)
                && Objects.equals(certificationId, that.certificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, certificationId);
    }
}