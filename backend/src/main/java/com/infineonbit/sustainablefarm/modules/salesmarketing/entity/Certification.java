package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Maps to the "certifications" table.
 * Foreign keys are kept as plain Integer id fields (not @ManyToOne)
 * to keep things simple while learning - same approach as the rest
 * of this backend.
 */
@Entity
@Table(name = "certifications")
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certification_id")
    private Integer certificationId;

    @NotBlank(message = "Certification name is required")
    @Column(name = "name")
    private String name;

    public Integer getCertificationId() { return certificationId; }
    public void setCertificationId(Integer certificationId) { this.certificationId = certificationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
