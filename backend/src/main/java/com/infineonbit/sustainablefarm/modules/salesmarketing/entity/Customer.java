package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(name = "customers_customer_code_key", columnNames = "customer_code"),
        indexes = {
                @Index(name = "idx_customers_country", columnList = "country"),
                @Index(name = "idx_customers_status", columnList = "status")
        })
@Check(name = "customers_customer_type_check", constraints = "customer_type IN ('distributor', 'retailer', 'direct_export', 'other')")
@Check(name = "customers_status_check", constraints = "status IN ('active', 'inactive', 'prospect')")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_code", length = 20)
    private String customerCode;

    @NotBlank(message = "Company name is required")
    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "customer_type", nullable = false, length = 30)
    private String customerType;

    @Column(length = 80)
    private String country;
    @Column(length = 80)
    private String region;
    @Column(length = 80)
    private String city;

    @ColumnDefault("'prospect'")
    @Column(nullable = false, length = 20)
    private String status = "prospect";

    @Column(name = "payment_terms", length = 50)
    private String paymentTerms;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
