package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Who delivered a batch to the store when it does not come from the farm itself. */
@Entity
@Table(name = "supplier")
@Getter
@Setter
public class Supplier extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String supplierCode;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String region;

    private String contactPhone;
}
