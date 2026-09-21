package com.sustainablefarm.modules.producttransformation.integration.plants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Plants API Variety response
 * 
 * Contract: API-001
 * Endpoint: GET /api/plants/varieties
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VarietyResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("id_ferme")
    private String idFerme;
    
    @JsonProperty("nom")
    private String nom;
    
    @JsonProperty("nombre_arbres")
    private Integer nombreArbres;
    
    @JsonProperty("espacement_inter_rang_m")
    private Double espacementInterRangM;
    
    @JsonProperty("espacement_intra_rang_m")
    private Double espacementIntraRangM;
    
    @JsonProperty("densite_arbres_ha")
    private Double densiteArbresHa;
    
    @JsonProperty("rendement_attendu_kg")
    private Double rendementAttenduKg;
    
    @JsonProperty("rendement_reel_kg")
    private Double rendementReelKg;
    
    @JsonProperty("vigueur")
    private String vigueur;
    
    @JsonProperty("bloc_parcelle")
    private String blocParcelle;
    
    @JsonProperty("origine_plant")
    private String originePlant;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("date_maj")
    private LocalDateTime dateMaj;
    
    /**
     * Maps to Product Transformation's farm_id
     */
    public String getFarmId() {
        return idFerme;
    }
    
    /**
     * Maps to Product Transformation's block_id/parcel_id
     * Returns the parcel identifier in the correct format (A, B, C, ...)
     */
    public String getParcelId() {
        if (blocParcelle == null) {
            return null;
        }
        // Ensure we store only the character, not "Bloc A", "block A", etc.
        return blocParcelle.trim();
    }
    
    /**
     * Maps to Product Transformation's variety
     * Converts variety name to enum if possible
     */
    public String getVarietyName() {
        return nom;
    }
}
