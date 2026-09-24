package com.sustainablefarm.modules.producttransformation.integration.plants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Plants API Growth Calendar response
 * 
 * Contract: API-001
 * Endpoint: GET /api/plants/growth-calendar
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthCalendarResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("id_ferme")
    private String idFerme;
    
    @JsonProperty("bloc_parcelle")
    private String blocParcelle;
    
    @JsonProperty("varietes")
    private String varietes;
    
    @JsonProperty("date_plantation")
    private LocalDateTime datePlantation;
    
    @JsonProperty("precision_date")
    private String precisionDate;
    
    @JsonProperty("age_annees")
    private Integer ageAnnees;
    
    @JsonProperty("age_mois")
    private Integer ageMois;
    
    @JsonProperty("phase_croissance")
    private String phaseCroissance;
    
    @JsonProperty("phase_tranche_annees")
    private String phaseTrancheAnnees;
    
    @JsonProperty("stade_actuel")
    private String stadeActuel;
    
    @JsonProperty("phase_annees")
    private String phaseAnnees;
    
    @JsonProperty("pluviometrie_locale_mm")
    private Double pluviometrieLocaleMm;
    
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
     */
    public String getParcelId() {
        if (blocParcelle == null) {
            return null;
        }
        return blocParcelle.trim();
    }
    
    /**
     * Returns the varieties planted
     */
    public String getVarieties() {
        return varietes;
    }
}
