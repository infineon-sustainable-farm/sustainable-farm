package com.sustainablefarm.modules.producttransformation.integration.plants.service;

import com.sustainablefarm.modules.producttransformation.integration.plants.dto.GrowthCalendarResponse;
import com.sustainablefarm.modules.producttransformation.integration.plants.dto.VarietyResponse;
import com.sustainablefarm.modules.producttransformation.integration.plants.impl.PlantsIntegrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Plants Integration Service
 * 
 * Contract: API-001
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PlantsIntegrationServiceTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    private PlantsIntegrationServiceImpl plantsIntegrationService;
    
    @BeforeEach
    void setUp() {
        plantsIntegrationService = new PlantsIntegrationServiceImpl(restTemplate);
        ReflectionTestUtils.setField(plantsIntegrationService, "plantsBaseUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(plantsIntegrationService, "integrationEnabled", true);
    }
    
    @Test
    void getAllVarieties_Success() {
        // Arrange
        VarietyResponse[] mockResponse = {
            createMockVariety("VAR-001", "FARM-001", "A", "Keitt"),
            createMockVariety("VAR-002", "FARM-001", "B", "Kent")
        };
        
        when(restTemplate.getForObject(anyString(), eq(VarietyResponse[].class)))
            .thenReturn(mockResponse);
        
        // Act
        List<VarietyResponse> result = plantsIntegrationService.getAllVarieties();
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Keitt", result.get(0).getNom());
        assertEquals("Kent", result.get(1).getNom());
    }
    
    @Test
    void getAllVarieties_EmptyResponse() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(VarietyResponse[].class)))
            .thenReturn(new VarietyResponse[0]);
        
        // Act
        List<VarietyResponse> result = plantsIntegrationService.getAllVarieties();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void getAllVarieties_NullResponse() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(VarietyResponse[].class)))
            .thenReturn(null);
        
        // Act
        List<VarietyResponse> result = plantsIntegrationService.getAllVarieties();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void getAllVarieties_IntegrationDisabled() {
        // Arrange
        ReflectionTestUtils.setField(plantsIntegrationService, "integrationEnabled", false);
        
        // Act
        List<VarietyResponse> result = plantsIntegrationService.getAllVarieties();
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void getGrowthCalendar_WithFilters() {
        // Arrange
        GrowthCalendarResponse[] mockResponse = {
            createMockGrowthCalendar("GC-001", "FARM-001", "A", "Keitt")
        };
        
        when(restTemplate.getForObject(anyString(), eq(GrowthCalendarResponse[].class)))
            .thenReturn(mockResponse);
        
        // Act
        List<GrowthCalendarResponse> result = plantsIntegrationService.getGrowthCalendar("A", "FARM-001");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Keitt", result.get(0).getVarieties());
    }
    
    @Test
    void getVarietiesByFarm_Success() {
        // Arrange
        VarietyResponse[] mockResponse = {
            createMockVariety("VAR-001", "FARM-001", "A", "Keitt"),
            createMockVariety("VAR-002", "FARM-002", "B", "Kent")
        };
        
        when(restTemplate.getForObject(anyString(), eq(VarietyResponse[].class)))
            .thenReturn(mockResponse);
        
        // Act
        List<VarietyResponse> result = plantsIntegrationService.getVarietiesByFarm("FARM-001");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("FARM-001", result.get(0).getFarmId());
    }
    
    @Test
    void getVarietiesByParcel_Success() {
        // Arrange
        VarietyResponse[] mockResponse = {
            createMockVariety("VAR-001", "FARM-001", "A", "Keitt"),
            createMockVariety("VAR-002", "FARM-001", "B", "Kent")
        };
        
        when(restTemplate.getForObject(anyString(), eq(VarietyResponse[].class)))
            .thenReturn(mockResponse);
        
        // Act
        List<VarietyResponse> result = plantsIntegrationService.getVarietiesByParcel("A");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getParcelId());
    }
    
    @Test
    void getVarietiesByParcel_WithWhitespace() {
        // Arrange
        VarietyResponse[] mockResponse = {
            createMockVariety("VAR-001", "FARM-001", "A", "Keitt")
        };
        
        when(restTemplate.getForObject(anyString(), eq(VarietyResponse[].class)))
            .thenReturn(mockResponse);
        
        // Act
        List<VarietyResponse> result = plantsIntegrationService.getVarietiesByParcel(" A ");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getParcelId());
    }
    
    @Test
    void getGrowthCalendarByFarmAndParcel_Success() {
        // Arrange
        GrowthCalendarResponse[] mockResponse = {
            createMockGrowthCalendar("GC-001", "FARM-001", "A", "Keitt")
        };
        
        when(restTemplate.getForObject(anyString(), eq(GrowthCalendarResponse[].class)))
            .thenReturn(mockResponse);
        
        // Act
        List<GrowthCalendarResponse> result = plantsIntegrationService.getGrowthCalendarByFarmAndParcel("FARM-001", "A");
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("FARM-001", result.get(0).getFarmId());
        assertEquals("A", result.get(0).getParcelId());
    }
    
    @Test
    void getGrowthCalendarByFarmAndParcel_NullParameters() {
        // Act
        List<GrowthCalendarResponse> result = plantsIntegrationService.getGrowthCalendarByFarmAndParcel(null, null);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    // Helper methods
    
    private VarietyResponse createMockVariety(String id, String farmId, String parcel, String name) {
        VarietyResponse variety = new VarietyResponse();
        variety.setId(id);
        variety.setIdFerme(farmId);
        variety.setBlocParcelle(parcel);
        variety.setNom(name);
        variety.setNombreArbres(100);
        variety.setRendementAttenduKg(5000.0);
        variety.setRendementReelKg(4500.0);
        variety.setSource("TEST");
        variety.setDateMaj(LocalDateTime.now());
        return variety;
    }
    
    private GrowthCalendarResponse createMockGrowthCalendar(String id, String farmId, String parcel, String varieties) {
        GrowthCalendarResponse calendar = new GrowthCalendarResponse();
        calendar.setId(id);
        calendar.setIdFerme(farmId);
        calendar.setBlocParcelle(parcel);
        calendar.setVarietes(varieties);
        calendar.setDatePlantation(LocalDateTime.now().minusYears(5));
        calendar.setAgeAnnees(5);
        calendar.setAgeMois(3);
        calendar.setPhaseCroissance("Production");
        calendar.setStadeActuel("Fruiting");
        calendar.setSource("TEST");
        calendar.setDateMaj(LocalDateTime.now());
        return calendar;
    }
}
