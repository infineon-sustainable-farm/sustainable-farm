package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A zone of the store (warehouse room, cold room), with its capacity and the
 * climate range its sensors are checked against. Not a farm zone: farm zones
 * are irrigation areas on a block.
 */
@Entity
@Table(name = "storage_zone")
@Getter
@Setter
public class StorageZone extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String zoneCode;

    private String name;

    private String description;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal capacityKg;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperatureMinCelsius;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperatureMaxCelsius;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(precision = 5, scale = 2)
    private BigDecimal humidityMinPercent;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(precision = 5, scale = 2)
    private BigDecimal humidityMaxPercent;

    private Boolean coolingAvailable;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperationalStatus status;
}
