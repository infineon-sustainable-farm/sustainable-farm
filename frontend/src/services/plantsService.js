import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
const PLANTS_INTEGRATION_PATH = '/api/integration/plants';

/**
 * Service for Plants module integration
 * 
 * Consumes Plants API data through Product Transformation backend
 * Contract: API-001
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
class PlantsService {
  /**
   * Retrieves all mango varieties from Plants module
   * 
   * @returns {Promise<Array>} List of variety responses
   */
  async getAllVarieties() {
    try {
      const response = await axios.get(`${API_BASE_URL}${PLANTS_INTEGRATION_PATH}/varieties`);
      return response.data;
    } catch (error) {
      console.error('Error fetching varieties from Plants API:', error);
      throw error;
    }
  }

  /**
   * Retrieves varieties filtered by farm
   * 
   * @param {string} farmId - Farm identifier
   * @returns {Promise<Array>} List of varieties for the specified farm
   */
  async getVarietiesByFarm(farmId) {
    try {
      const response = await axios.get(`${API_BASE_URL}${PLANTS_INTEGRATION_PATH}/varieties/farm/${farmId}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching varieties for farm ${farmId}:`, error);
      throw error;
    }
  }

  /**
   * Retrieves varieties filtered by parcel
   * 
   * @param {string} parcelId - Parcel identifier (single character: A, B, C, ...)
   * @returns {Promise<Array>} List of varieties for the specified parcel
   */
  async getVarietiesByParcel(parcelId) {
    try {
      const response = await axios.get(`${API_BASE_URL}${PLANTS_INTEGRATION_PATH}/varieties/parcel/${parcelId}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching varieties for parcel ${parcelId}:`, error);
      throw error;
    }
  }

  /**
   * Retrieves growth calendar data from Plants module
   * 
   * @param {Object} filters - Optional filters
   * @param {string} filters.blocParcelle - Filter by parcel/block identifier
   * @param {string} filters.idFerme - Filter by farm identifier
   * @returns {Promise<Array>} List of growth calendar responses
   */
  async getGrowthCalendar(filters = {}) {
    try {
      const params = new URLSearchParams();
      if (filters.blocParcelle) params.append('blocParcelle', filters.blocParcelle);
      if (filters.idFerme) params.append('idFerme', filters.idFerme);

      const url = `${API_BASE_URL}${PLANTS_INTEGRATION_PATH}/growth-calendar${params.toString() ? '?' + params.toString() : ''}`;
      const response = await axios.get(url);
      return response.data;
    } catch (error) {
      console.error('Error fetching growth calendar from Plants API:', error);
      throw error;
    }
  }

  /**
   * Retrieves growth calendar for a specific farm and parcel
   * 
   * @param {string} farmId - Farm identifier
   * @param {string} parcelId - Parcel identifier (single character: A, B, C, ...)
   * @returns {Promise<Array>} Growth calendar data for the specified farm and parcel
   */
  async getGrowthCalendarByFarmAndParcel(farmId, parcelId) {
    try {
      const response = await axios.get(
        `${API_BASE_URL}${PLANTS_INTEGRATION_PATH}/growth-calendar/farm/${farmId}/parcel/${parcelId}`
      );
      return response.data;
    } catch (error) {
      console.error(`Error fetching growth calendar for farm ${farmId} and parcel ${parcelId}:`, error);
      throw error;
    }
  }

  /**
   * Formats variety data for display
   * 
   * @param {Object} variety - Raw variety data from API
   * @returns {Object} Formatted variety data
   */
  formatVarietyForDisplay(variety) {
    return {
      id: variety.id,
      farmId: variety.id_ferme,
      parcelId: variety.bloc_parcelle,
      name: variety.nom,
      treeCount: variety.nombre_arbres,
      interRowSpacing: variety.espacement_inter_rang_m,
      intraRowSpacing: variety.espacement_intra_rang_m,
      densityPerHectare: variety.densite_arbres_ha,
      expectedYield: variety.rendement_attendu_kg,
      actualYield: variety.rendement_reel_kg,
      vigor: variety.vigueur,
      plantOrigin: variety.origine_plant,
      source: variety.source,
      lastUpdated: variety.date_maj
    };
  }

  /**
   * Formats growth calendar data for display
   * 
   * @param {Object} calendar - Raw growth calendar data from API
   * @returns {Object} Formatted growth calendar data
   */
  formatGrowthCalendarForDisplay(calendar) {
    return {
      id: calendar.id,
      farmId: calendar.id_ferme,
      parcelId: calendar.bloc_parcelle,
      varieties: calendar.varietes,
      plantingDate: calendar.date_plantation,
      datePrecision: calendar.precision_date,
      ageYears: calendar.age_annees,
      ageMonths: calendar.age_mois,
      growthPhase: calendar.phase_croissance,
      phaseTimeRange: calendar.phase_tranche_annees,
      currentStage: calendar.stade_actuel,
      phaseYears: calendar.phase_annees,
      localRainfall: calendar.pluviometrie_locale_mm,
      source: calendar.source,
      lastUpdated: calendar.date_maj
    };
  }
}

const plantsServiceInstance = new PlantsService();
export default plantsServiceInstance;
