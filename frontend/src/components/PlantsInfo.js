import React, { useState, useEffect } from 'react';
import plantsServiceInstance from '../services/plantsService';
import Card from './common/Card';
import Loading from './common/Loading';
import ErrorMessage from './common/ErrorMessage';
import Badge from './common/Badge';

/**
 * Plants Information Component
 * 
 * Displays variety and growth calendar data from Plants module integration
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
const PlantsInfo = ({ farmId, parcelId, showGrowthCalendar = false }) => {
  const [varieties, setVarieties] = useState([]);
  const [growthCalendar, setGrowthCalendar] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchPlantsData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [farmId, parcelId]);

  const fetchPlantsData = async () => {
    setLoading(true);
    setError(null);

    try {
      // Fetch varieties based on filters
      let varietiesData;
      if (farmId && parcelId) {
        // If both farm and parcel provided, fetch by farm (varieties don't have parcel filter in API)
        varietiesData = await plantsServiceInstance.getVarietiesByFarm(farmId);
        // Filter by parcel on client side
        varietiesData = varietiesData.filter(v => v.bloc_parcelle === parcelId);
      } else if (farmId) {
        varietiesData = await plantsServiceInstance.getVarietiesByFarm(farmId);
      } else if (parcelId) {
        varietiesData = await plantsServiceInstance.getVarietiesByParcel(parcelId);
      } else {
        varietiesData = await plantsServiceInstance.getAllVarieties();
      }

      setVarieties(varietiesData.map(plantsServiceInstance.formatVarietyForDisplay));

      // Fetch growth calendar if requested
      if (showGrowthCalendar) {
        let calendarData;
        if (farmId && parcelId) {
          calendarData = await plantsServiceInstance.getGrowthCalendarByFarmAndParcel(farmId, parcelId);
        } else {
          calendarData = await plantsServiceInstance.getGrowthCalendar({ 
            blocParcelle: parcelId, 
            idFerme: farmId 
          });
        }
        setGrowthCalendar(calendarData.map(plantsServiceInstance.formatGrowthCalendarForDisplay));
      }
    } catch (err) {
      setError(err.message || 'Failed to fetch Plants data');
      console.error('Error fetching Plants data:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <Loading message="Loading Plants data..." />;
  }

  if (error) {
    return (
      <ErrorMessage 
        message={error}
        onRetry={fetchPlantsData}
      />
    );
  }

  return (
    <div className="plants-info">
      {varieties.length > 0 && (
        <Card title="Varieties from Plants Module">
          <div className="varieties-list">
            {varieties.map((variety) => (
              <div key={variety.id} className="variety-item">
                <div className="variety-header">
                  <h4>{variety.name}</h4>
                  <Badge variant="info">{variety.parcelId}</Badge>
                </div>
                <div className="variety-details">
                  <div className="detail-row">
                    <span className="label">Farm:</span>
                    <span className="value">{variety.farmId}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Trees:</span>
                    <span className="value">{variety.treeCount}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Density:</span>
                    <span className="value">{variety.densityPerHectare} trees/ha</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Expected Yield:</span>
                    <span className="value">{variety.expectedYield} kg</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Actual Yield:</span>
                    <span className="value">{variety.actualYield} kg</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Vigor:</span>
                    <span className="value">{variety.vigor}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {showGrowthCalendar && growthCalendar.length > 0 && (
        <Card title="Growth Calendar">
          <div className="growth-calendar-list">
            {growthCalendar.map((calendar) => (
              <div key={calendar.id} className="calendar-item">
                <div className="calendar-header">
                  <h4>{calendar.varieties}</h4>
                  <Badge variant="success">{calendar.currentStage}</Badge>
                </div>
                <div className="calendar-details">
                  <div className="detail-row">
                    <span className="label">Farm:</span>
                    <span className="value">{calendar.farmId}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Parcel:</span>
                    <span className="value">{calendar.parcelId}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Planting Date:</span>
                    <span className="value">
                      {calendar.plantingDate ? new Date(calendar.plantingDate).toLocaleDateString() : 'N/A'}
                    </span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Age:</span>
                    <span className="value">{calendar.ageYears} years, {calendar.ageMonths} months</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Growth Phase:</span>
                    <span className="value">{calendar.growthPhase}</span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Local Rainfall:</span>
                    <span className="value">{calendar.localRainfall} mm</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {varieties.length === 0 && !loading && !error && (
        <Card title="Plants Information">
          <p className="no-data">No Plants data available for the selected filters.</p>
        </Card>
      )}
    </div>
  );
};

export default PlantsInfo;
