import React from 'react';
import { equipmentApi } from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import './EquipmentPage.css';

const EquipmentPage = () => {
  const [equipment, setEquipment] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  React.useEffect(() => {
    fetchEquipment();
  }, []);

  const fetchEquipment = async () => {
    try {
      setLoading(true);
      const response = await equipmentApi.getAll();
      setEquipment(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching equipment:', err);
      const status = err.response?.status;
      if (status === 500) {
        setError('Equipment data temporarily unavailable (backend service error). Please try again later.');
      } else {
        setError('Failed to load equipment.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchEquipment} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Equipment</h1>
      </div>

      <Card>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Equipment ID</th>
                <th>Name</th>
                <th>Type</th>
                <th>Capacity (kg/h)</th>
                <th>Energy (kWh/kg)</th>
                <th>Location</th>
                <th>Status</th>
                <th>Last Maintenance</th>
              </tr>
            </thead>
            <tbody>
              {equipment.length === 0 ? (
                <tr><td colSpan="8" className="empty-row">No equipment found</td></tr>
              ) : (
                equipment.map((item, index) => (
                  <tr key={index}>
                    <td>{item.equipmentId}</td>
                    <td>{item.equipmentName}</td>
                    <td>{item.equipmentType}</td>
                    <td>{item.capacityKgPerHour}</td>
                    <td>{item.energyConsumptionKwhPerKg}</td>
                    <td>{item.location}</td>
                    <td><Badge variant={getStatusVariant(item.maintenanceStatus)}>{item.maintenanceStatus}</Badge></td>
                    <td>{item.lastMaintenanceDate}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};

function getStatusVariant(status) {
  if (status === 'ACTIVE') return 'success';
  if (status === 'MAINTENANCE') return 'warning';
  if (status === 'INACTIVE') return 'error';
  return 'neutral';
}

export default EquipmentPage;