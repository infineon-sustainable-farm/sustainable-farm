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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** A zone of a block, the unit that is irrigated and measured. */
@Entity
@Table(name = "zone", indexes = @Index(name = "idx_zone_block_id", columnList = "block_id"))
@Getter
@Setter
public class Zone extends BaseEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false, foreignKey = @ForeignKey(name = "fk_zone_block"))
    private Block block;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(name = "area_hectares", nullable = false)
    private Double areaHectares;

    @Column(name = "irrigation_method")
    private String irrigationMethod;

    /**
     * Crop coefficient (Kc) used to estimate the zone's theoretical water need
     * (need = area x ET0 x Kc / system efficiency). Null = default value 1.0.
     */
    @Column(name = "crop_coefficient")
    private Double cropCoefficient;

    /**
     * Number of emitters of the zone and nominal flow of one emitter (L/h): they give the
     * network's theoretical flow, the reference for clogging (measured flow below 90 %)
     * or leak detection (measured flow above 110 %).
     */
    @Column(name = "emitter_count")
    private Integer emitterCount;

    @Column(name = "emitter_nominal_flow_lh")
    private Double emitterNominalFlowLh;
}
