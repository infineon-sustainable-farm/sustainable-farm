package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
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
 * <p>Nullable on purpose: {@code treeDensityPerHa}, {@code vigor} and
 * {@code plantOrigin} have no source data yet. They stay NULL rather than
 * being filled with a default or a derived value.
 *
 * <p>No yield is stored on the row. The expected and the actual yield of a
 * variety are computed on every read, from its plantings, the agronomic
 * reference and its harvests (see {@code YieldComparisonService}).
 *
 * <p>One row per (block, name): {@code varietyKey} holds that pair and
 * carries a unique constraint, so two plantings of a new pair sent at the
 * same time cannot both create a row.
 */
@Entity
@EntityListeners(BlockLabelListener.class)
@Table(name = "varietes",
        uniqueConstraints = @UniqueConstraint(name = Variety.KEY_CONSTRAINT, columnNames = "variety_key"),
        indexes = @Index(name = "idx_varietes_block_id", columnList = "block_id"))
@Getter
@Setter
@NoArgsConstructor
public class Variety implements BlockRow {

    /** Name of the unique constraint on {@code variety_key}. */
    public static final String KEY_CONSTRAINT = "uk_varietes_variety_key";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    /** Not assessed yet. Stays NULL until an agronomist rates it. */
    @Column(name = "vigueur")
    private String vigor;

    /** Block of the variety row, a {@code farm.block} row (foreign key {@code fk_varietes_block}). */
    @Column(name = "block_id")
    private Long blockId;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final BlockLink block = new BlockLink();

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
     * <p>300 characters hold the longest block id (19) and name (255) with
     * the separator, so no row is ever cut.
     */
    @Column(name = "variety_key", length = 300)
    private String varietyKey;

    /**
     * Natural key of a variety row: the block id and the name in lower case,
     * joined by {@code "|"}. For example {@code "3|kent"}.
     *
     * <p>It follows the rule of the planting lookup
     * ({@code VarietyRepository.findByBlockAndName}): the name ignores case,
     * and the stored name is taken as it is, accents and spaces included,
     * without trimming. The block gives the farm, so the farm is not part of
     * the key.
     *
     * @param blockId id of the block
     * @param name    variety name as stored
     * @return the key, or {@code null} when the block or the name is missing,
     *         since no planting can match such a row
     */
    public static String keyOf(Long blockId, String name) {
        if (blockId == null || name == null) {
            return null;
        }
        return blockId + "|" + name.toLowerCase(Locale.ROOT);
    }

    @PrePersist
    @PreUpdate
    void refreshVarietyKey() {
        block.requireResolved("variety row");
        varietyKey = keyOf(blockId, name);
    }

    /** Farm of the block, read from the farm referential; {@code null} without a block. */
    public Long getFarmId() {
        return block.label(blockId).farmId();
    }

    /** Sets the farm by hand, on a row built in memory; a saved row takes it from its block. */
    public void setFarmId(Long farmId) {
        block.setFarmId(farmId);
    }

    /** Code of the block, for example {@code "A"}, read from the farm referential. */
    public String getBlockCode() {
        return block.label(blockId).code();
    }

    /** Sets the block code by hand, on a row built in memory; a saved row takes it from its block. */
    public void setBlockCode(String blockCode) {
        block.setCode(blockCode);
    }

    /** Points the row to a block of the farm referential, with the label plants shows for it. */
    public void assignBlock(Long blockId, BlockLabel label) {
        this.blockId = blockId;
        block.set(blockId, label);
    }

    @Override
    public void useBlockLabels(BlockLabelSource source) {
        block.useSource(source);
    }
}
