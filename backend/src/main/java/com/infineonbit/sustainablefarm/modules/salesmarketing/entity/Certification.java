package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** A certification a customer can hold (organic, fair trade...). */
@Entity
@Table(name = "certifications", uniqueConstraints = @UniqueConstraint(name = "certifications_name_key", columnNames = "name"))
@Getter
@Setter
public class Certification extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
