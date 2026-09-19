package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A mango variety planted on a given block of the farm.
 *
 * <p>Column names are the domain names used by the Zalka 2025 study and are
 * mapped explicitly, so the snake_case schema stays stable no matter what the
 * Java field naming strategy does.
 *
 * <p>Nullable on purpose: {@code treeDensityPerHa}, {@code actualYieldKg},
 * {@code vigor} and {@code plantOrigin} have no source data yet. They stay
 * NULL rather than being filled with a default or a derived value.
 */
@Entity
@Table(name = "varietes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Variety {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_ferme")
    private Integer farmId;

    @Column(name = "nom", nullable = false)
    private String name;

    @Column(name = "nombre_arbres")
    private Integer treeCount;

    @Column(name = "espacement_inter_rang_m")
    private Double rowSpacingM;

    @Column(name = "espacement_intra_rang_m")
    private Double treeSpacingM;

    /** Not available in the source study. Stays NULL until measured. */
    @Column(name = "densite_arbres_ha")
    private Double treeDensityPerHa;

    @Column(name = "rendement_attendu_kg")
    private Double expectedYieldKg;

    /** Not harvested yet. Stays NULL until the harvest is recorded. */
    @Column(name = "rendement_reel_kg")
    private Double actualYieldKg;

    /** Not assessed yet. Stays NULL until an agronomist rates it. */
    @Column(name = "vigueur")
    private String vigor;

    @Column(name = "bloc_parcelle")
    private String blockCode;

    /** Not documented in the source study. Stays NULL. */
    @Column(name = "origine_plant")
    private String plantOrigin;

    @Column(name = "source")
    private String source;

    @Column(name = "date_maj")
    private Instant lastUpdated;
}
