package com.infineonbit.sustainablefarm.modules.salesmarketing.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** A sales channel (export, retail...). */
@Entity
@Table(name = "sales_channels", uniqueConstraints = @UniqueConstraint(name = "sales_channels_name_key", columnNames = "name"))
@Getter
@Setter
public class SalesChannel extends BaseEntity {

    @Column(name = "name", nullable = false, length = 60)
    private String name;
}
