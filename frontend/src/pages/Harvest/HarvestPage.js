import React from 'react';
import { harvestApi } from '../../services/api';
import Card from '../../components/common/Card';
import Badge from '../../components/common/Badge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import Button from '../../components/common/Button';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import HarvestForm from './HarvestForm';
import './HarvestPage.css';

const HarvestPage = () => {
  const [harvests, setHarvests] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(null);

  const [formOpen, setFormOpen] = React.useState(false);
  const [editingRecord, setEditingRecord] = React.useState(null);

  const [deleteConfirm, setDeleteConfirm] = React.useState({ open: false, record: null });
  const [deleting, setDeleting] = React.useState(false);

  React.useEffect(() => {
    fetchHarvests();
  }, []);

  const fetchHarvests = async () => {
    try {
      setLoading(true);
      const response = await harvestApi.getAll();
      setHarvests(response.data || []);
      setError(null);
    } catch (err) {
      console.error('Error fetching harvests:', err);
      setError('Failed to load harvest events.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingRecord(null);
    setFormOpen(true);
  };

  const handleEdit = (record) => {
    setEditingRecord(record);
    setFormOpen(true);
  };

  const handleDeleteRequest = (record) => {
    setDeleteConfirm({ open: true, record });
  };

  const handleDeleteConfirm = async () => {
    const record = deleteConfirm.record;
    if (!record) return;
    setDeleting(true);
    try {
      await harvestApi.delete(record.harvestId);
      setDeleteConfirm({ open: false, record: null });
      await fetchHarvests();
    } catch (err) {
      console.error('Error deleting harvest:', err);
      setError(`Failed to delete harvest ${record.harvestId}.`);
    } finally {
      setDeleting(false);
    }
  };

  const handleFormSuccess = () => {
    fetchHarvests();
  };

  if (loading) return <Loading />;
  if (error) return <ErrorMessage message={error} onRetry={fetchHarvests} />;

  return (
    <div className="page-container">
      <div className="page-header">
        <h1>Harvest Events</h1>
        <Button variant="primary" onClick={handleCreate}>
          <span className="material-icons" style={{ fontSize: 18, verticalAlign: 'middle', marginRight: 6 }}>add</span>
          New Harvest
        </Button>
      </div>

      <Card>
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Harvest ID</th>
                <th>Date</th>
                <th>Farm</th>
                <th>Block</th>
                <th>Variety</th>
                <th>Quantity (kg)</th>
                <th>Grade</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {harvests.length === 0 ? (
                <tr>
                  <td colSpan="8" className="empty-row">No harvest events found</td>
                </tr>
              ) : (
                harvests.map((harvest) => (
                  <tr key={harvest.harvestId}>
                    <td>{harvest.harvestId}</td>
                    <td>{harvest.harvestDate}</td>
                    <td>{harvest.farmId}</td>
                    <td>{harvest.blockId}</td>
                    <td>{harvest.mangoVariety}</td>
                    <td>{harvest.harvestQuantityKg}</td>
                    <td><Badge variant={harvest.qualityGrade === 'A' ? 'success' : harvest.qualityGrade === 'D' ? 'error' : 'info'}>{harvest.qualityGrade}</Badge></td>
                    <td>
                      <div className="row-actions">
                        <Button variant="secondary" size="sm" onClick={() => handleEdit(harvest)}>Edit</Button>
                        <Button variant="danger" size="sm" onClick={() => handleDeleteRequest(harvest)}>Delete</Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <HarvestForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSuccess={handleFormSuccess}
        editRecord={editingRecord}
      />

      <ConfirmDialog
        isOpen={deleteConfirm.open}
        onClose={() => setDeleteConfirm({ open: false, record: null })}
        onConfirm={handleDeleteConfirm}
        title="Delete Harvest Event"
        message={`Are you sure you want to delete harvest event "${deleteConfirm.record?.harvestId}"? This action cannot be undone.`}
        confirmText="Delete"
        loading={deleting}
      />
    </div>
  );
};

export default HarvestPage;