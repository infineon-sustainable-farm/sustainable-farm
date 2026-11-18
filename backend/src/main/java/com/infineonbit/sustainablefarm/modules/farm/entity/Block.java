package com.infineonbit.sustainablefarm.modules.farm.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * A block of a farm: the plot that Water Supply calls a field and Plants designates by its code.
 *
 * <p>The code ("A", "B2") is the readable business key shared with Plants, unique across the
 * farms and stored in upper case. It stays empty until the block is linked to a Plants block.</p>
 */
@Entity
@Table(name = "block",
        uniqueConstraints = @UniqueConstraint(name = "uk_block_code", columnNames = "code"),
        indexes = @Index(name = "idx_block_farm_id", columnList = "farm_id"))
@Getter
@Setter
public class Block extends BaseEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false, foreignKey = @ForeignKey(name = "fk_block_farm"))
    private Farm farm;

    @Column(length = 50)
    private String code;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(name = "area_hectares", nullable = false)
    private Double areaHectares;

    @Column(name = "crop_type")
    private String cropType;

    @Column(name = "soil_type")
    private String soilType;

    @Column(columnDefinition = "text")
    private String coordinates;
}
