package com.infineonbit.sustainablefarm.modules.farm.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** A farm, the root of the site referential: it is divided into blocks, themselves divided into zones. */
@Entity
@Table(name = "farm")
@Getter
@Setter
public class Farm extends BaseEntity {
    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private String address;
    private Double latitude;
    private Double longitude;

    @Column(name = "area_hectares")
    private Double areaHectares;
}
