package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;

/**
 * API representation of a variety.
 *
 * <p>The component names deliberately keep the snake_case domain names of the
 * Zalka 2025 study ({@code nombre_arbres}, {@code rendement_attendu_kg},
 * {@code bloc_parcelle}, ...). Other modules will consume this contract, so the
 * names are part of it and must not be renamed to camelCase.
 *
 * <p>A component is {@code null} whenever the underlying data does not exist.
 * Consumers render that as an explicit "no value", never as zero.
 */
public record VarietyResponse(
        Long id,
        Integer id_ferme,
        String nom,
        Integer nombre_arbres,
        Double espacement_inter_rang_m,
        Double espacement_intra_rang_m,
        Double densite_arbres_ha,
        Double rendement_attendu_kg,
        Double rendement_reel_kg,
        String vigueur,
        String bloc_parcelle,
        String origine_plant,
        String source,
        Instant date_maj) {
}
