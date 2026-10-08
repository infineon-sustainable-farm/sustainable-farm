package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

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
 *
 * <p>One row per (farm, block, name): {@code varietyKey} holds that triple and
 * carries a unique constraint, so two plantings of a new triple sent at the
 * same time cannot both create a row.
 */
@Entity
@Table(name = "varietes",
        uniqueConstraints = @UniqueConstraint(name = Variety.KEY_CONSTRAINT, columnNames = "variety_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Variety {

    /** Name of the unique constraint on {@code variety_key}. */
    public static final String KEY_CONSTRAINT = "uk_varietes_variety_key";

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

    /**
     * Natural key of the row, written on every insert and update and never
     * exposed by the API. See {@link #keyOf}. NULL on the rows written before
     * it existed, until they are updated or, in the dev profile, the startup
     * filling reaches them.
     *
     * <p>523 characters hold the longest farm (11), block (255) and name (255)
     * with both separators, so no row is ever cut.
     */
    @Column(name = "variety_key", length = 523)
    private String varietyKey;

    /**
     * Natural key of a variety row: the farm, 0 for a row without a farm (farm
     * identifiers start at 1), the block and the name in lower case, joined by
     * {@code "|"}. For example {@code "0|C|kent"}.
     *
     * <p>It follows the rule of the planting lookup
     * ({@code VarietyRepository.findByFarmBlockAndName}): a NULL farm is a
     * value of its own, the name ignores case, and the stored name is taken as
     * it is, accents and spaces included, without trimming.
     *
     * @param farmId    farm identifier, or {@code null} for no farm
     * @param blockCode block code as stored
     * @param name      variety name as stored
     * @return the key, or {@code null} when the block or the name is missing,
     *         since no planting can match such a row
     */
    public static String keyOf(Integer farmId, String blockCode, String name) {
        if (blockCode == null || name == null) {
            return null;
        }
        return (farmId == null ? 0 : farmId) + "|" + blockCode + "|" + name.toLowerCase(Locale.ROOT);
    }

    @PrePersist
    @PreUpdate
    void refreshVarietyKey() {
        varietyKey = keyOf(farmId, blockCode, name);
    }
}
