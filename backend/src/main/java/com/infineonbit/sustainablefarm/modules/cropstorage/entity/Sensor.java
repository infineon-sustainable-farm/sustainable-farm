package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

import com.infineonbit.sustainablefarm.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** A temperature or humidity sensor installed in a storage zone. */
@Entity
@Table(name = "sensor")
@Getter
@Setter
public class Sensor extends BaseEntity {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String sensorCode;

    private String sensorType;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_zone_id")
    private StorageZone storageZone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperationalStatus status;

    private LocalDate installationDate;
}
